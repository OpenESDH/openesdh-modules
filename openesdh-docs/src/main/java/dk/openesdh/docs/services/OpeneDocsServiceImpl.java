package dk.openesdh.docs.services;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.MimetypeService;
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

import dk.openesdh.repo.model.OpenESDHModel;
import dk.openesdh.repo.services.NodeInfoService;
import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.services.documents.DocumentService;
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
    @Qualifier(DocumentService.BEAN_ID)
    private DocumentService documentService;

    @Autowired
    @Qualifier("ContentService")
    private ContentService contentService;

    @Autowired
    @Qualifier("mimetypeService")
    private MimetypeService mimetypeService;

    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;

    @Override
    public List<NodeRef> getDocumentTemplates(String... extensions) {
        return getDocsTemplatesStream(extensions)
                .collect(Collectors.toList());
    }
    
    @Override
    public JSONArray getDocumentTemplatesJson() {
        return getDocsTemplatesStream()
                .map(this::getTemplateJson)
                .collect(JSONArrayCollector.simple());
    }
    
    @Override
    public JSONArray getDocumentTemplatesJson(String... extensions) {
        return getDocsTemplatesStream(extensions)
                .map(this::getTemplateJson)
                .collect(JSONArrayCollector.simple());
    }

    @Override
    public void updateTemplate(NodeRef templateRef, String title, String description, NodeRef docType,
            NodeRef docCategory) {

        Map<QName, Serializable> props = nodeService.getProperties(templateRef);
        props.put(ContentModel.PROP_TITLE, title);

        if (Objects.nonNull(docType)) {
            props.put(OpenESDHModel.PROP_DOC_TYPE, docType);
        } else {
            props.remove(OpenESDHModel.PROP_DOC_TYPE);
        }

        if (Objects.nonNull(docCategory)) {
            props.put(OpenESDHModel.PROP_DOC_CATEGORY, docCategory);
        } else {
            props.remove(OpenESDHModel.PROP_DOC_CATEGORY);
        }

        if (StringUtils.isNotBlank(description)) {
            props.put(ContentModel.PROP_DESCRIPTION, description);
        } else {
            props.remove(ContentModel.PROP_DESCRIPTION);
        }

        nodeService.setProperties(templateRef, props);
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
        Set<String> extList = Arrays.stream(extensions)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return getDocsTemplatesStream()
                .filter(tplRef -> matchTemplateExtension(tplRef, extList));
    }

    private Stream<NodeRef> getDocsTemplatesStream() {
        return nodeService
                .getChildAssocs(openeDocsFolderService.getDocsTemplatesFolder(), ContentModel.ASSOC_CONTAINS, null)
                .stream()
                .map(ChildAssociationRef::getChildRef);
    }

    private boolean matchTemplateExtension(NodeRef templateRef, Set<String> extensions) {
        String name = nodeService.getProperty(templateRef, ContentModel.PROP_NAME).toString().toLowerCase();
        String ext = FilenameUtils.getExtension(name).toLowerCase();
        return extensions.contains(ext);
    }
}
