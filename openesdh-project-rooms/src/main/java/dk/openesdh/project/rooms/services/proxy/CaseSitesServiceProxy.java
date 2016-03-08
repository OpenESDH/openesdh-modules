package dk.openesdh.project.rooms.services.proxy;

import javax.annotation.PostConstruct;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.stereotype.Service;

import dk.openesdh.project.rooms.services.CaseSitesService;
import dk.openesdh.project.rooms.services.CaseSitesServiceImpl;

@SuppressWarnings("serial")
@Service(CaseSitesService.BEAN_ID)
public class CaseSitesServiceProxy extends ProxyFactoryBean {

    @PostConstruct
    public void init() {
        setTargetName(CaseSitesServiceImpl.BEAN_ID);
        setInterfaces(CaseSitesService.class);
    }
}
