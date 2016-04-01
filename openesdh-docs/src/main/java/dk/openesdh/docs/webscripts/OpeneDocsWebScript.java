package dk.openesdh.docs.webscripts;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;

import dk.openesdh.docs.services.OpeneDocsService;
import dk.openesdh.repo.model.OpenESDHModel;
import dk.openesdh.repo.webscripts.WebScriptParams;
import dk.openesdh.repo.webscripts.utils.WebScriptUtils;

@Component
@WebScript(description = "Provides API for case document templates", families = "OpenE docs")
public class OpeneDocsWebScript {

    private static final String TEMPLATE_REF = "templateRef";

    @Autowired
    @Qualifier("OpeneDocsService")
    private OpeneDocsService openeDocsService;

    @Uri("/api/openesdh/docs/templates")
    public Resolution getTemplates(@RequestParam(value = "extensions", required = false) String[] extensions) {
        return WebScriptUtils.jsonResolution(openeDocsService.getDocumentTemplatesJson(extensions));
    }

    @Uri(value = "/api/openesdh/docs", method = HttpMethod.POST)
    public Resolution createDocument(
            @RequestParam(WebScriptParams.DESTINATION) NodeRef targetFolderRef,
            @RequestParam(TEMPLATE_REF) NodeRef templateRef,
            @RequestParam(WebScriptParams.DOC_TYPE) NodeRef docType,
            @RequestParam(WebScriptParams.DOC_CATEGORY) NodeRef docCategory,
            @RequestParam(WebScriptParams.TITLE) String title,
            @RequestParam(value=WebScriptParams.DESCRIPTION, required=false) String description) throws JSONException, IOException {
        
        Map<QName, Serializable> props = new HashMap<>();
        props.put(ContentModel.PROP_TITLE, title);
        if (StringUtils.isNotEmpty(description)) {
            props.put(ContentModel.PROP_DESCRIPTION, description);
        }
        props.put(OpenESDHModel.PROP_DOC_TYPE, docType);
        props.put(OpenESDHModel.PROP_DOC_CATEGORY, docCategory);
        NodeRef docRef = openeDocsService.createDocument(targetFolderRef, templateRef, props);
        Map<String, Object> result = new HashMap<>();
        result.put(WebScriptParams.NODE_REF, docRef);
        return WebScriptUtils.jsonResolution(result);
    }}
