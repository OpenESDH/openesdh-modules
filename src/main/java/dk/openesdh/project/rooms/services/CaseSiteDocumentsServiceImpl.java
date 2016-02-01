package dk.openesdh.project.rooms.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.project.rooms.model.CaseSiteDocument;
import dk.openesdh.repo.services.documents.DocumentService;

@Service("CaseSiteDocumentsService")
public class CaseSiteDocumentsServiceImpl implements CaseSiteDocumentsService {

    @Autowired
    @Qualifier("DocumentService")
    private DocumentService documentService;
    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;
    @Autowired
    @Qualifier("CheckOutCheckInService")
    private CheckOutCheckInService checkOutCheckInService;
    @Autowired
    @Qualifier("ContentService")
    private ContentService contentService;

    @Override
    public void createNewVersionOfCaseDocument(NodeRef caseDocumentFolder, NodeRef newVersionDocumentFolder) {
        NodeRef newVersionMainDoc = documentService.getMainDocument(newVersionDocumentFolder);
        createNewVersionOfCaseMainDocument(caseDocumentFolder, newVersionMainDoc);

        List<CaseSiteDocument> newVersionAttachments = getAttachmentsObjects(newVersionDocumentFolder);
        if (newVersionAttachments.isEmpty()) {
            return;
        }

        List<CaseSiteDocument> caseDocAttachments = getAttachmentsObjects(caseDocumentFolder);

        for (CaseSiteDocument newVersionAttachment : newVersionAttachments) {
            Optional<CaseSiteDocument> docAttachment = caseDocAttachments
                    .stream()
                    .filter(attachment -> attachment.getName().equals(newVersionAttachment.getName()))
                    .findAny();
            if (docAttachment.isPresent()) {
                createDocumentNewVersion(docAttachment.get(), newVersionAttachment);
            } else {
                copyDocumentFileToDocumentFolder(newVersionAttachment, caseDocumentFolder);
            }
        }
    }

    @Override
    public NodeRef createNewVersionOfCaseMainDocument(NodeRef caseDocumentFolder,
            NodeRef newVersionDocumentContent) {
        NodeRef mainDoc = documentService.getMainDocument(caseDocumentFolder);
        return createDocumentNewVersion(mainDoc, newVersionDocumentContent);
    }

    @Override
    public CaseSiteDocument getCaseSiteDocument(NodeRef nodeRef) {
        String name = nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
        String type = nodeService.getType(nodeRef).toString();
        return new CaseSiteDocument(name, nodeRef.toString(), type);
    }

    @Override
    public List<CaseSiteDocument> getCaseDocuments(NodeRef caseNodeRef) {
        return documentService.getDocumentsForCase(caseNodeRef)
                .stream()
                .map(assoc -> getCaseSiteDocument(assoc.getChildRef()))
                .collect(Collectors.toList());
    }

    @Override
    public NodeRef copyDocumentFileToDocumentFolder(CaseSiteDocument sourceDocumentFile, NodeRef targetDocumentFolder) {
        Map<QName, Serializable> sourceProps = nodeService.getProperties(new NodeRef(sourceDocumentFile.getNodeRef()));
        
        Map<QName, Serializable> targetProps = new HashMap<QName, Serializable>();
        targetProps.put(ContentModel.PROP_NAME, sourceProps.get(ContentModel.PROP_NAME));
        targetProps.put(ContentModel.PROP_CONTENT, sourceProps.get(ContentModel.PROP_CONTENT));

        Set<QName> sourcePropNames = sourceProps.keySet();

        // Serializable docType =
        // sourcePropNames.contains(OpenESDHModel.PROP_DOC_TYPE) ? sourceProps
        // .get(OpenESDHModel.PROP_DOC_TYPE) :
        // OpenESDHModel.DEFAULT_PROP_DOC_TYPE;
        // targetProps.put(OpenESDHModel.PROP_DOC_TYPE, docType);
        //
        // Serializable docCategory =
        // sourcePropNames.contains(OpenESDHModel.PROP_DOC_CATEGORY) ?
        // sourceProps
        // .get(OpenESDHModel.PROP_DOC_CATEGORY) :
        // OpenESDHModel.DEFAULT_PROP_DOC_CATEGORY;
        // targetProps.put(OpenESDHModel.PROP_DOC_CATEGORY, docCategory);
        //
        // Serializable docState =
        // sourcePropNames.contains(OpenESDHModel.PROP_DOC_STATE) ? sourceProps
        // .get(OpenESDHModel.PROP_DOC_STATE) :
        // OpenESDHModel.DEFAULT_PROP_DOC_STATE;
        // targetProps.put(OpenESDHModel.PROP_DOC_STATE, docState);

        return nodeService.createNode(targetDocumentFolder, ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, sourceDocumentFile.getName()),
                ContentModel.TYPE_CONTENT, targetProps).getChildRef();
    }
    
    private NodeRef createDocumentNewVersion(CaseSiteDocument oldVersionDocument,
            CaseSiteDocument newVersionDocument) {
        return createDocumentNewVersion(
                new NodeRef(oldVersionDocument.getNodeRef()), new NodeRef(newVersionDocument.getNodeRef()));
    }
    
    private NodeRef createDocumentNewVersion(NodeRef oldVersionDocumentContent, NodeRef newVersionDocumentContent) {

        ContentReader newVersionContentReader = contentService.getReader(newVersionDocumentContent,
                ContentModel.PROP_CONTENT);

        if (newVersionContentReader == null) {
            return oldVersionDocumentContent; // no new content
        }

        NodeRef workingCopy = checkOutCheckInService.checkout(oldVersionDocumentContent);

        ContentWriter writer = contentService.getWriter(workingCopy, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(newVersionContentReader.getMimetype());
        writer.putContent(newVersionContentReader);

        return checkOutCheckInService.checkin(workingCopy, null);
    }
    
    private List<CaseSiteDocument> getAttachmentsObjects(NodeRef documentFolderRef) {
        NodeRef mainDocRef = documentService.getMainDocument(documentFolderRef);
        return documentService.getAttachmentsAssoc(mainDocRef)
                .stream()
                .filter(assoc -> !assoc.getChildRef().equals(mainDocRef))
                .map(assoc -> getCaseSiteDocument(assoc.getChildRef()))
                .collect(Collectors.toList());
   }

}
