package dk.openesdh.casetemplates.services;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.DocumentService;

@Component
public class CaseTemplatesAspects implements BeanFactoryAware {

    @Autowired
    @Qualifier("CaseTemplateService")
    private CaseTemplateService caseTemplateService;

    @Autowired
    @Qualifier("CaseTemplatesFolderService")
    private CaseTemplatesFolderService caseTemplatesFolderService;

    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!beanFactory.containsBean(CaseService.BEAN_ID)) {
            return;
        }
        createAspectForCreateCase(beanFactory);
        createAspectForIsDocBelongsToCase(beanFactory);
    }

    private void createAspectForCreateCase(BeanFactory beanFactory) {
        Advised caseService = (Advised) beanFactory.getBean(CaseService.BEAN_ID);
        NameMatchMethodPointcutAdvisor createCaseAdvisor = new NameMatchMethodPointcutAdvisor(
                (MethodInterceptor) this::createCaseInterceptor);
        createCaseAdvisor.addMethodName("createCase");
        caseService.addAdvisor(createCaseAdvisor);
    }

    private void createAspectForIsDocBelongsToCase(BeanFactory beanFactory) {
        Advised documentService = (Advised) beanFactory.getBean(DocumentService.BEAN_ID);
        NameMatchMethodPointcutAdvisor isDocBelongsToCaseAdvisor = new NameMatchMethodPointcutAdvisor(
                (MethodInterceptor) this::isDocBelongsToCaseInterceptor);
        isDocBelongsToCaseAdvisor.addMethodName("isDocBelongsToCase");
        documentService.addAdvisor(isDocBelongsToCaseAdvisor);
    }

    /**
     * Creates case template object instead of case object if the target folder
     * is case templates root.
     * If a case object is being created then copies documents from case template. 
     */
    private Object createCaseInterceptor(MethodInvocation invocation) throws Throwable {
        ChildAssociationRef childAssocRef = (ChildAssociationRef) invocation.getArguments()[0];
        NodeRef casesTemplatesFolder = caseTemplatesFolderService.getCaseTemplatesRootFolder();
        if (casesTemplatesFolder.equals(childAssocRef.getParentRef())) {
            caseTemplateService.onCreateCaseTemplate(childAssocRef.getChildRef());
        } else {
            invocation.proceed();
            tr.runAsSystem(() -> {
                caseTemplateService.copyCaseTemplateDocsToCase(childAssocRef.getChildRef());
                return null;
            });
        }
        return null;
    }

    /**
     * Checks whether provided doc belongs to case template before check if
     * belongs to case.
     * This is due to case activity behaviour, which sends notifications when case document is uploaded.
     * The behaviour is omitted by checking whether it's a template document.
     * 
     */
    private Object isDocBelongsToCaseInterceptor(MethodInvocation invocation) throws Throwable {
        NodeRef docRef = (NodeRef) invocation.getArguments()[0];
        if (caseTemplateService.isDocBelongsToCaseTemplate(docRef)) {
            return false;
        }
        return invocation.proceed();
    }
}
