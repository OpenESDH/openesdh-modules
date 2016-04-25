package dk.openesdh.project.rooms.services;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.repo.model.CaseDocument;

public interface CaseSiteDocumentsService {
    /**
     * Retrieves site documents with attachments
     * 
     * @param siteShortName
     * @return List of documents with attachments
     */
    List<CaseDocument> getCaseSiteDocumentsWithAttachments(String siteShortName);

    /**
     * Copies provided site documents back to case and unlocks case documents.
     * 
     * @param site
     */
    void copySiteDocumentsBackToCase(CaseSite site);

    void copySiteDocuments(CaseSite site, NodeRef targetFolder) throws Exception;
}
