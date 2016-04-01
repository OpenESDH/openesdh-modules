package dk.openesdh.docs.services;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.repo.services.system.OpenESDHFoldersService;

@Service("OpeneDocsFolderService")
public class OpeneDocsFolderServiceImpl implements OpeneDocsFolderService {

    @Autowired
    @Qualifier("OpenESDHFoldersService")
    private OpenESDHFoldersService oeFoldersService;

    @Override
    public NodeRef getDocsTemplatesFolder() {
        return oeFoldersService.getFolder(oeFoldersService.getSubsystemRootNodeRef(), "docTemplates");
    }

}
