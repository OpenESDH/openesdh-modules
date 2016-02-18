package dk.openesdh.project.rooms.services;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParserException;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.invitation.Invitation;
import org.alfresco.service.cmr.invitation.InvitationService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.project.rooms.model.CaseSite.SiteMember;
import dk.openesdh.project.rooms.model.CaseSite.SiteParty;
import dk.openesdh.repo.model.ContactInfo;
import dk.openesdh.repo.model.OpenESDHModel;
import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.contacts.ContactService;
import dk.openesdh.repo.services.documents.DocumentService;

@Service("CaseSitesService")
public class CaseSitesServiceImpl implements CaseSitesService {

    @Autowired
    @Qualifier("SiteService")
    private SiteService siteService;

    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;

    @Autowired
    @Qualifier("InvitationService")
    private InvitationService invitationService;

    @Autowired
    @Qualifier("DocumentService")
    private DocumentService documentService;

    @Autowired
    @Qualifier("ContactService")
    private ContactService contactService;

    @Autowired
    @Qualifier("AuthorityService")
    private AuthorityService authorityService;

    @Autowired
    @Qualifier("SearchService")
    private SearchService searchService;

    @Autowired
    @Qualifier("PermissionService")
    private PermissionService permissionService;

    @Autowired
    @Qualifier("CaseService")
    private CaseService caseService;

    @Autowired
    @Qualifier("PersonService")
    private PersonService personService;

    @Autowired
    @Qualifier("CaseSiteDocumentsService")
    private CaseSiteDocumentsService caseSiteDocumentsService;
    @Autowired
    @Qualifier("policyComponent")
    private PolicyComponent policyComponent;

    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;

    private static final String ACCEPT_URL = "page/accept-invite";
    private static final String REJECT_URL = "page/reject-invite";
    private static final String CASE_SITES_QUERY = "SELECT s.cmis:objectId FROM st:site AS s JOIN oe:caseId AS cid ON s.cmis:objectId=cid.cmis:objectId";

    @Override
    public NodeRef createCaseSite(CaseSite site) {
        return tr.runInTransaction(() -> createCaseSiteImpl(site));
    }

