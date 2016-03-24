package dk.openesdh.project.rooms.services.activity;

import java.lang.reflect.Method;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.project.rooms.services.CaseSitesService;

@Component
public class CaseSiteActivityAspect implements BeanFactoryAware {

    @Autowired
    @Qualifier(CaseSiteActivityService.BEAN_ID)
    private CaseSiteActivityService caseSiteActivityService;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!beanFactory.containsBean(CaseSitesService.BEAN_ID)) {
            return;
        }

        Advised caseSiteServiceProxy = (Advised) beanFactory.getBean(CaseSitesService.BEAN_ID);

        NameMatchMethodPointcutAdvisor createCaseSiteAdvisor = new NameMatchMethodPointcutAdvisor(
                (AfterReturningAdvice) this::afterCaseSiteCreated);
        createCaseSiteAdvisor.addMethodName("createCaseSite");
        caseSiteServiceProxy.addAdvisor(createCaseSiteAdvisor);

        NameMatchMethodPointcutAdvisor closeCaseSiteAdvisor = new NameMatchMethodPointcutAdvisor(
                (MethodInterceptor) this::onCloseCaseSite);
        closeCaseSiteAdvisor.addMethodName("closeCaseSite");
        caseSiteServiceProxy.addAdvisor(closeCaseSiteAdvisor);

        NameMatchMethodPointcutAdvisor inviteCaseSiteParticipantsAdvisor = new NameMatchMethodPointcutAdvisor(
                (AfterReturningAdvice) this::afterCaseSiteParticipantsInvited);
        inviteCaseSiteParticipantsAdvisor.addMethodName("inviteParticipants");
        caseSiteServiceProxy.addAdvisor(inviteCaseSiteParticipantsAdvisor);
    }

    public void afterCaseSiteCreated(Object result, Method method, Object[] args, Object target) {
        CaseSite site = (CaseSite) args[0];
        caseSiteActivityService.postOnCaseSiteCreate(site);
    }

    public Object onCloseCaseSite(MethodInvocation invocation) throws Throwable {
        CaseSite site = (CaseSite) invocation.getArguments()[0];
        Set<String> siteMembers = caseSiteActivityService.getSiteMembersToNotify(site.getShortName());
        Object result = invocation.proceed();
        caseSiteActivityService.postOnCaseSiteClose((CaseSite) result, siteMembers);
        return result;
    }

    public void afterCaseSiteParticipantsInvited(Object result, Method method, Object[] args, Object target) {
        caseSiteActivityService.postOnCaseSiteParticipantsInvited((CaseSite) result);
    }
}
