package dk.openesdh.project.rooms.services;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.project.rooms.model.CaseSiteDocument;
import dk.openesdh.repo.model.CaseDocument;

public interface CaseSiteDocumentsService {
    /**
     * Retrieves case or site document by provided document node ref
     * 
     * @param nodeRef
     *            Node ref of the document to retrieve
     * @return requested document object
     */
    public CaseSiteDocument getCaseSiteDocument(NodeRef nodeRef);

    /**
     * Retrieves documents of the provided case
     * 
     * @param caseNodeRef
     *            Node ref of the case to retrieve documents from
     * @return documents of the provided case
     */
    public List<CaseSiteDocument> getCaseDocuments(NodeRef caseNodeRef);

    /**
     * Retrieves a list of documents contained in the provided site
     * 
     * @param shortName
     *            Short name of the site to retrieve documents from
     * @return a list of documents contained in the provided site
     */
    public List<CaseSiteDocument> getCaseSiteDocuments(String shortName);

    /**
     * Retrieves JSONArray of site documents with same properties as for case
     * documents list.
     * 
     * @param siteShortName
     * @return
     */
    JSONArray getCaseSiteDocumentsJson(String siteShortName);

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