    @PostConstruct
    public void init() {
        this.policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, SiteModel.TYPE_SITE,
                new JavaBehaviour(this, "beforeDeleteSite"));
    }

    // Fix for non-working SiteService.beforePurgeNode
    public void beforeDeleteSite(NodeRef nodeRef) {
        String shortName = siteService.getSiteShortName(nodeRef);
        tr.runAsSystem(() -> {
            deleteSiteGroups(shortName);
            return null;
        });
    }

    private void deleteSiteGroups(String shortName) {
        String siteGroup = siteService.getSiteGroup(shortName);
        if (authorityService.authorityExists(siteGroup)) {
            authorityService.deleteAuthority(siteGroup, false);
        }

        List<String> roles = siteService.getSiteRoles();
        for (String role : roles) {
            String roleGroup = siteService.getSiteRoleGroup(shortName, role);
            if (authorityService.authorityExists(roleGroup)) {
                authorityService.deleteAuthority(roleGroup, false);
            }
        }
    }

    protected NodeRef createCaseSiteImpl(CaseSite site)
            throws Exception {

        /**
         * There is an issue related to copying documents from case to site
         * concerned with ASSOC_DOC_MAIN relations, which is addressed in
         * DocumentBehaviour.
         * 
         * However for some reason the method handler for OnCopyCompletePolicy
         * is called only once per transaction commit, thus resulting in the
         * ASSOC_DOC_MAIN restored for the first copied document only. 
         * All the rest copied documents remain without ASSOC_DOC_MAIN restored, which
         * results in integrity violations.
         * 
         * To address this issue separate transactions are used for creating a
         * site and copying each document to the site.
         */

        // Set site description otherwise it becomes non-searchable for some
        // reason. Perhaps it's an alfresco bug.
        String siteDescription = StringUtils.isEmpty(site.getDescription()) ? site.getTitle()
                : site.getDescription();
        Pair<SiteInfo, NodeRef> siteDocLibPair = tr.runInNewTransaction(() -> {
            SiteInfo siteInfo = siteService.createSite("site-dashboard", site.getShortName(), site.getTitle(),
                    siteDescription, SiteVisibility.PRIVATE);

            Map<QName, Serializable> properties = new HashMap<>();
            properties.put(OpenESDHModel.PROP_OE_CASE_ID, site.getCaseId());
            nodeService.addAspect(siteInfo.getNodeRef(), OpenESDHModel.ASPECT_OE_CASE_ID, properties);
            createCaseIdFolder(siteInfo, site);
            NodeRef docLibrary = createDocumentLibrary(siteInfo);
            return new Pair<>(siteInfo, docLibrary);
        });

        final SiteInfo createdSite = siteDocLibPair.getFirst();
        final NodeRef documentLibrary = siteDocLibPair.getSecond();

        try {
            caseSiteDocumentsService.copySiteDocuments(site, documentLibrary);

            inviteSiteMembers(site);

            inviteSiteParties(site);
        } catch (Exception ex) {
            tr.runInNewTransactionAsAdmin(() -> {
                siteService.deleteSite(createdSite.getShortName());
                return null;
            });
            throw ex;
        }

        return createdSite.getNodeRef();
    }

    /**
     * Create a hidden marker folder to store case id. Compulsory for site
     * delete auditing since there no other way to get case id.
     * 
     * @param createdSite
     * @param site
     * @return
     */
    protected NodeRef createCaseIdFolder(SiteInfo createdSite, CaseSite site) {
        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_DESCRIPTION, "case Id" + site.getCaseId());
        properties.put(ContentModel.PROP_NAME, site.getCaseId());
        properties.put(SiteModel.PROP_COMPONENT_ID, SiteService.DOCUMENT_LIBRARY);

        NodeRef caseIdFolder = nodeService.createNode(createdSite.getNodeRef(), ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, site.getCaseId()),
                ContentModel.TYPE_FOLDER, properties).getChildRef();

        nodeService.addAspect(caseIdFolder, ContentModel.ASPECT_HIDDEN, null);

        return caseIdFolder;
    }

    protected NodeRef createDocumentLibrary(SiteInfo createdSite){

        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_DESCRIPTION, "Document Library");
        properties.put(ContentModel.PROP_NAME, SiteService.DOCUMENT_LIBRARY);
        properties.put(SiteModel.PROP_COMPONENT_ID, SiteService.DOCUMENT_LIBRARY);

        NodeRef documentLibrary = nodeService.createNode(createdSite.getNodeRef(), ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, SiteService.DOCUMENT_LIBRARY),
                ContentModel.TYPE_FOLDER, properties).getChildRef();
        nodeService.addAspect(documentLibrary, OpenESDHModel.ASPECT_DOCUMENT_CONTAINER, null);
        return documentLibrary;
    }

    protected void inviteSiteMembers(CaseSite site) {
        for (SiteMember member : site.getSiteMembers()) {
            invitationService.inviteNominated(member.getAuthority(), Invitation.ResourceType.WEB_SITE,
                    site.getShortName(), member.getRole(), ACCEPT_URL, REJECT_URL);
        }
    }

    protected void inviteSiteParties(CaseSite site) {
        for (SiteParty party : site.getSiteParties()) {
            ContactInfo partyContact = contactService.getContactInfo(new NodeRef(party.getNodeRef()));
            invitationService.inviteNominated(partyContact.getName(), "", partyContact.getEmail(),
                    Invitation.ResourceType.WEB_SITE, site.getShortName(), party.getRole(), ACCEPT_URL, REJECT_URL);
        }
    }

    @Override
    public List<CaseSite> getCaseSites(String caseId) {
        if (Strings.isNullOrEmpty(caseId)) {
            return Collections.emptyList();
        }

        String query = new StringBuilder(256).append(CASE_SITES_QUERY).append(" WHERE cid.oe:caseId='")
                .append(caseId).append("'").toString();

        return queryCaseSites(query);
    }

    private List<CaseSite> queryCaseSites(String query) {
        NodeRef siteRoot = siteService.getSiteRoot();
        if (Objects.isNull(siteRoot)) {
            return Collections.emptyList();
        }

        SearchParameters sp = new SearchParameters();
        sp.addStore(siteRoot.getStoreRef());
        sp.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
        sp.setQuery(query);

        ResultSet results = null;
        try {
            results = this.searchService.query(sp);
            return results.getNodeRefs().stream()
                    .map(nodeRef -> getCaseSite(nodeRef))
                    .collect(Collectors.toList());
        } catch (LuceneQueryParserException lqpe) {
            lqpe.printStackTrace();
            return Collections.emptyList();
        } finally {
            if (results != null)
                results.close();
        }
    }

    private CaseSite getCaseSite(NodeRef siteNodeRef) {
        Map<QName, Serializable> properties = this.nodeService.getProperties(siteNodeRef);
        String caseId = null;
        if (properties.keySet().contains(OpenESDHModel.PROP_OE_CASE_ID)) {
            caseId = (String) properties.get(OpenESDHModel.PROP_OE_CASE_ID);
        }

        CaseSite site = new CaseSite();
        site.setCaseId(caseId);
        site.setShortName((String) properties.get(ContentModel.PROP_NAME));
        site.setSitePreset((String) properties.get(SiteModel.PROP_SITE_PRESET));
        site.setTitle((String) properties.get(ContentModel.PROP_TITLE));
        site.setDescription((String) properties.get(ContentModel.PROP_DESCRIPTION));
        site.setCreatedDate((Date) properties.get(ContentModel.PROP_CREATED));
        site.setLastModifiedDate((Date) properties.get(ContentModel.PROP_MODIFIED));
        site.setVisibility(getSiteVisibility(siteNodeRef));

        String siteCreatorUserName = properties.get(ContentModel.PROP_CREATOR).toString();
        PersonInfo siteCreator = personService.getPerson(personService.getPerson(siteCreatorUserName));
        site.setCreator(siteCreator);

        site.setDocumentsFolderRef(
                siteService.getContainer(site.getShortName(), SiteService.DOCUMENT_LIBRARY).toString());

        return site;
    }

    private SiteVisibility getSiteVisibility(NodeRef siteNodeRef) {
        // Get the visibility value stored in the repo
        String visibilityValue = (String) this.nodeService.getProperty(siteNodeRef, SiteModel.PROP_SITE_VISIBILITY);

        // To maintain backwards compatibility calculate the visibility from the
        // permissions if there is no value specified on the site node
        if (visibilityValue != null) {
            return SiteVisibility.valueOf(visibilityValue);
        }

        // Examine each permission to see if this is a public site or not
        try {
            Predicate<AccessPermission> isPublicPermission = permission -> PermissionService.ALL_AUTHORITIES.equals(permission.getAuthority())
                    && SiteModel.SITE_CONSUMER.equals(permission.getPermission());
            
            return this.permissionService.getAllSetPermissions(siteNodeRef).stream().anyMatch(isPublicPermission)
                    ? SiteVisibility.PUBLIC : SiteVisibility.PRIVATE;
        } catch (AccessDeniedException ae) {
            // We might not have permission to examine the permissions
            return SiteVisibility.PRIVATE;
        }

    }

    @Override
    public void updateCaseSite(CaseSite site) {
        SiteInfo siteInfo = siteService.getSite(site.getShortName());
        siteInfo.setDescription(site.getDescription());
        siteInfo.setTitle(site.getTitle());
        siteInfo.setVisibility(site.getVisibility());
        siteService.updateSite(siteInfo);
    }

    @Override
    public void inviteParticipants(CaseSite site) {
        inviteSiteMembers(site);
        inviteSiteParties(site);
    }

    @Override
    public List<CaseSite> getCaseSites() {
        return queryCaseSites(CASE_SITES_QUERY);
    }

    @Override
    public CaseSite getCaseSite(String shortName) {
        NodeRef siteNodeRef = siteService.getSite(shortName).getNodeRef();
        return getCaseSite(siteNodeRef);
    }

    @Override
    public void closeCaseSite(CaseSite siteData) {
        CaseSite site = getCaseSite(siteData.getShortName());
        site.setSiteDocuments(siteData.getSiteDocuments());
        tr.runInTransaction(() -> {
            tr.runInNewTransaction(() -> {
                caseSiteDocumentsService.copySiteDocumentsBackToCase(site);
                return null;
            });
            siteService.deleteSite(siteData.getShortName());
            return null;
        });
    }
}
