package dk.openesdh.project.rooms.services.activity;

import static dk.openesdh.repo.services.activities.CaseActivityService.ATTACHMENT_TITLE;
import static dk.openesdh.repo.services.activities.CaseActivityService.CASE_ID;
import static dk.openesdh.repo.services.activities.CaseActivityService.CASE_TITLE;
import static dk.openesdh.repo.services.activities.CaseActivityService.DOC_TITLE;
import static dk.openesdh.repo.services.activities.CaseActivityService.MEMBER;
import static dk.openesdh.repo.services.activities.CaseActivityService.MODIFIER;
import static dk.openesdh.repo.services.activities.CaseActivityService.MODIFIER_DISPLAY_NAME;
import static dk.openesdh.repo.services.activities.CaseActivityService.ROLE;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.project.rooms.services.CaseSitesService;
import dk.openesdh.repo.services.activities.ActivitiesFeedMailMessageResolver;
import dk.openesdh.repo.services.activities.ActivitiesFeedMailMessageResolver.MessageResolver;
import dk.openesdh.repo.services.activities.CaseActivityService;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.DocumentService;

@Service(CaseSiteActivityService.BEAN_ID)
public class CaseSiteActivityServiceImpl implements CaseSiteActivityService {
    
    public static final String SITE_TITLE = "siteTitle";
    public static final String SITE_MEMBER_ROLE = "siteMemberRole";
    public static final String MEMBER_TITLE = "title";
    public static final String SITE_NETWORK = "siteNetwork";
    
    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;

    @Autowired
    @Qualifier("SiteService")
    private SiteService siteService;

    @Autowired
    @Qualifier("PersonService")
    private PersonService personService;

    @Autowired
    @Qualifier(CaseService.BEAN_ID)
    private CaseService caseService;

    @Autowired
    @Qualifier(DocumentService.BEAN_ID)
    private DocumentService documentService;

    @Autowired
    @Qualifier(CaseActivityService.BEAN_ID)
    private CaseActivityService caseActivityService;

    @Autowired
    @Qualifier(CaseSitesService.BEAN_ID)
    private CaseSitesService caseSiteService;

    @Autowired
    @Qualifier(ActivitiesFeedMailMessageResolver.BEAN_ID)
    private ActivitiesFeedMailMessageResolver messageResolvers;

    @PostConstruct
    public void init() {

        Map<String, Function<String, String>> parameterResolvers = new HashMap<>();
        parameterResolvers.put(SITE_MEMBER_ROLE, this::getLocalizedSiteMemberRole);
        parameterResolvers.put(ROLE, this::getLocalizedSiteMemberRole);
        parameterResolvers.put(SITE_NETWORK, this::getSiteTitle);

        messageResolvers.registerMesageResolver(ACTIVITY_TYPE_CASE_SITE_CREATE, MODIFIER_DISPLAY_NAME, SITE_TITLE, CASE_TITLE, CASE_ID);
        messageResolvers.registerMesageResolver(ACTIVITY_TYPE_CASE_SITE_CLOSE, MODIFIER_DISPLAY_NAME, SITE_TITLE, CASE_TITLE, CASE_ID);
        messageResolvers.registerMesageResolver(ACTIVITY_TYPE_CASE_SITE_MEMBERS_INVITED, new MessageResolver(parameterResolvers, MODIFIER_DISPLAY_NAME, MEMBER, SITE_TITLE, SITE_MEMBER_ROLE));
        
        messageResolvers.registerMesageResolver(ACTIVITY_TYPE_CASE_SITE_USER_JOINED, new MessageResolver(parameterResolvers, MEMBER_TITLE, SITE_NETWORK, ROLE));
        messageResolvers.registerMesageResolver(ACTIVITY_TYPE_CASE_SITE_USER_LEFT, new MessageResolver(parameterResolvers, MEMBER_TITLE, SITE_NETWORK));
        
        messageResolvers.registerMesageResolver(ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT, MODIFIER_DISPLAY_NAME, DOC_TITLE, SITE_TITLE);
        messageResolvers.registerMesageResolver(ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT_VERSION, MODIFIER_DISPLAY_NAME, DOC_TITLE, SITE_TITLE);
        messageResolvers.registerMesageResolver(ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT_ATTACHMENT, MODIFIER_DISPLAY_NAME, ATTACHMENT_TITLE, DOC_TITLE, SITE_TITLE);
        messageResolvers.registerMesageResolver(ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT_ATTACHMENT_VERSION, MODIFIER_DISPLAY_NAME, ATTACHMENT_TITLE, DOC_TITLE, SITE_TITLE);
    }

    @Override
    public void postOnCaseSiteCreate(CaseSite site) {
        caseActivityService.postActivity(site.getCaseId(), ACTIVITY_TYPE_CASE_SITE_CREATE, (caseNodeRef) -> {
            JSONObject json = caseActivityService.createNewActivity(site.getCaseId(), caseNodeRef);
            json.put(SITE_TITLE, site.getTitle());
            return json;
        });
    }

