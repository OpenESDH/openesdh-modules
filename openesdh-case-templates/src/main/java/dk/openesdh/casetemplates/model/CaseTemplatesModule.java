package dk.openesdh.casetemplates.model;

import org.alfresco.service.namespace.QName;

public interface CaseTemplatesModule {

    String MODULE_ID = "openesdh-case-templates";

    String CT_URI = "http://openesdh.dk/model/case/templates/1.0";

    QName ASPECT_CT_CASE_TEMPLATE = QName.createQName(CT_URI, "CaseTemplate");

    QName PROP_WORKFLOWS = QName.createQName(CT_URI, "workflows");
}
