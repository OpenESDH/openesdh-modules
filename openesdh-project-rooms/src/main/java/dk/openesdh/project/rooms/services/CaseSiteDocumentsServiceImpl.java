package dk.openesdh.project.rooms.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.repo.model.CaseDocsFolder;
import dk.openesdh.repo.model.CaseDocument;
import dk.openesdh.repo.model.CaseDocumentAttachment;
import dk.openesdh.repo.model.CaseFolderItem;
import dk.openesdh.repo.services.BehaviourFilterService;
import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.CaseDocsFolderExplorerService;
import dk.openesdh.repo.services.documents.CaseDocumentCopyService;
import dk.openesdh.repo.services.documents.DocumentService;
import dk.openesdh.repo.services.lock.OELockService;

@Service("CaseSiteDocumentsService")
public class CaseSiteDocumentsServiceImpl implements CaseSiteDocumentsService {

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
    @Qualifier("CaseDocsFolderExplorerService")
    private CaseDocsFolderExplorerService caseDocsFolderExplorerService;
    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;


    @Override
    public void copySiteDocuments(CaseSite site, NodeRef targetFolder) throws Exception {
        for (CaseFolderItem item : site.getSiteDocuments()) {
            caseDocumentCopyService.copyDocFolderItemRetainVersionLabels(item, targetFolder);
            lockCaseDocuments(item);
        }
    }

    @Override
    public List<CaseFolderItem> getCaseSiteDocFolderItems(String siteShortName) {
        NodeRef siteDocLibrary = siteService.getContainer(siteShortName, SiteService.DOCUMENT_LIBRARY);
        return caseDocsFolderExplorerService.getCaseDocsFoldersHierarchy(siteDocLibrary);
    }

    private void lockCaseDocuments(CaseFolderItem item) {
        if (!item.isFolder()) {
            oeLockService.lock(item.getNodeRef(), true);
            return;
        }
        CaseDocsFolder folder = (CaseDocsFolder) item;
        folder.getChildren().forEach(this::lockCaseDocuments);
    }

    @Override
    public void copySiteDocumentsBackToCase(CaseSite siteDataToCopyBack) {
        NodeRef caseRef = caseService.getCaseById(siteDataToCopyBack.getCaseId());
        NodeRef caseDocsFolderRef = caseService.getDocumentsFolder(caseRef);

        List<CaseFolderItem> caseDocFolderItems = caseDocsFolderExplorerService
                .getCaseDocsFoldersHierarchy(caseDocsFolderRef);
        Map<NodeRef, CaseFolderItem> caseFolderItemsMap = getDocsFolderItemsMap(caseDocFolderItems);
        
        List<NodeRef> nodesToRestoreAutoVersion = tr.runInNewTransaction(() -> {

            List<NodeRef> restoreAutoVersion = unlockCaseDocsDisableAutoVersion(siteDataToCopyBack.getShortName(),
                    caseFolderItemsMap);

            if (siteDataToCopyBack.getSiteDocuments().isEmpty()) {
                return restoreAutoVersion;
            }

            siteDataToCopyBack.getSiteDocuments()
                    .forEach(item -> copySiteDocFolderItemBackToCase(item, caseDocsFolderRef, caseFolderItemsMap));

            return restoreAutoVersion;
        });

        tr.runInNewTransaction(() -> {
            behaviourFilterService.executeWithoutBehavior(() -> {
                nodesToRestoreAutoVersion.stream().forEach(this::enableAutoVersion);
            });
            return null;
        });

    }
    
    private Map<NodeRef, CaseFolderItem> getDocsFolderItemsMap(List<CaseFolderItem> items) {
        Map<NodeRef, CaseFolderItem> map = new HashMap<>();
        putCaseFolderItemsToMap(items, map);
        return map;
    }

    private void putCaseFolderItemsToMap(List<CaseFolderItem> items, Map<NodeRef, CaseFolderItem> map) {
        for (CaseFolderItem item : items) {
            map.put(item.getNodeRef(), item);
            if (item.isFolder()) {
                putCaseFolderItemsToMap(((CaseDocsFolder) item).getChildren(), map);
            }
        }
    }

