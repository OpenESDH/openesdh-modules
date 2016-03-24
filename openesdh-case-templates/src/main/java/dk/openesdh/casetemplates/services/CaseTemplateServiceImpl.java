package dk.openesdh.casetemplates.services;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.casetemplates.model.CaseTemplatesModule;
import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.CaseDocumentCopyService;
import dk.openesdh.repo.services.xsearch.CaseDocumentsSearchServiceImpl;
import dk.openesdh.repo.utils.JSONArrayCollector;

@Service("CaseTemplateService")
public class CaseTemplateServiceImpl implements CaseTemplateService {

    @Autowired
    @Qualifier(CaseService.BEAN_ID)
    private CaseService caseService;
    @Autowired
    @Qualifier("CaseTemplatesFolderService")
    private CaseTemplatesFolderService caseTemplatesFolderService;
    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;
    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;
    @Autowired
    @Qualifier("NamespaceService")
    private NamespaceService namespaceService;
    @Autowired
    @Qualifier("CaseDocumentCopyService")
    private CaseDocumentCopyService caseDocumentCopyService;
    @Autowired
    @Qualifier("CaseDocumentsSearchService")
    private CaseDocumentsSearchServiceImpl caseDocumentsSearchService;

    @Override
    public void onCreateCaseTemplate(final NodeRef caseTemplateRef) {
        tr.runAsAdmin(() -> {
            moveTemplateToProperFolder(caseTemplateRef);
            nodeService.addAspect(caseTemplateRef, CaseTemplatesModule.ASPECT_CT_CASE_TEMPLATE, null);
            caseService.createFolderForCaseDocuments(caseTemplateRef);
            return null;
        });
    }

    @Override
    public JSONArray getCaseTemplates(String caseType) {
        NodeRef caseTypeTemplatesFolder = caseTemplatesFolderService.getCaseTypeTemplatesFolder(caseType);
        return nodeService.getChildAssocs(caseTypeTemplatesFolder, ContentModel.ASSOC_CONTAINS, null)
            .stream()
            .map(ChildAssociationRef::getChildRef)
            .map(this::getCaseTemplateJson)
            .collect(JSONArrayCollector.json());
    }
    
    @Override
    public JSONObject getCaseTemplateJson(NodeRef templateRef) {
        try {
            JSONObject json = caseService.getCaseInfoJson(templateRef);
            json.put(TEMPLATE_DOCS, getCaseTemplateDocs(templateRef));
            return json;
        } catch (JSONException e) {
            throw new AlfrescoRuntimeException("Error getting case template json", e);
        }
    }

    @Override
    public boolean isDocBelongsToCaseTemplate(NodeRef docRef){
        return nodeService.getPath(docRef)
                .toPrefixString(namespaceService)
                .startsWith(CaseTemplatesFolderService.CASE_TEMPLATES_ROOT_FOLDER_PATH);
    }

    @Override
    public List<String> getCaseTemplateWorkflows(NodeRef templateRef) {
        return (List<String>) nodeService.getProperty(templateRef, CaseTemplatesModule.PROP_WORKFLOWS);
    }

    @Override
    public void copyCaseTemplateDocsToCase(NodeRef caseRef) {
        NodeRef templateRef = (NodeRef) nodeService.getProperty(caseRef, ContentModel.PROP_TEMPLATE);
        if (Objects.isNull(templateRef)) {
            return;
        }
        NodeRef caseDocsFolder = caseService.getDocumentsFolder(caseRef);
        getCaseTemplateDocsStream(templateRef)
                .forEach(docRecRef -> caseDocumentCopyService.copyDocumentToFolder(docRecRef, caseDocsFolder));
    }
    
    @Override
    public JSONArray getCaseTemplateDocuments(NodeRef templateRef){
        return getCaseTemplateDocsStream(templateRef)
                .map(this::getCaseTemplateDocument)
                .collect(JSONArrayCollector.json());
    }
    
    private JSONObject getCaseTemplateDocument(NodeRef nodeRef) {
        try {
            return caseDocumentsSearchService.nodeToJSON(nodeRef);
        } catch (JSONException e) {
            throw new AlfrescoRuntimeException("Error retrieving case template document", e);
        }
    }

    private void moveTemplateToProperFolder(NodeRef caseTemplateRef) {
        QName caseType = nodeService.getType(caseTemplateRef);
        NodeRef caseTypeTemplatesFolder = caseTemplatesFolderService.getCaseTypeTemplatesFolder(caseType);
        String templateName = (String) nodeService.getProperty(caseTemplateRef, ContentModel.PROP_NAME);
        nodeService.moveNode(caseTemplateRef, caseTypeTemplatesFolder, ContentModel.ASSOC_CONTAINS,
                QName.createQName(templateName));
    }

    private JSONArray getCaseTemplateDocs(NodeRef templateRef) {
        return getCaseTemplateDocsStream(templateRef)
                .map(this::getTemplateDocInfo)
                .collect(JSONArrayCollector.json());
    }
    
    private Stream<NodeRef> getCaseTemplateDocsStream(NodeRef templateRef){
        NodeRef docsFolder = caseService.getDocumentsFolder(templateRef);
        return nodeService.getChildAssocs(docsFolder, ContentModel.ASSOC_CONTAINS, null)
            .stream()
            .map(ChildAssociationRef::getChildRef);
    }

    private JSONObject getTemplateDocInfo(NodeRef docRef) {
        try {
            JSONObject info = new JSONObject();
            info.put(TEMPLATE_DOC_NAME, nodeService.getProperty(docRef, ContentModel.PROP_NAME));
            info.put(TEMPLATE_DOC_TITLE, nodeService.getProperty(docRef, ContentModel.PROP_TITLE));
            return info;
        } catch (JSONException e) {
            throw new AlfrescoRuntimeException("Error getting case template document json", e);
        }
    }

}
