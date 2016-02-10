package dk.openesdh.project.rooms.services;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;

import dk.openesdh.project.rooms.model.CaseSiteDocument;

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
}
