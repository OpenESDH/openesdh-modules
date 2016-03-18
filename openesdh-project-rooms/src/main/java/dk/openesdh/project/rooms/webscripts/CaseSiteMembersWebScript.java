package dk.openesdh.project.rooms.webscripts;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteMemberInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;

import dk.openesdh.repo.utils.JSONArrayCollector;
import dk.openesdh.repo.webscripts.utils.WebScriptUtils;

@Component
@WebScript(description = "Retrieves members of the provided case site", families = "Case Sites")
public class CaseSiteMembersWebScript {
    private static final String FULL_NAME = "fullName";
    private static final String USER_NAME = "userName";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final String EMAIL = "email";
    private static final String ROLE = "role";
    private static final String AUTHORITY = "authority";

    @Autowired
    @Qualifier("SiteService")
    private SiteService siteService;
    @Autowired
    @Qualifier("PersonService")
    private PersonService personService;
    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;

    @Uri(value = "/api/openesdh/case/sites/{shortName}/members", defaultFormat = WebScriptUtils.JSON)
    public Resolution getMembers(@UriVariable("shortName") String siteShortName) {
        JSONArray members = siteService.listMembersInfo(siteShortName, null, null, 0, true)
                .stream()
                .map(this::getMemberInfo)
                .collect(JSONArrayCollector.json());
        return WebScriptUtils.jsonResolution(members);
    }
    
    private JSONObject getMemberInfo(SiteMemberInfo member){
        try {
            JSONObject info = new JSONObject();
            info.put(ROLE, member.getMemberRole());
            info.put(AUTHORITY, getPersonInfo(member.getMemberName()));
            return info;
        } catch (JSONException e) {
            throw new AlfrescoRuntimeException("Error getting site member info", e);
        }
    }

    private JSONObject getPersonInfo(String userName) throws JSONException {
        NodeRef personRef = personService.getPerson(userName);
        Map<QName, Serializable> props = nodeService.getProperties(personRef);
        JSONObject info = new JSONObject();
        info.put(FULL_NAME, props.get(ContentModel.PROP_USERNAME));
        info.put(USER_NAME, props.get(ContentModel.PROP_USERNAME));
        info.put(FIRST_NAME, props.get(ContentModel.PROP_FIRSTNAME));
        info.put(LAST_NAME, props.get(ContentModel.PROP_LASTNAME));
        info.put(EMAIL, props.get(ContentModel.PROP_EMAIL));
        return info;
    }
}
