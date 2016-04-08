package dk.openesdh.office.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.stereotype.Service;

import dk.openesdh.office.model.OutlookModel;
import dk.openesdh.repo.model.OpenESDHModel;
import dk.openesdh.repo.services.classification.ClassificatorManagementService;
import dk.openesdh.repo.services.documents.DocumentService;

@Service("OfficeService")
public class OfficeServiceImpl implements OfficeService {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    @Qualifier("DocumentTypeService")
    private ClassificatorManagementService documentTypeService;
    @Autowired
    @Qualifier("DocumentCategoryService")
    private ClassificatorManagementService documentCategoryService;

    public NodeRef createEmailDocument(String caseId, String name, String bodyText) {
        NodeRef documentFolder = documentService.createCaseDocument(
                caseId,
                name,
                name + ".txt",
                getDocumentTypeLetter(),
                getDocumentCategoryOther(),
                writer -> {
                    writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                    writer.putContent(bodyText);
                });
        setReceivedFromOutlook(documentFolder);
        return documentFolder;
    }

    private void setReceivedFromOutlook(NodeRef nodeRef) {
        Map<QName, Serializable> props = new HashMap<>();
        props.put(OutlookModel.PROP_OFFICE_OUTLOOK_RECEIVED, true);
        nodeService.addAspect(nodeRef, OutlookModel.ASPECT_OFFICE_OUTLOOK_RECEIVABLE, props);
    }

    private NodeRef getDocumentTypeLetter() {
        return documentTypeService.getClassifValueByName(OpenESDHModel.DOCUMENT_TYPE_LETTER)
                .orElseThrow(() -> new WebScriptException("Document type \"letter\" not found")).getNodeRef();
    }

    private NodeRef getDocumentCategoryOther() {
        return documentCategoryService.getClassifValueByName(OpenESDHModel.DOCUMENT_CATEGORY_OTHER)
                .orElseThrow(() -> new WebScriptException("Document type \"other\" not found")).getNodeRef();
    }
}
