package dk.openesdh.docs.tenant;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import dk.openesdh.docs.model.OpeneDocsModule;
import dk.openesdh.repo.services.tenant.TenantModulesSecurityAutoProxyCreator;

@SuppressWarnings("serial")
@Component
public class OpeneDocsModuleTenantServicesSecurity extends TenantModulesSecurityAutoProxyCreator {
    @PostConstruct
    public void init() {
        setOpeneModuleId(OpeneDocsModule.MODULE_ID);
        setBeanPackageNames("dk.openesdh.docs.services");
    }
}
