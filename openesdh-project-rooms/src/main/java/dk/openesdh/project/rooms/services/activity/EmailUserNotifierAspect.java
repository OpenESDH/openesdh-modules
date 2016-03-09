package dk.openesdh.project.rooms.services.activity;

import org.alfresco.repo.activities.feed.EmailUserNotifier;
import org.alfresco.repo.management.subsystems.ApplicationContextFactory;
import org.alfresco.service.cmr.site.SiteService;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * This is a fix for Alfresco EmailUserNotifier which throws
 * NullPointerException trying to send a notification about site activity, when
 * the site has been removed.
 * 
 * @author rudinjur
 *
 */
@Component
public class EmailUserNotifierAspect implements BeanFactoryAware {

    @Autowired
    @Qualifier("ActivitiesFeed")
    public ApplicationContextFactory activitiesFeedContextFactory;

    @Autowired
    @Qualifier("SiteService")
    private SiteService siteService;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

        ApplicationContext activitiesFeedContext = activitiesFeedContextFactory.getApplicationContext();

        NameMatchMethodPointcutAdvisor getSiteAdvisor = new NameMatchMethodPointcutAdvisor(
                (MethodInterceptor) this::getSiteInterceptor);
        getSiteAdvisor.addMethodName("getSite");
        ProxyFactory pf = new ProxyFactory();
        pf.setTarget(siteService);
        pf.setInterfaces(SiteService.class);
        pf.addAdvisor(getSiteAdvisor);
        EmailUserNotifier emailUserNotifier = (EmailUserNotifier) activitiesFeedContext
                .getBean("emailUserNotifier");
        emailUserNotifier.setSiteService((SiteService) pf.getProxy());
    }

    private Object getSiteInterceptor(MethodInvocation invocation) throws Throwable {
        String siteId = (String) invocation.getArguments()[0];
        if (!siteService.hasSite(siteId)) {
            return null;
        }
        return invocation.proceed();
    }

}
