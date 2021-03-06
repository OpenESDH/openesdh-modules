package dk.openesdh.project.rooms.services;

import java.util.List;
import java.util.regex.Pattern;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.repo.services.cases.CaseService;

public interface CaseSitesService {

    String BEAN_ID = "CaseSitesService";

    // Mind the same RegEx in CaseSitesService.js
    public static final String SITE_SHORT_NAME_REGEX = "([a-zA-Z0-9_]+)";

    public static final String SITES_ROOT = "/app:company_home/st:sites";

    public static final Pattern SITE_PATH_SHORT_NAME_CASE_ID_REG_EX = Pattern.compile(SITES_ROOT + "/"
            + NamespaceService.CONTENT_MODEL_PREFIX + ":" + CaseSitesService.SITE_SHORT_NAME_REGEX + "/"
            + NamespaceService.CONTENT_MODEL_PREFIX + ":(" + CaseService.CASE_ID_PATTERN_STRING + ")$");

    /**
     * Creates a site for provided case
     * 
     * @param site
     *            - the case site info to create a new site
     * @return node ref of the created site
     */
    public NodeRef createCaseSite(CaseSite site);

    /**
     * Retrieves all sites regardless of case they belong to
     * 
     * @return List of all sites
     */
    public List<CaseSite> getCaseSites();

    /**
     * Retrieves sites list by provided case id
     * 
     * @param caseId
     *            Case id to retrieve sites for
     * @return
     */
    public List<CaseSite> getCaseSites(String caseId);

    /**
     * Updates provided case site details
     * 
     * @param site
     */
    public void updateCaseSite(CaseSite site);

    /**
     * Retrieves site info by provide short name
     * 
     * @param shortName
     * @return
     */
    CaseSite getCaseSite(String shortName);

    /**
     * Closes case site and copies provided documents back to the case
     * 
     * @param site
     * @return
     */
    CaseSite closeCaseSite(CaseSite site);

    /**
     * Invites participants to the provided site.
     * 
     * @param site
     * @return
     */
    CaseSite inviteParticipants(CaseSite site);

    /**
     * Retrieves site info by provided nodeRef
     * 
     * @param shortName
     * @return
     */
    CaseSite getCaseSite(NodeRef siteNodeRef);

}
