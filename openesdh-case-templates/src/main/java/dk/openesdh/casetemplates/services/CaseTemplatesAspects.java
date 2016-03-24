package dk.openesdh.casetemplates.services;

import javax.annotation.PostConstruct;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dk.openesdh.repo.policy.CaseBehaviour;
import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.services.activities.CaseDocumentActivityBehaviour;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.DocumentService;
import dk.openesdh.repo.utils.ClassUtils;

@Component
public class CaseTemplatesAspects {

    @Autowired
    @Qualifier("CaseTemplateService")
    private CaseTemplateService caseTemplateService;

    @Autowired
    @Qualifier("CaseTemplatesFolderService")
    private CaseTemplatesFolderService caseTemplatesFolderService;

    @Autowired
    @Qualifier("CaseService")
    private CaseService caseService;

    @Autowired
    @Qualifier("DocumentService")
    private DocumentService documentService;

    @Autowired
    @Qualifier(CaseBehaviour.BEAN_ID)
    private CaseBehaviour caseBehaviour;

    @Autowired
    @Qualifier(CaseDocumentActivityBehaviour.BEAN_ID)
    private CaseDocumentActivityBehaviour caseDocumentActivityBehaviour;

    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;

    @PostConstruct
    public void init() {
        createAspectForCaseBehaviour();
        createAspectForCaseDocumentActivityBehaviour();
    }

    private void createAspectForCaseBehaviour() {
        ClassUtils.checkHasMethods(CaseService.class, "createCase");
        NameMatchMethodPointcutAdvisor createCaseAdvisor = new NameMatchMethodPointcutAdvisor(
                (MethodInterceptor) this::createCaseInterceptor);
        createCaseAdvisor.addMethodName("createCase");
        ProxyFactory pf = new ProxyFactory();
        pf.setTarget(caseService);
        pf.setInterfaces(CaseService.class);
        pf.addAdvisor(createCaseAdvisor);
        caseBehaviour.setCaseService((CaseService) pf.getProxy());
    }

    private void createAspectForCaseDocumentActivityBehaviour() {
        ClassUtils.checkHasMethods(DocumentService.class, "isDocBelongsToCase");
        NameMatchMethodPointcutAdvisor isDocBelongsToCaseAdvisor = new NameMatchMethodPointcutAdvisor(
                (MethodInterceptor) this::isDocBelongsToCaseInterceptor);
        isDocBelongsToCaseAdvisor.addMethodName("isDocBelongsToCase");
        ProxyFactory pf = new ProxyFactory();
        pf.setTarget(documentService);
        pf.setInterfaces(DocumentService.class);
        pf.addAdvisor(isDocBelongsToCaseAdvisor);
        caseDocumentActivityBehaviour.setDocumentService((DocumentService) pf.getProxy());
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
            caseService.createCase(childAssocRef);
            caseTemplateService.copyCaseTemplateDocsToCase(childAssocRef.getChildRef());
        }
        return null;
    }

    /**
     * Checks whether provided doc belongs to case template before check if
     * belongs to case.
     * 
     */
    private Object isDocBelongsToCaseInterceptor(MethodInvocation invocation) throws Throwable {
        NodeRef docRef = (NodeRef) invocation.getArguments()[0];
        return !caseTemplateService.isDocBelongsToCaseTemplate(docRef)
                && documentService.isDocBelongsToCase(docRef);
    }
}
