package dk.openesdh.casetemplates.tenant;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import dk.openesdh.casetemplates.model.CaseTemplatesModule;
import dk.openesdh.repo.services.tenant.TenantModulesSecurityAutoProxyCreator;

@SuppressWarnings("serial")
@Component
public class CaseTemplatesModuleTenantServicesSecurity extends TenantModulesSecurityAutoProxyCreator {
    @PostConstruct
    public void init() {
        setOpeneModuleId(CaseTemplatesModule.MODULE_ID);
        setBeanPackageNames("dk.openesdh.casetemplates.services");
    }
}
