package dk.openesdh.project.rooms.services;

import java.util.regex.Pattern;

import org.alfresco.repo.invitation.InvitationServiceImpl;
import org.alfresco.service.cmr.security.PersonService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * This is a workaround for Alfresco invitation service which bans sending invitations for all disabled accounts.
 * Sometimes we need to send several invitations to the same external party.
 * The party account stays disabled till the user accepts any invitation.
 * Therefore this aspect bypasses InvitationService account check, if the account username matches the external party pattern.
 * @author rudinjur
 *
 */
@Component
public class InvitationServiceAspect implements BeanFactoryAware {

    private static final Pattern EXTERNAL_USER_NAME = Pattern.compile(".+_.+(@.+)?$");

    @Autowired
    @Qualifier("PersonService")
    private PersonService personService;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!beanFactory.containsBean("invitationService")) {
            return;
        }
        NameMatchMethodPointcutAdvisor isEnabledAdvisor = new NameMatchMethodPointcutAdvisor(
                (MethodInterceptor) this::isInviteePersonEnabled);
        isEnabledAdvisor.addMethodName("isEnabled");
        ProxyFactory pf = new ProxyFactory();
        pf.setTarget(personService);
        pf.setInterfaces(PersonService.class);
        pf.addAdvisor(isEnabledAdvisor);
        InvitationServiceImpl invitationService = (InvitationServiceImpl) beanFactory.getBean("invitationService");
        invitationService.setPersonService((PersonService) pf.getProxy());
    }

    private Object isInviteePersonEnabled(MethodInvocation invocation) throws Throwable {
        String userName = (String) invocation.getArguments()[0];
        if (EXTERNAL_USER_NAME.matcher(userName).find()) {
            return true;
        }
        return invocation.proceed();
    }
}
