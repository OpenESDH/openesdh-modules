package dk.openesdh.project.rooms.services;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.repo.model.CaseFolderItem;

public interface CaseSiteDocumentsService {
    /**
     * Copies provided site documents back to case and unlocks case documents.
     * 
     * @param site
     */
    void copySiteDocumentsBackToCase(CaseSite site);

    void copySiteDocuments(CaseSite site, NodeRef targetFolder) throws Exception;

    List<CaseFolderItem> getCaseSiteDocFolderItems(String siteShortName);
}
