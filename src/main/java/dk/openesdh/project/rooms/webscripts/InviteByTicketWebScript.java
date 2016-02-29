package dk.openesdh.project.rooms.webscripts;

import java.util.Map;

import org.alfresco.repo.invitation.site.InviteInfo;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.repo.tenant.TenantUtil;
import org.alfresco.repo.web.scripts.invite.InviteByTicket;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.invitation.InvitationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.stereotype.Component;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;

import dk.openesdh.repo.webscripts.utils.WebScriptUtils;

@Component
@WebScript(description = "Provides api to get invitations to join project room by provided ticket", families = "Case Sites")
public class InviteByTicketWebScript extends InviteByTicket {

    private static final String PARAM_INVITEE_USER_NAME = "inviteeUserName";

    @Autowired
    @Qualifier("PersonService")
    private PersonService personService;
    private TenantService tenantService;

    @Authentication(AuthenticationType.NONE)
    @Uri("/api/openesdh/invite/{inviteId}/{inviteTicket}")
    public Resolution getInviteByTicket(WebScriptRequest req) throws JSONException {

        String tenantDomain = TenantService.DEFAULT_DOMAIN;

        if (tenantService.isEnabled()) {
            String inviteeUserName = req.getParameter(PARAM_INVITEE_USER_NAME);
            if (inviteeUserName != null) {
                tenantDomain = tenantService.getUserDomain(inviteeUserName);
            }
        }

        // run as system user
        String mtAwareSystemUser = tenantService.getDomainUser(AuthenticationUtil.getSystemUserName(),
                tenantDomain);

        JSONObject json = TenantUtil.runAsSystemTenant(() -> {
            return getInvite(req);
        } , tenantDomain);

        // authenticate as system for the rest of the webscript
        AuthenticationUtil.setRunAsUser(mtAwareSystemUser);

        return WebScriptUtils.jsonResolution(json);
    }

    private JSONObject getInvite(WebScriptRequest req) throws JSONException {
        Map<String, Object> map = executeImpl(req, new Status());
        InviteInfo invite = (InviteInfo) map.get("invite");

        JSONObject jsonInvite = new JSONObject();
        jsonInvite.put("invitationStatus", invite.getInvitationStatus());
        jsonInvite.put("inviteId", invite.getInviteId());
        jsonInvite.put("role", invite.getRole());
        jsonInvite.put("sentInviteDate", invite.getSentInviteDate().getTime());

        JSONObject jsonSite = new JSONObject();
        jsonSite.put("shortName", invite.getSiteInfo().getShortName());
        jsonSite.put("title", invite.getSiteInfo().getTitle());
        jsonInvite.put("site", jsonSite);

        PersonInfo inviter = personService.getPerson(personService.getPerson(invite.getInviterUserName()));
        JSONObject jsonInviter = new JSONObject();
        jsonInviter.put("userName", inviter.getUserName());
        jsonInviter.put("firstName", inviter.getFirstName());
        jsonInviter.put("lastName", inviter.getLastName());
        jsonInvite.put("inviter", jsonInviter);

        PersonInfo invitee = personService.getPerson(personService.getPerson(invite.getInviteeUserName()));
        JSONObject jsonInvitee = new JSONObject();
        jsonInvitee.put("userName", invitee.getUserName());
        jsonInvitee.put("firstName", invitee.getFirstName());
        jsonInvitee.put("lastName", invitee.getLastName());
        jsonInvite.put("invitee", jsonInvitee);

        JSONObject json = new JSONObject();
        json.put("invite", jsonInvite);
        return json;
    }

    @Autowired
    @Qualifier("ServiceRegistry")
    @Override
    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        super.setServiceRegistry(serviceRegistry);
    }

    @Autowired
    @Qualifier("InvitationService")
    @Override
    public void setInvitationService(InvitationService invitationService) {
        super.setInvitationService(invitationService);
    }

    @Autowired
    @Qualifier("SiteService")
    @Override
    public void setSiteService(SiteService siteService) {
        super.setSiteService(siteService);
    }

    @Autowired
    @Qualifier("tenantService")
    @Override
    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
        super.setTenantService(tenantService);
    }

}
