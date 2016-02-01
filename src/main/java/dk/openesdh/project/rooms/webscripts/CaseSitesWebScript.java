package dk.openesdh.project.rooms.webscripts;

import java.io.IOException;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.stereotype.Component;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Attribute;
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.project.rooms.services.CaseSitesService;
import dk.openesdh.repo.webscripts.utils.WebScriptUtils;

@Component
@WebScript(description = "Provides api for case sites/project rooms", families = "Case Sites")
public class CaseSitesWebScript {

    @Autowired
    @Qualifier("CaseSitesService")
    private CaseSitesService caseSitesService;
    @Autowired
    @Qualifier("SiteService")
    private SiteService siteService;
    
    @Attribute
    public CaseSite getSite(WebScriptRequest req) throws IOException {
        return WebScriptUtils.isContentTypeJson(req) ? 
                (CaseSite) WebScriptUtils.readJson(CaseSite.class, req) : null;
    }

    @Uri("/api/openesdh/site/{shortName}/exists")
    public Resolution siteExists(@UriVariable("shortName") String shortName) throws JSONException {
        boolean isSiteExists = siteService.hasSite(shortName);
        JSONObject json = new JSONObject();
        json.put("siteExists", isSiteExists);
        return WebScriptUtils.jsonResolution(json);
    }

    @Uri("/api/openesdh/case/{caseId}/sites")
    public Resolution getSites(@UriVariable(WebScriptUtils.CASE_ID) String caseId) throws IOException {
        return WebScriptUtils.jsonResolution(caseSitesService.getCaseSites(caseId));
    }

    @Uri("/api/openesdh/case/sites/{shortName}/documents")
    public Resolution getSiteDocuments(@UriVariable("shortName") String shortName) {
        return WebScriptUtils.jsonResolution(caseSitesService.getCaseSiteDocuments(shortName));
    }
    
    @Uri(value = "/api/openesdh/case/sites", method = HttpMethod.POST)
    public Resolution createSite(@Attribute CaseSite site) throws IOException {
        NodeRef siteNodeRef = caseSitesService.createCaseSite(site);
        site.setNodeRef(siteNodeRef.toString());
        return WebScriptUtils.jsonResolution(site);
    }

    @Uri(value = "/api/openesdh/case/sites", method = HttpMethod.PUT)
    public Resolution updateSite(@Attribute CaseSite site) throws IOException {
        caseSitesService.updateCaseSite(site);
        return WebScriptUtils.jsonResolution(site);
    }

    @Uri(value = "/api/openesdh/case/sites", method = HttpMethod.DELETE)
    public void deleteSite(@Attribute CaseSite site) {
        caseSitesService.deleteCaseSite(site);
    }
}
