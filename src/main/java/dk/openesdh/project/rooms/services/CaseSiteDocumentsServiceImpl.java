package dk.openesdh.project.rooms.services;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteService;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.project.rooms.model.CaseSiteDocument;
import dk.openesdh.repo.model.CaseDocument;
import dk.openesdh.repo.model.CaseDocumentAttachment;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.CaseDocumentCopyService;
import dk.openesdh.repo.services.documents.DocumentService;
import dk.openesdh.repo.services.lock.OELockService;
import dk.openesdh.repo.services.xsearch.CaseDocumentsSearchServiceImpl;

@Service("CaseSiteDocumentsService")
public class CaseSiteDocumentsServiceImpl extends CaseDocumentsSearchServiceImpl
        implements CaseSiteDocumentsService {

    @Autowired
    @Qualifier("SiteService")
    private SiteService siteService;
    @Autowired
    @Qualifier("CheckOutCheckInService")
    private CheckOutCheckInService checkOutCheckInService;
    @Autowired
    @Qualifier("ContentService")
    private ContentService contentService;
    @Autowired
    @Qualifier("DocumentService")
    private DocumentService documentService;
    @Autowired
    @Qualifier("CaseService")
    private CaseService caseService;
    @Autowired
    @Qualifier("CaseDocumentCopyService")
    private CaseDocumentCopyService caseDocumentCopyService;
    @Autowired
    @Qualifier("OELockService")
    private OELockService oeLockService;


    @Override
    public void copySiteDocuments(CaseSite site, NodeRef targetFolder) throws Exception {
        for (CaseDocument document : site.getSiteDocuments()) {
            caseDocumentCopyService.copyDocumentToFolderRetainVersionLabels(document, targetFolder);
            oeLockService.lock(new NodeRef(document.getNodeRef()), true);
        }
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
    public List<CaseSiteDocument> getCaseSiteDocuments(String siteShortName) {
        return getSiteDocumentsRefs(siteShortName).map(this::getCaseSiteDocument)
                .collect(Collectors.toList());
    }
    
    @Override
    public JSONArray getCaseSiteDocumentsJson(String siteShortName) {
        List<NodeRef> siteDocsRefs = getSiteDocumentsRefs(siteShortName).collect(Collectors.toList());
        return this.getNodesJSON(siteDocsRefs);
    }

    @Override
    public List<CaseDocument> getCaseSiteDocumentsWithAttachments(String siteShortName){
        return getSiteDocumentsRefs(siteShortName)
                    .map(documentService::getCaseDocument)
                    .collect(Collectors.toList());
    }

    private Stream<NodeRef> getSiteDocumentsRefs(String siteShortName) {
        NodeRef documentLibrary = siteService.getContainer(siteShortName, SiteService.DOCUMENT_LIBRARY);
        return nodeService.getChildAssocs(documentLibrary)
                .stream()
                .map(ChildAssociationRef::getChildRef);
    }

    @Override
    public void copySiteDocumentsBackToCase(CaseSite site) {
        NodeRef caseRef = caseService.getCaseById(site.getCaseId());
        NodeRef caseDocsFolderRef = caseService.getDocumentsFolder(caseRef);
        Map<NodeRef, CaseDocument> caseDocuments = documentService.getCaseDocumentsWithAttachments(site.getCaseId())
                .stream()
                .collect(Collectors.toMap(CaseDocument::nodeRefObject, Function.identity()));

        unlockCaseDocuments(site.getShortName(), caseDocuments);
        
        if (site.getSiteDocuments().isEmpty()) {
            return;
        }

        for (CaseDocument siteDocument : site.getSiteDocuments()) {
            Optional<CaseDocument> originalDoc = getDocOriginal(siteDocument.nodeRefObject(), caseDocuments);
            if (originalDoc.isPresent()) {
                copySiteDocumentBackToCase(siteDocument, originalDoc.get());
            } else {
                copySiteDocAsNewCaseDocument(siteDocument, caseDocsFolderRef);
            }
        }
    }
    
    private void unlockCaseDocuments(String siteShortName, Map<NodeRef, CaseDocument> caseDocuments){
        getSiteDocumentsRefs(siteShortName)
            .map(siteDoc -> getDocOriginal(siteDoc, caseDocuments))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(CaseDocument::nodeRefObject)
            .forEach(doc -> oeLockService.unlock(doc, true));
    }
    
    private void copySiteDocAsNewCaseDocument(CaseDocument siteDocument, NodeRef caseDocsFolderRef) {
        caseDocumentCopyService.copyDocumentToFolder(siteDocument.nodeRefObject(), caseDocsFolderRef);
    }

    private void copySiteDocumentBackToCase(CaseDocument siteDocument, CaseDocument originalCaseDocument) {
        NodeRef originalDocRef = new NodeRef(originalCaseDocument.getMainDocNodeRef());
        NodeRef originalDocRecord = originalCaseDocument.nodeRefObject();

        createDocNewVersion(originalDocRef, new NodeRef(siteDocument.getMainDocNodeRef()));
        
        Map<NodeRef, CaseDocumentAttachment> caseDocAttachments = originalCaseDocument.getAttachments()
                .stream()
                .collect(Collectors.toMap(CaseDocumentAttachment::nodeRefObject, Function.identity()));
        
        for(CaseDocumentAttachment attachment : siteDocument.getAttachments()){
            NodeRef attachmentRef = attachment.nodeRefObject();
            Optional<CaseDocumentAttachment> attachmentOriginal = getDocOriginal(attachmentRef,
                    caseDocAttachments);
            if(attachmentOriginal.isPresent()){
                createDocNewVersion(attachmentOriginal.get().nodeRefObject(), attachmentRef);
            }else{
                caseDocumentCopyService.copyDocument(attachmentRef, originalDocRecord, false);
            }
        }
    }

    private void createDocNewVersion(NodeRef oldVersionContent, NodeRef newVersionContentRef) {
        ContentReader newContent = contentService.getReader(newVersionContentRef, ContentModel.PROP_CONTENT);
        if (Objects.isNull(newContent)) {
            return;
        }
        NodeRef workingCopy = checkOutCheckInService.checkout(oldVersionContent);
        ContentWriter writer = contentService.getWriter(workingCopy, ContentModel.PROP_CONTENT, true);
        writer.setMimetype(newContent.getMimetype());
        writer.putContent(newContent);
        checkOutCheckInService.checkin(workingCopy, null);
    }
    
    private <T> Optional<T> getDocOriginal(NodeRef docRef, Map<NodeRef, T> docList) {
        return nodeService.getTargetAssocs(docRef, ContentModel.ASSOC_ORIGINAL)
                .stream()
                .map(AssociationRef::getTargetRef)
                .filter(docList.keySet()::contains)
                .map(docList::get)
                .findAny();
    }
}
