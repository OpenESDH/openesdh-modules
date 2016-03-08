package dk.openesdh.project.rooms.services.activity;

import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;

import dk.openesdh.project.rooms.model.CaseSite;

public interface CaseSiteActivityService {

    String BEAN_ID = "CaseSiteActivityService";

    String ACTIVITY_TYPE_CASE_SITE_CREATE = "dk.openesdh.case.site-create";

    String ACTIVITY_TYPE_CASE_SITE_CLOSE = "dk.openesdh.case.site-close";

    String ACTIVITY_TYPE_CASE_SITE_MEMBERS_INVITED = "dk.openesdh.case.site.members-invited";

    String ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT = "dk.openesdh.case.site.document-new";

    String ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT_ATTACHMENT = "dk.openesdh.case.site.document.attachment-new";

    String ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT_VERSION = "dk.openesdh.case.site.document.version-new";

    String ACTIVITY_TYPE_CASE_SITE_NEW_DOCUMENT_ATTACHMENT_VERSION = "dk.openesdh.case.site.document.attachment.version-new";

    String ACTIVITY_TYPE_CASE_SITE_USER_JOINED = "org.alfresco.site.user-joined";

    String ACTIVITY_TYPE_CASE_SITE_USER_LEFT = "org.alfresco.site.user-left";

    String INVITATION_INVITESENDER_EMAIL_ROLE = "invitation.invitesender.email.role.";

    void postOnCaseSiteCreate(CaseSite site);

    void postOnCaseSiteClose(CaseSite site, Set<String> membersToNotify);

    void postOnCaseSiteParticipantsInvited(CaseSite site);

    void postOnCaseSiteNewDocument(NodeRef siteRef, NodeRef docNodeRef);

    void postOnCaseSiteNewDocumentAttachment(NodeRef siteRef, NodeRef docNodeRef);

    void postOnCaseSiteNewDocumentVersion(NodeRef siteRef, NodeRef attachmentRef);

    void postOnCaseSiteNewDocumentAttachmentVersion(NodeRef siteRef, NodeRef attachmentRef);

    Set<String> getSiteMembersToNotify(String shortName);
}
