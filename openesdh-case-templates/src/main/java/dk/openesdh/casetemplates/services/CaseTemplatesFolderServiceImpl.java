package dk.openesdh.casetemplates.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.casetemplates.model.CaseTemplatesModule;
import dk.openesdh.repo.services.system.OpenESDHFoldersService;

@Service("CaseTemplatesFolderService")
public class CaseTemplatesFolderServiceImpl implements CaseTemplatesFolderService {

    @Autowired
    @Qualifier("NamespaceService")
    private NamespaceService namespaceService;
    @Autowired
    @Qualifier("OpenESDHFoldersService")
    private OpenESDHFoldersService oeFoldersService;
    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;

    @Override
    public NodeRef getCaseTemplatesRootFolder() {
        return oeFoldersService.getFolder(oeFoldersService.getCasesRootNodeRef(), "templates");
    }

    @Override
    public NodeRef getOrCreateCaseTypeTemplatesFolder(QName caseTypeQName) {
        String caseType = caseTypeQName.toPrefixString(namespaceService);
        NodeRef templatesRootRef = getCaseTemplatesRootFolder();
        String sCaseType = caseType.replace(':', '_');
        return oeFoldersService.getFolderOptional(templatesRootRef, sCaseType)
                .orElseGet(() -> createCaseTemplatesFolder(templatesRootRef, sCaseType));
    }

    @Override
    public Optional<NodeRef> getCaseTypeTemplatesFolder(String caseType) {
        NodeRef templatesRootRef = getCaseTemplatesRootFolder();
        String sCaseType = caseType.replace(':', '_');
        return oeFoldersService.getFolderOptional(templatesRootRef, sCaseType);
    }
    
    private NodeRef createCaseTemplatesFolder(NodeRef templatesRootRef, String caseType){
        Map<QName, Serializable> props = new HashMap<>();
        props.put(ContentModel.PROP_NAME, caseType);
        props.put(ContentModel.PROP_TITLE, caseType);
        return nodeService.createNode(templatesRootRef, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(CaseTemplatesModule.CT_URI, caseType),
                ContentModel.TYPE_FOLDER, props).getChildRef();
    }
}
