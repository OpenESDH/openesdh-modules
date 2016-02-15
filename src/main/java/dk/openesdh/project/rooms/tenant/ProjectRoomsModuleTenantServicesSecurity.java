package dk.openesdh.project.rooms.tenant;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import dk.openesdh.project.rooms.model.ProjectRoomsModule;
import dk.openesdh.repo.services.tenant.TenantModulesSecurityAutoProxyCreator;

@SuppressWarnings("serial")
@Component
public class ProjectRoomsModuleTenantServicesSecurity extends TenantModulesSecurityAutoProxyCreator {

    @PostConstruct
    public void init() {
        setOpeneModuleId(ProjectRoomsModule.MODULE_ID);
        setBeanPackageNames("dk.openesdh.project.rooms.services*");
    }

}
