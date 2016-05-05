package dk.openesdh.doctemplates.webscripts.officetemplate;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;

import dk.openesdh.doctemplates.api.model.OfficeTemplateMerged;
import dk.openesdh.doctemplates.api.services.OfficeTemplateService;
import dk.openesdh.repo.webscripts.utils.WebScriptUtils;

@Component
@WebScript(description = "Fill the specified office template, and save it to case folder", families = {
        "OpenESDH Office Template" })
public class OfficeTemplateFillToCaseFolderWebScript {

    @Autowired
    @Qualifier("OfficeTemplateService")
    private OfficeTemplateService officeTemplateService;

    @Uri(value = "/api/openesdh/template/{store_type}/{store_id}/{node_id}/case/{caseId}/folder/{folderStoreType}/{folderStoreId}/{folderNodeId}", method = HttpMethod.POST, defaultFormat = "json")
    public void fillToCaseFolder(
            @UriVariable final String store_type,
            @UriVariable final String store_id,
            @UriVariable final String node_id,
            @UriVariable final String caseId,
            @UriVariable final String folderStoreType,
            @UriVariable final String folderStoreId,
            @UriVariable final String folderNodeId,
            WebScriptRequest req, WebScriptResponse res
    ) throws Exception {
        List<OfficeTemplateMerged> merged = officeTemplateService.getMergedTemplates(
                new NodeRef(store_type, store_id, node_id),
                caseId,
                WebScriptUtils.readJson(req));
        officeTemplateService.saveToFolder(new NodeRef(folderStoreType, folderStoreId, folderNodeId), merged);
    }
}
