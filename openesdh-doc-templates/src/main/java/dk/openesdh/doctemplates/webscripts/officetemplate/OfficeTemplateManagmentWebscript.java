package dk.openesdh.doctemplates.webscripts.officetemplate;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.servlet.FormData.FormField;
import org.springframework.stereotype.Component;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.FileField;
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;

import dk.openesdh.doctemplates.api.services.OfficeTemplateService;
import dk.openesdh.repo.webscripts.WebScriptParams;
import dk.openesdh.repo.webscripts.utils.WebScriptUtils;

@Component
@WebScript(families = {"OpenESDH Office Template"}, description = "The CRUD webscripts for document templates")
@Authentication(AuthenticationType.ADMIN)
public class OfficeTemplateManagmentWebscript {

    @Autowired
    @Qualifier("OfficeTemplateService")
    private OfficeTemplateService officeTemplateService;

    @Uri(value = "/api/openesdh/officeDocTemplate", method = HttpMethod.POST, defaultFormat = "json", multipartProcessing = true)
    public Resolution post(
            @RequestParam(value = WebScriptParams.TITLE, required = true) String title,
            @RequestParam(value = WebScriptParams.DESCRIPTION, required = false) String description,
            @RequestParam(value = WebScriptParams.DOC_TYPE, required = true) NodeRef docType,
            @RequestParam(value = WebScriptParams.DOC_CATEGORY, required = true) NodeRef docCategory,
            @FileField("filedata") FormField fileField) {

        officeTemplateService.saveTemplate(
                title,
                description,
                docType,
                docCategory,
                fileField.getFilename(),
                fileField.getInputStream(),
                fileField.getMimetype()
        );
        return WebScriptUtils.respondSuccess("The the template was successfully uploaded.");
    }

    @Uri(value = "/api/openesdh/officeDocTemplate/{store_type}/{store_id}/{id}", method = HttpMethod.DELETE, defaultFormat = "json")
    public Resolution delete(
            @UriVariable("store_type") String storeType,
            @UriVariable("store_id") String storeId,
            @UriVariable("id") String id) {

        officeTemplateService.deleteTemplate(new NodeRef(storeType, storeId, id));
        return WebScriptUtils.respondSuccess("The template was successfully deleted.");
    }
}
