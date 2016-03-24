package dk.openesdh.project.rooms.services.activity;

import org.junit.Test;

import dk.openesdh.project.rooms.services.CaseSitesService;
import dk.openesdh.repo.utils.ClassUtils;

public class CaseSiteActivityAspectIT {

    @Test
    public void checkMandatoryMethods() {
        ClassUtils.checkHasMethods(CaseSitesService.class, "createCaseSite", "closeCaseSite",
                "inviteParticipants");
    }
}
