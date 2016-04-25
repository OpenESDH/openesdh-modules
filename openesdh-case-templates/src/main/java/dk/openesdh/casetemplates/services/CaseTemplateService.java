package dk.openesdh.casetemplates.services;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONObject;

public interface CaseTemplateService {

    String TEMPLATE_DOCS = "templateDocs";
    String TEMPLATE_DOC_NAME = "name";
    String TEMPLATE_DOC_TITLE = "title";

    JSONArray getCaseTemplates(String caseType);

    void onCreateCaseTemplate(NodeRef caseTemplateRef);

    void copyCaseTemplateDocsToCase(NodeRef caseRef);

    boolean isDocBelongsToCaseTemplate(NodeRef docRef);

    List<String> getCaseTemplateWorkflows(NodeRef templateRef);

    JSONObject getCaseTemplateJson(NodeRef templateRef);

}
