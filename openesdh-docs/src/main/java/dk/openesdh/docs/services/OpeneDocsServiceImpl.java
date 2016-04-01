package dk.openesdh.docs.services;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.repo.services.NodeInfoService;
import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.utils.JSONArrayCollector;

@Service("OpeneDocsService")
public class OpeneDocsServiceImpl implements OpeneDocsService {

    @Autowired
    @Qualifier("OpeneDocsFolderService")
    private OpeneDocsFolderService openeDocsFolderService;

    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;

    @Autowired
    @Qualifier("CopyService")
    private CopyService copyService;
    
    @Autowired
    @Qualifier("NodeInfoService")
    private NodeInfoService nodeInfoService;

    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;

    @Override
    public List<NodeRef> getDocumentTemplates(String... extensions) {
        return getDocsTemplatesStream(extensions)
                .collect(Collectors.toList());
    }
    
    @Override
    public JSONArray getDocumentTemplatesJson(String... extensions) {
        return getDocsTemplatesStream(extensions)
                .map(this::getTemplateJson)
                .collect(JSONArrayCollector.simple());
    }

    @Override
    public NodeRef createDocument(NodeRef targetFolderRef, NodeRef sourceTemplateRef,
            Map<QName, Serializable> props) {
        return tr.runInTransaction(() -> {
            NodeRef copyRef = copyService.copyAndRename(sourceTemplateRef, targetFolderRef,
                    ContentModel.ASSOC_CONTAINS, null, false);
            if (props == null || props.keySet().isEmpty()) {
                return copyRef;
            }
            Map<QName, Serializable> curProps = nodeService.getProperties(copyRef);
            curProps.putAll(props);
            String title = (String) curProps.get(ContentModel.PROP_TITLE);
            if (StringUtils.isNotEmpty(title)) {
                String docName = (String) curProps.get(ContentModel.PROP_NAME);
                String ext = FilenameUtils.getExtension(docName);
                curProps.put(ContentModel.PROP_NAME, title + FilenameUtils.EXTENSION_SEPARATOR_STR + ext);
            }
            nodeService.setProperties(copyRef, curProps);
            return copyRef;
        });
    }
    
    private JSONObject getTemplateJson(NodeRef nodeRef){
        JSONObject json = nodeInfoService.getNodeParametersJSON(nodeRef);
        json.put("nodeRef", nodeRef.toString());
        return json;
    }
    
    private Stream<NodeRef> getDocsTemplatesStream(String... extensions){
        List<String> extList = Arrays.stream(extensions)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        return getDocsTemplatesStream()
                .filter(tplRef -> matchTemplateExtension(tplRef, extList));
    }

    private Stream<NodeRef> getDocsTemplatesStream() {
        return nodeService
                .getChildAssocs(openeDocsFolderService.getDocsTemplatesFolder(), ContentModel.ASSOC_CONTAINS, null)
                .stream()
                .map(ChildAssociationRef::getChildRef);
    }

    private boolean matchTemplateExtension(NodeRef templateRef, List<String> extensions) {
        String name = nodeService.getProperty(templateRef, ContentModel.PROP_NAME).toString().toLowerCase();
        return extensions.stream()
                .filter(ext -> StringUtils.endsWith(name, ext))
                .findAny()
                .isPresent();
    }
}
