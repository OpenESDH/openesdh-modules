package dk.openesdh.casetemplates.webscripts;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;

import dk.openesdh.casetemplates.services.CaseTemplateService;
import dk.openesdh.casetemplates.services.CaseTemplatesFolderService;
import dk.openesdh.repo.webscripts.utils.WebScriptUtils;

@Component
@WebScript(description = "Provides api for case templates", families = "Case Templates")
public class CaseTemplatesWebScript {

    @Autowired
    @Qualifier("CaseTemplateService")
    private CaseTemplateService caseTemplateService;

    @Autowired
    @Qualifier("CaseTemplatesFolderService")
    private CaseTemplatesFolderService caseTemplatesFolderService;

    @Uri("/api/openesdh/case/templates/folder")
    public Resolution getTemplatesFolderRef() {
        return WebScriptUtils.jsonResolution(caseTemplatesFolderService.getCaseTemplatesRootFolder());
    }

    @Uri("/api/openesdh/case/templates/{caseType}")
    public Resolution getTemplates(@UriVariable("caseType") String caseType) {
        return WebScriptUtils.jsonResolution(caseTemplateService.getCaseTemplates(caseType));
    }

    @Uri("/api/openesdh/case/template/{protocol}/{storeId}/{id}")
    public Resolution getTemplate(@UriVariable("protocol") String protocol, @UriVariable("storeId") String storeId,
            @UriVariable("id") String id) {
        NodeRef templateRef = new NodeRef(protocol, storeId, id);
        return WebScriptUtils.jsonResolution(caseTemplateService.getCaseTemplateJson(templateRef));
    }
}
