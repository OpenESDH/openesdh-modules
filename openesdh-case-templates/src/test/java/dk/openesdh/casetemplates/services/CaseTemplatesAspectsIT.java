package dk.openesdh.casetemplates.services;

import org.junit.Test;

import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.DocumentService;
import dk.openesdh.repo.utils.ClassUtils;

public class CaseTemplatesAspectsIT {
    @Test
    public void checkMandatoryMethods() {
        ClassUtils.checkHasMethods(CaseService.class, "createCase");
        ClassUtils.checkHasMethods(DocumentService.class, "isDocBelongsToCase");
    }

}
