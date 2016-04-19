package dk.openesdh.project.rooms.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.repo.model.CaseDocument;
import dk.openesdh.repo.model.CaseDocumentAttachment;
import dk.openesdh.repo.services.BehaviourFilterService;
import dk.openesdh.repo.services.TransactionRunner;
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
    @Qualifier(DocumentService.BEAN_ID)
    private DocumentService documentService;
    @Autowired
    @Qualifier(CaseService.BEAN_ID)
    private CaseService caseService;
    @Autowired
    @Qualifier("CaseDocumentCopyService")
    private CaseDocumentCopyService caseDocumentCopyService;
    @Autowired
    @Qualifier("OELockService")
    private OELockService oeLockService;
    @Autowired
    @Qualifier("CaseDocumentVersionService")
    private VersionService versionService;
    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;
    @Autowired
    private BehaviourFilterService behaviourFilterService;
    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;


    @Override
    public void copySiteDocuments(CaseSite site, NodeRef targetFolder) throws Exception {
        for (CaseDocument document : site.getSiteDocuments()) {
            caseDocumentCopyService.copyDocumentToFolderRetainVersionLabels(document, targetFolder);
            oeLockService.lock(new NodeRef(document.getNodeRef()), true);
        }
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
    public void copySiteDocumentsBackToCase(CaseSite siteDataToCopyBack) {
        NodeRef caseRef = caseService.getCaseById(siteDataToCopyBack.getCaseId());
        NodeRef caseDocsFolderRef = caseService.getDocumentsFolder(caseRef);
        Map<NodeRef, CaseDocument> caseDocuments = documentService.getCaseDocumentsWithAttachments(siteDataToCopyBack.getCaseId())
                .stream()
                .collect(Collectors.toMap(CaseDocument::nodeRefObject, Function.identity()));

        List<NodeRef> nodesToRestoreAutoVersion = tr.runInNewTransaction(() -> {
            List<NodeRef> restoreAutoVersion = unlockCaseDocsDisableAutoVersion(siteDataToCopyBack.getShortName(),
                    caseDocuments);

            if (siteDataToCopyBack.getSiteDocuments().isEmpty()) {
                return restoreAutoVersion;
            }

            for (CaseDocument siteDocument : siteDataToCopyBack.getSiteDocuments()) {
                Optional<CaseDocument> originalDoc = getDocOriginal(siteDocument.nodeRefObject(), caseDocuments);
                if (originalDoc.isPresent()) {
                    copySiteDocumentBackToCase(siteDocument, originalDoc.get());
                } else {
                    copySiteDocAsNewCaseDocument(siteDocument, caseDocsFolderRef);
                }
            }
            return restoreAutoVersion;
        });

        behaviourFilterService.executeWithoutBehavior(() -> {
            nodesToRestoreAutoVersion.stream().forEach(this::enableAutoVersion);
        });

    }
    
    private List<NodeRef> unlockCaseDocsDisableAutoVersion(String siteShortName,
            Map<NodeRef, CaseDocument> caseDocuments) {
        return getSiteDocumentsRefs(siteShortName)
                .map(siteDoc -> getDocOriginal(siteDoc, caseDocuments))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::unlockDoc)
                .map(this::disableAutoVersion)
                .flatMap(nodes -> nodes.stream()).collect(Collectors.toList());
    }

    private CaseDocument unlockDoc(CaseDocument caseDoc) {
        oeLockService.unlock(caseDoc.nodeRefObject(), true);
        return caseDoc;
    }

    private List<NodeRef> disableAutoVersion(CaseDocument caseDoc) {
        List<NodeRef> nodesToRestoreAutoVersion = new ArrayList<>();
        NodeRef mainDocRef = new NodeRef(caseDoc.getMainDocNodeRef());
        if (disableAutoVersion(mainDocRef)) {
            nodesToRestoreAutoVersion.add(mainDocRef);
        }
        for (CaseDocumentAttachment attachment : caseDoc.getAttachments()) {
            if (disableAutoVersion(attachment.nodeRefObject())) {
                nodesToRestoreAutoVersion.add(attachment.nodeRefObject());
            }
        }
        return nodesToRestoreAutoVersion;
    }
    
    private boolean disableAutoVersion(NodeRef nodeRef) {
        Optional<Boolean> autoVersion = Optional.ofNullable(
                (Boolean) nodeService.getProperty(nodeRef, ContentModel.PROP_AUTO_VERSION));
        if (autoVersion.isPresent() && autoVersion.get() == true) {
            nodeService.setProperty(nodeRef, ContentModel.PROP_AUTO_VERSION, false);
            return true;
        }
        return false;
    }
    
    private void enableAutoVersion(NodeRef nodeRef) {
        nodeService.setProperty(nodeRef, ContentModel.PROP_AUTO_VERSION, true);
    }

    private void copySiteDocAsNewCaseDocument(CaseDocument siteDocument, NodeRef caseDocsFolderRef) {
        NodeRef documentCopyRef = caseDocumentCopyService.copyDocumentToFolder(siteDocument.nodeRefObject(),
                caseDocsFolderRef);
        caseDocumentCopyService.moveDocumentComments(siteDocument.nodeRefObject(), documentCopyRef);
    }

    private void copySiteDocumentBackToCase(CaseDocument siteDocument, CaseDocument originalCaseDocument) {
        NodeRef originalDocRef = new NodeRef(originalCaseDocument.getMainDocNodeRef());
        NodeRef originalDocRecord = originalCaseDocument.nodeRefObject();

        NodeRef siteMainDocRef = new NodeRef(siteDocument.getMainDocNodeRef());
        if (isDocVersionChanged(originalDocRef, siteMainDocRef)) {
            caseDocumentCopyService.copyDocContentRetainVersionLabel(siteMainDocRef, originalDocRef);
        }
        
        caseDocumentCopyService.moveDocumentComments(siteDocument.nodeRefObject(), originalDocRecord);

        Map<NodeRef, CaseDocumentAttachment> caseDocAttachments = originalCaseDocument.getAttachments()
                .stream()
                .collect(Collectors.toMap(CaseDocumentAttachment::nodeRefObject, Function.identity()));
        
        for(CaseDocumentAttachment siteAttachment : siteDocument.getAttachments()){
            NodeRef siteAttachmentRef = siteAttachment.nodeRefObject();
            Optional<CaseDocumentAttachment> attachmentOriginal = getDocOriginal(siteAttachmentRef,
                    caseDocAttachments);
            if (attachmentOriginal.isPresent()
                    && isDocVersionChanged(attachmentOriginal.get().nodeRefObject(), siteAttachmentRef)) {
                caseDocumentCopyService.copyDocContentRetainVersionLabel(
                        siteAttachmentRef, attachmentOriginal.get().nodeRefObject());
            }else{
                caseDocumentCopyService.copyDocument(siteAttachmentRef, originalDocRecord, false);
            }
        }
    }

    private boolean isDocVersionChanged(NodeRef original, NodeRef copy) {
        Version originalVersion = versionService.getCurrentVersion(original);
        Version copyVersion = versionService.getCurrentVersion(copy);
        return !originalVersion.getVersionLabel().equals(copyVersion.getVersionLabel());
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
