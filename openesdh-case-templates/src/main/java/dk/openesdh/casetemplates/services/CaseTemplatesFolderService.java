package dk.openesdh.casetemplates.services;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import dk.openesdh.repo.services.system.OpenESDHFoldersService;

public interface CaseTemplatesFolderService {

    String CASE_TEMPLATES_ROOT_FOLDER_PATH = OpenESDHFoldersService.CASES_ROOT_PATH + "ct:templates";

    NodeRef getCaseTemplatesRootFolder();

    NodeRef getCaseTypeTemplatesFolder(QName caseTypeQName);

    NodeRef getCaseTypeTemplatesFolder(String caseType);

}
