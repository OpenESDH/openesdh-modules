package dk.openesdh.project.rooms.services;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
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
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.project.rooms.model.CaseSite.SiteMember;
import dk.openesdh.project.rooms.model.CaseSite.SiteParty;
import dk.openesdh.project.rooms.model.CaseSiteDocument;
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
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;

    private static final String ACCEPT_URL = "page/accept-invite";
    private static final String REJECT_URL = "page/reject-invite";

    @Override
    public NodeRef createCaseSite(CaseSite site) {
        return tr.runInTransaction(() -> createCaseSiteImpl(site));
    }

    protected void deleteSite(String shortName) {

        // TODO investigate why the SiteServiceImpl.beforePurgeNode() is not
        // called on siteService.deleteSite(), which should take care of this
        // groups/roles stuff

        // !!!!!!!! this is likely because the wrong "siteService" bean is used
        // !!!!! Use qualifier with bean id "SiteService".

        String siteGroup = siteService.getSiteGroup(shortName);
        if (authorityService.authorityExists(siteGroup)) {
            authorityService.deleteAuthority(siteGroup);
        }

        List<String> roles = siteService.getSiteRoles();
        for (String role : roles) {
            String roleGroup = siteService.getSiteRoleGroup(shortName, role);
            if (authorityService.authorityExists(roleGroup)) {
                authorityService.deleteAuthority(roleGroup);
            }
        }

        siteService.deleteSite(shortName);

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

        Pair<SiteInfo, NodeRef> siteDocLibPair = tr.runInNewTransaction(() -> {
            SiteInfo siteInfo = siteService.createSite("site-dashboard", site.getShortName(), site.getTitle(),
                    site.getDescription(),
                    site.getVisibility());

            Map<QName, Serializable> properties = new HashMap<>();
            properties.put(OpenESDHModel.PROP_OE_CASE_ID, site.getCaseId());
            nodeService.addAspect(siteInfo.getNodeRef(), OpenESDHModel.ASPECT_OE_CASE_ID, properties);
            createCaseIdFolder(siteInfo, site);
            NodeRef docLibrary = createDocumentLibrary(siteInfo);
            return new Pair<SiteInfo, NodeRef>(siteInfo, docLibrary);
        });

        final SiteInfo createdSite = siteDocLibPair.getFirst();
        final NodeRef documentLibrary = siteDocLibPair.getSecond();

        try {
            copySiteDocuments(site, documentLibrary);

            inviteSiteMembers(site);

            inviteSiteParties(site);
        } catch (Exception ex) {
            tr.runInNewTransaction(() -> {
                deleteSite(createdSite.getShortName());
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
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
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

        Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
        properties.put(ContentModel.PROP_DESCRIPTION, "Document Library");
        properties.put(ContentModel.PROP_NAME, SiteService.DOCUMENT_LIBRARY);
        properties.put(SiteModel.PROP_COMPONENT_ID, SiteService.DOCUMENT_LIBRARY);

        return nodeService.createNode(createdSite.getNodeRef(), ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, SiteService.DOCUMENT_LIBRARY),
                ContentModel.TYPE_FOLDER, properties).getChildRef();
    }

    protected void copySiteDocuments(CaseSite site, NodeRef targetFolder) throws Exception {
        for (String docRecordFolderNodeRef : site.getSiteDocuments()) {
            tr.runInNewTransaction(() -> {
                documentService.copyDocumentToFolder(new NodeRef(docRecordFolderNodeRef), targetFolder);
                return null;
            });
        }
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
        NodeRef siteRoot = siteService.getSiteRoot();

        if (Strings.isNullOrEmpty(caseId) || siteRoot == null) {
            return Collections.emptyList();
        }

        StringBuilder query = new StringBuilder(128);
        query.append("+TYPE:\"").append(SiteModel.TYPE_SITE).append('"').append(" AND (")
                .append(OpenESDHModel.OE_PREFIX).append(":caseId:\"").append(caseId).append("\"")
                .append(")");

        SearchParameters sp = new SearchParameters();
        sp.addStore(siteRoot.getStoreRef());
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        sp.setQuery(query.toString());

        ResultSet results = null;
        try {
            results = this.searchService.query(sp);
            return results.getNodeRefs().stream()
.map(nodeRef -> createCaseSite(nodeRef))
                    .collect(Collectors.toList());
        } catch (LuceneQueryParserException lqpe) {
            lqpe.printStackTrace();
            // Log the error but suppress is from the user
            // logger.error("LuceneQueryParserException with findSites()",
            // lqpe);
            return Collections.emptyList();
        } finally {
            if (results != null)
                results.close();
        }
    }

    private CaseSite createCaseSite(NodeRef siteNodeRef) {
        Map<QName, Serializable> properties = this.nodeService.getProperties(siteNodeRef);
        String caseId = null;
        if (properties.keySet().contains(OpenESDHModel.PROP_OE_CASE_ID)) {
            caseId = (String) properties.get(OpenESDHModel.PROP_OE_CASE_ID);
        }
        String shortName = (String) properties.get(ContentModel.PROP_NAME);
        String sitePreset = (String) properties.get(SiteModel.PROP_SITE_PRESET);
        String title = (String) properties.get(ContentModel.PROP_TITLE);
        String description = (String) properties.get(ContentModel.PROP_DESCRIPTION);

        // Get the visibility of the site
        SiteVisibility visibility = getSiteVisibility(siteNodeRef);

        String siteCreatorUserName = properties.get(ContentModel.PROP_CREATOR).toString();
        PersonInfo siteCreator = personService.getPerson(personService.getPerson(siteCreatorUserName));

        return new CaseSite(caseId, sitePreset, shortName, title, description, visibility, siteNodeRef.toString(),
                siteCreator);
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

    public List<CaseSiteDocument> getCaseSiteDocuments(String siteShortName) {
        NodeRef documentLibrary = siteService.getContainer(siteShortName, SiteService.DOCUMENT_LIBRARY);
        return nodeService.getChildAssocs(documentLibrary)
                .stream()
                .map(assoc -> caseSiteDocumentsService.getCaseSiteDocument(assoc.getChildRef()))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCaseSite(CaseSite site) {
        tr.runInTransaction(() -> {
            copySiteDocumentsToCase(site);
            deleteSite(site.getShortName());
            return null;
        });
    }

    protected void copySiteDocumentsToCase(CaseSite site) {

        if (site.getSiteDocuments().isEmpty()) {
            return;
        }

        List<CaseSiteDocument> siteDocsToCopy = site.getSiteDocuments()
                .stream()
                .map(docNodeRef -> caseSiteDocumentsService.getCaseSiteDocument(new NodeRef(docNodeRef)))
                .collect(Collectors.toList());

        NodeRef caseNodeRef = caseService.getCaseById(site.getCaseId());

        List<CaseSiteDocument> caseCurrentDocuments = caseSiteDocumentsService.getCaseDocuments(caseNodeRef);
        NodeRef caseDocsFolder = caseService.getDocumentsFolder(caseNodeRef);

        for (CaseSiteDocument siteDocument : siteDocsToCopy) {
            String siteDocumentName = FilenameUtils.removeExtension(siteDocument.getName());

            Optional<CaseSiteDocument> caseDocumentTwinForSiteDocument = caseCurrentDocuments
                    .stream()
                    .filter(caseDoc -> caseDoc.getName().equals(siteDocumentName))
                    .findAny();

            if (caseDocumentTwinForSiteDocument.isPresent()) {
                createCaseDocumentNewVersionFromSiteDocument(caseDocumentTwinForSiteDocument.get(), siteDocument);
            } else {
                caseSiteDocumentsService.copyDocumentFileToDocumentFolder(siteDocument, caseDocsFolder);
            }
        }
    }

    private void createCaseDocumentNewVersionFromSiteDocument(CaseSiteDocument caseDocument,
            CaseSiteDocument siteDocument) {
        if (ContentModel.TYPE_CONTENT.toString().equals(siteDocument.getType())) {
            caseSiteDocumentsService.createNewVersionOfCaseMainDocument(
                    new NodeRef(caseDocument.getNodeRef()), new NodeRef(siteDocument.getNodeRef()));
        }else{
            caseSiteDocumentsService.createNewVersionOfCaseDocument(
                    new NodeRef(caseDocument.getNodeRef()), new NodeRef(siteDocument.getNodeRef()));
        }
    }

    @Override
    public List<CaseSite> getCaseSites() {
        return nodeService.getChildAssocs(siteService.getSiteRoot(), new HashSet<QName>(Arrays.asList(SiteModel.TYPE_SITE)))
              .stream()
.map(assoc -> createCaseSite(assoc.getChildRef()))
              .collect(Collectors.toList());
    }
}