    @Override
    public void postOnCaseSiteClose(CaseSite site, Set<String> membersToNotify) {
        String caseId = site.getCaseId();
        NodeRef caseNodeRef = caseService.getCaseById(caseId);
        JSONObject json = caseActivityService.createNewActivity(caseId, caseNodeRef);
        json.put(SITE_TITLE, site.getTitle());

        membersToNotify.addAll(caseActivityService.getCaseMembersToNotify(caseId, caseNodeRef));

        String activity = json.toJSONString();
        membersToNotify.forEach(userId -> caseActivityService.notifyUser(ACTIVITY_TYPE_CASE_SITE_CLOSE, userId, activity));
    }

    @Override
    public void postOnCaseSiteParticipantsInvited(CaseSite site) {
        Set<String> usersToNotify = getSiteMembersToNotify(site.getShortName());
        if (usersToNotify.isEmpty()) {
            return;
        }

        JSONObject activity = createNewSiteActivity(site.getTitle());

        getInvitedParticipants(site).entrySet().stream().map(invited -> {
            activity.put(MEMBER, invited.getKey());
            activity.put(SITE_MEMBER_ROLE, invited.getValue());
            return activity.toJSONString();
        }).forEach(
                jsonData -> postActivity(ACTIVITY_TYPE_CASE_SITE_MEMBERS_INVITED, usersToNotify, jsonData)
        );
    }

    @Override
    public void postOnCaseSiteNewDocument(NodeRef siteRef, NodeRef docNodeRef) {
        postSiteDocumentActivity(ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT, siteRef, docNodeRef);
    }

    @Override
    public void postOnCaseSiteNewDocumentVersion(NodeRef siteRef, NodeRef docNodeRef) {
        postSiteDocumentActivity(ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT_VERSION, siteRef, docNodeRef);
    }

    @Override
    public void postOnCaseSiteNewDocumentAttachment(NodeRef siteRef, NodeRef attachmentRef) {
        postSiteDocumentAttachmentActivity(ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT_ATTACHMENT, siteRef, attachmentRef);
    }

    @Override
    public void postOnCaseSiteNewDocumentAttachmentVersion(NodeRef siteRef, NodeRef attachmentRef) {
        postSiteDocumentAttachmentActivity(ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT_ATTACHMENT_VERSION, siteRef,
                attachmentRef);
    }

    @Override
    public Set<String> getSiteMembersToNotify(String shortName) {
        String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
        return siteService.listMembers(shortName, null, null, 0).keySet().stream()
                .filter(userName -> !userName.equals(currentUser)).collect(Collectors.toSet());
    }

    private void postSiteDocumentActivity(String activityType, NodeRef siteRef, NodeRef docNodeRef) {
        CaseSite site = caseSiteService.getCaseSite(siteRef);
        JSONObject json = createNewSiteActivity(site.getTitle());
        json.put(DOC_TITLE, nodeService.getProperty(docNodeRef, ContentModel.PROP_TITLE));
        postActivity(activityType, getSiteMembersToNotify(site.getShortName()), json.toJSONString());
    }

    private void postSiteDocumentAttachmentActivity(String activityType, NodeRef siteRef, NodeRef attachmentRef) {
        CaseSite site = caseSiteService.getCaseSite(siteRef);
        JSONObject json = createNewSiteActivity(site.getTitle());
        NodeRef docRecord = documentService.getDocRecordNodeRef(attachmentRef);
        json.put(ATTACHMENT_TITLE, nodeService.getProperty(attachmentRef, ContentModel.PROP_NAME));
        json.put(DOC_TITLE, nodeService.getProperty(docRecord, ContentModel.PROP_TITLE));
        postActivity(activityType, getSiteMembersToNotify(site.getShortName()), json.toJSONString());
    }

    private void postActivity(String activityType, Set<String> usersToNotify, String json) {
        usersToNotify.forEach(userId -> caseActivityService.notifyUser(activityType, userId, json));
    }

    private Map<String, String> getInvitedParticipants(CaseSite site) {
        Map<String, String> invited = new HashMap<>();
        site.getSiteMembers().forEach(member -> invited.put(member.getName(), member.getRole()));
        site.getSiteParties().forEach(party -> invited.put(party.getName(), party.getRole()));
        return invited;
    }

    private JSONObject createNewSiteActivity(String siteTitle) {
        PersonInfo currentUserInfo = personService
                .getPerson(personService.getPerson(AuthenticationUtil.getFullyAuthenticatedUser()));
        String currentUserDisplayName = currentUserInfo.getFirstName() + " " + currentUserInfo.getLastName();
        JSONObject json = new JSONObject();
        json.put(SITE_TITLE, siteTitle);
        json.put(MODIFIER, AuthenticationUtil.getFullyAuthenticatedUser());
        json.put(MODIFIER_DISPLAY_NAME, currentUserDisplayName);
        return json;
    }

    private String getLocalizedSiteMemberRole(String value) {
        return ActivitiesFeedMailMessageResolver.getMessage(INVITATION_INVITESENDER_EMAIL_ROLE + value);
    }

    private String getSiteTitle(String shortName) {
        if (!siteService.hasSite(shortName)) {
            return shortName;
        }
        return siteService.getSite(shortName).getTitle();
    }
}