    private List<NodeRef> unlockCaseDocsDisableAutoVersion(String siteShortName,
            Map<NodeRef, CaseFolderItem> caseFolderItemsMap) {
        List<CaseFolderItem> siteDocFolderItems = getCaseSiteDocFolderItems(siteShortName);
        Map<NodeRef, CaseFolderItem> siteDocsFolderItemsMap = getDocsFolderItemsMap(siteDocFolderItems);
        return siteDocsFolderItemsMap.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isFolder())
                .map(entry -> getItemOriginal(entry.getKey(), caseFolderItemsMap))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(item -> (CaseDocument)item)
                .map(this::unlockDoc)
                .map(this::disableAutoVersion)
                .flatMap(nodes -> nodes.stream())
                .collect(Collectors.toList());
    }

    private CaseDocument unlockDoc(CaseDocument caseDoc) {
        oeLockService.unlock(caseDoc.getNodeRef(), true);
        return caseDoc;
    }

    private List<NodeRef> disableAutoVersion(CaseDocument caseDoc) {
        List<NodeRef> nodesToRestoreAutoVersion = new ArrayList<>();
        NodeRef mainDocRef = caseDoc.getMainDocNodeRef();
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

    private void copySiteDocFolderItemBackToCase(CaseFolderItem item, NodeRef targetFolderRef,
            Map<NodeRef, CaseFolderItem> caseFolderItemsMap) {
        
        Optional<CaseFolderItem> originalItem = getItemOriginal(item.getNodeRef(), caseFolderItemsMap);

        if (originalItem.isPresent()) {
            copySiteItemBackToCase(item, originalItem.get(), caseFolderItemsMap);
        } else {
            copySiteItemAsNewCaseItem(item, targetFolderRef, caseFolderItemsMap);
        }
    }

    private void copySiteItemBackToCase(CaseFolderItem item, CaseFolderItem originalItem,
            Map<NodeRef, CaseFolderItem> caseFolderItemsMap) {
        
        if (!item.isFolder()) {
            copySiteDocumentBackToCase((CaseDocument) item, (CaseDocument) originalItem);
            return;
        }
        
        CaseDocsFolder folder = (CaseDocsFolder) item;
        folder.getChildren().forEach(childItem -> copySiteDocFolderItemBackToCase(childItem,
                originalItem.getNodeRef(), caseFolderItemsMap));
    }

    private void copySiteItemAsNewCaseItem(CaseFolderItem item, NodeRef targetFolderRef,
            Map<NodeRef, CaseFolderItem> caseFolderItemsMap) {

        if (!item.isFolder()) {
            copySiteDocAsNewCaseDocument((CaseDocument) item, targetFolderRef);
            return;
        }

        // We need to copy folder in a new transaction in order to copy docs and
        // retain versions (which also use new transactions).
        NodeRef newCaseItemRef = tr.runInNewTransaction(() -> {
            return caseDocumentCopyService.copyDocFolderItem(item.getNodeRef(), targetFolderRef, false);
        });

        CaseDocsFolder folder = (CaseDocsFolder) item;
        folder.getChildren().forEach(
                childItem -> copySiteDocFolderItemBackToCase(childItem, newCaseItemRef, caseFolderItemsMap));
    }

    private void copySiteDocAsNewCaseDocument(CaseDocument siteDocument, NodeRef targetFolderRef) {
        NodeRef documentCopyRef = caseDocumentCopyService.copyDocumentToFolderRetainVersionLabels(siteDocument,
                targetFolderRef);
        caseDocumentCopyService.moveDocumentComments(siteDocument.getNodeRef(), documentCopyRef);
    }

    private void copySiteDocumentBackToCase(CaseDocument siteDocument, CaseDocument originalCaseDocument) {
        NodeRef originalDocRef = originalCaseDocument.getMainDocNodeRef();
        NodeRef originalDocRecord = originalCaseDocument.getNodeRef();

        NodeRef siteMainDocRef = siteDocument.getMainDocNodeRef();
        if (isDocVersionChanged(originalDocRef, siteMainDocRef)) {
            caseDocumentCopyService.copyDocContentRetainVersionLabel(siteMainDocRef, originalDocRef);
        }
        
        caseDocumentCopyService.moveDocumentComments(siteDocument.getNodeRef(), originalDocRecord);

        Map<NodeRef, CaseDocumentAttachment> caseDocAttachments = originalCaseDocument.getAttachments()
                .stream()
                .collect(Collectors.toMap(CaseDocumentAttachment::nodeRefObject, Function.identity()));
        
        for(CaseDocumentAttachment siteAttachment : siteDocument.getAttachments()){
            NodeRef siteAttachmentRef = siteAttachment.nodeRefObject();
            Optional<CaseDocumentAttachment> attachmentOriginal = getItemOriginal(siteAttachmentRef,
                    caseDocAttachments);
            if (attachmentOriginal.isPresent()
                    && isDocVersionChanged(attachmentOriginal.get().nodeRefObject(), siteAttachmentRef)) {
                caseDocumentCopyService.copyDocContentRetainVersionLabel(
                        siteAttachmentRef, attachmentOriginal.get().nodeRefObject());
            }else{
                caseDocumentCopyService.copyDocFolderItem(siteAttachmentRef, originalDocRecord, false);
            }
        }
    }

    private boolean isDocVersionChanged(NodeRef original, NodeRef copy) {
        Version originalVersion = versionService.getCurrentVersion(original);
        Version copyVersion = versionService.getCurrentVersion(copy);
        return !originalVersion.getVersionLabel().equals(copyVersion.getVersionLabel());
    }

    private <T> Optional<T> getItemOriginal(NodeRef docRef, Map<NodeRef, T> docList) {
        return nodeService.getTargetAssocs(docRef, ContentModel.ASSOC_ORIGINAL)
                .stream()
                .map(AssociationRef::getTargetRef)
                .filter(docList.keySet()::contains)
                .map(docList::get)
                .findAny();
    }
}
