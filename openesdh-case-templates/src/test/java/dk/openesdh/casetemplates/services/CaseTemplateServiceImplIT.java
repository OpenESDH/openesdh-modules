package dk.openesdh.casetemplates.services;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;

import dk.openesdh.casetemplates.model.CaseTemplatesModule;
import dk.openesdh.repo.helper.CaseDocumentTestHelper;
import dk.openesdh.repo.helper.CaseHelper;
import dk.openesdh.repo.model.DocumentCategory;
import dk.openesdh.repo.model.DocumentType;
import dk.openesdh.repo.model.OpenESDHModel;
import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.DocumentCategoryService;
import dk.openesdh.repo.services.documents.DocumentService;
import dk.openesdh.repo.services.documents.DocumentTypeService;
import dk.openesdh.simplecase.model.SimpleCaseModel;

@RunWith(RemoteTestRunner.class)
@Remote(runnerClass = SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:alfresco/application-context.xml",
        "classpath:alfresco/extension/openesdh-test-context.xml" })
public class CaseTemplateServiceImplIT {

    private static final String TEST_SIMPLE_TEMPLATE_NAME = "Test Case Simple Template";
    private static final String TEST_SIMPLE_TEMPLATE_TITLE = "Test Case Simple Template Title";
    private static final String TEST_SIMPLE_TEMPLATE_DESCRIPTION = "Test Case Simple Template Description";

    private static final String TEMPLATE_DOC_NAME1 = "First case template doc";
    private static final String TEMPLATE_DOC_NAME2 = "Second case template doc";

    private static final String ACTIVITY_ADHOC = "activity$activitiAdhoc";
    private static final String ACTIVITY_REVIEW = "activity$activitiReview";

    private static final String TEST_CASE_NAME = "Test Case From Template";
    private static final String TEST_FOLDER_NAME = "CaseTemplateServiceImpIT";

    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;
    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;
    @Autowired
    @Qualifier("CaseTemplatesFolderService")
    private CaseTemplatesFolderService caseTemplatesFolderService;
    @Autowired
    @Qualifier("PersonService")
    private PersonService personService;
    @Autowired
    @Qualifier(CaseService.BEAN_ID)
    private CaseService caseService;
    @Autowired
    @Qualifier("CaseTemplateService")
    private CaseTemplateService caseTemplateService;
    @Autowired
    @Qualifier("DocumentTypeService")
    private DocumentTypeService documentTypeService;
    @Autowired
    @Qualifier("DocumentCategoryService")
    private DocumentCategoryService documentCategoryService;
    @Autowired
    @Qualifier("NamespaceService")
    private NamespaceService namespaceService;
    @Autowired
    @Qualifier("TestCaseHelper")
    private CaseHelper caseHelper;
    @Autowired
    @Qualifier("CaseDocumentTestHelper")
    private CaseDocumentTestHelper docTestHelper;
    @Autowired
    @Qualifier(DocumentService.BEAN_ID)
    private DocumentService documentService;
    @Autowired
    @Qualifier("AuthorityService")
    private AuthorityService authorityService;

    private NodeRef caseTemplateRef;
    private NodeRef caseRef;
    private NodeRef testFolder;
    private NodeRef adminNodeRef;

    @Before
    public void setUp() throws Exception {
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        tr.runInTransaction(() -> {
            testFolder = docTestHelper.createFolder(TEST_FOLDER_NAME);
            adminNodeRef = personService.getPerson(AuthenticationUtil.getAdminUserName());
            caseHelper.deleteDummyUser();
            caseHelper.createDummyUser();
            authorityService.addAuthority(CaseHelper.CASE_SIMPLE_CREATOR_GROUP, CaseHelper.DEFAULT_USERNAME);
            return null;
        });
    }

    @After
    public void tearDown() throws Exception {
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        tr.runInTransaction(() -> {
            if (caseRef != null && nodeService.exists(caseRef)) {
                nodeService.deleteNode(caseRef);
            }

            if (caseTemplateRef != null) {
                nodeService.deleteNode(caseTemplateRef);
            }

            caseHelper.deleteDummyUser();
            return null;
        });

    }

    @Test
    public void shouldCreateCaseTemplateAddAspectAndMoveToProperFolder() {
        tr.runInTransaction(() -> {
            createCaseTemplate();
            return null;
        });
        Assert.assertTrue("Case template aspect should be added",
                nodeService.hasAspect(caseTemplateRef, CaseTemplatesModule.ASPECT_CT_CASE_TEMPLATE));
        NodeRef templateFolder = nodeService.getPrimaryParent(caseTemplateRef).getParentRef();
        Assert.assertEquals("Wrong target path of the case template",
                "/app:company_home/oe:OpenESDH/oe:cases/ct:templates/ct:simple_case",
                nodeService.getPath(templateFolder).toPrefixString(namespaceService));

        List<String> workflows = (List<String>) nodeService.getProperty(caseTemplateRef,
                CaseTemplatesModule.PROP_WORKFLOWS);
        Assert.assertTrue("Case template should contain Adhoc workflow", workflows.contains(ACTIVITY_ADHOC));
        Assert.assertTrue("Case template should contain Review workflow", workflows.contains(ACTIVITY_REVIEW));
    }

    @Test
    public void shouldCreateCaseTemplateAvailableToCaseCreator() {
        tr.runInTransaction(() -> {
            createCaseTemplate();
            return null;
        });
        AuthenticationUtil.setFullyAuthenticatedUser(CaseHelper.DEFAULT_USERNAME);
        JSONArray array = caseTemplateService.getCaseTemplates(CaseHelper.SIMPLE_CASE_TYPE);
        Assert.assertEquals("Wrong number of retrieved case templates", 1, array.length());
    }

    @Test
    public void shouldCreateCaseTemplateWithDocsAndRetrieveJsonInfo() throws JSONException {
        tr.runInTransaction(() -> {
            createCaseTemplate();
            return null;
        });
        tr.runInTransaction(() -> {
            addTemplateDocs();
            return null;
        });
        JSONArray array = caseTemplateService.getCaseTemplates(CaseHelper.SIMPLE_CASE_TYPE);
        Assert.assertEquals("Wrong number of retrieved case templates", 1, array.length());
        JSONObject templateInfo = array.getJSONObject(0);

        Assert.assertTrue("Retrieved template info should contain template documents",
                templateInfo.has(CaseTemplateService.TEMPLATE_DOCS));
        JSONArray docsArray = templateInfo.getJSONArray(CaseTemplateService.TEMPLATE_DOCS);
        Assert.assertEquals("Wrong number of retrieved case template docs", 2, docsArray.length());

        JSONObject doc1 = docsArray.getJSONObject(0);
        Assert.assertEquals("Wrong name of retrieved case template document", TEMPLATE_DOC_NAME1,
                doc1.getString(CaseTemplateService.TEMPLATE_DOC_NAME));

        JSONObject doc2 = docsArray.getJSONObject(1);
        Assert.assertEquals("Wrong name of retrieved case template document", TEMPLATE_DOC_NAME2,
                doc2.getString(CaseTemplateService.TEMPLATE_DOC_NAME));
    }

    @Test
    public void shouldCreateCaseFromTemplateAndCopyTemplateDocs() {
        tr.runInTransaction(() -> {
            createCaseTemplate();
            return null;
        });
        
        tr.runInTransaction(() -> {
            addTemplateDocs();
            return null;
        });
        
        tr.runInTransaction(() -> {
            final Map<QName, Serializable> properties = new HashMap<>();
            properties.put(ContentModel.PROP_NAME, TEST_CASE_NAME);
            properties.put(ContentModel.PROP_TEMPLATE, caseTemplateRef);
            caseRef = caseHelper.createCase(testFolder, TEST_CASE_NAME, SimpleCaseModel.TYPE_CASE_SIMPLE,
                    properties, Arrays.asList(adminNodeRef), false);
            return null;
        });
        
        List<NodeRef> docs = documentService.getDocumentsForCase(caseRef)
            .stream()
            .map(ChildAssociationRef::getChildRef)
            .collect(Collectors.toList());

        Assert.assertEquals("Wrong number of case template docs copied to case", 2, docs.size());
        Assert.assertEquals("Wrong document copied from case template", TEMPLATE_DOC_NAME1,
                nodeService.getProperty(docs.get(0), ContentModel.PROP_NAME));
        Assert.assertEquals("Wrong document copied from case template", TEMPLATE_DOC_NAME2,
                nodeService.getProperty(docs.get(1), ContentModel.PROP_NAME));
    }

    private void createCaseTemplate() {
        NodeRef templatesRootRef = caseTemplatesFolderService.getCaseTemplatesRootFolder();
        Map<QName, Serializable> props = new HashMap<>();
        props.put(ContentModel.PROP_NAME, TEST_SIMPLE_TEMPLATE_NAME);
        props.put(ContentModel.PROP_TITLE, TEST_SIMPLE_TEMPLATE_TITLE);
        props.put(ContentModel.PROP_DESCRIPTION, TEST_SIMPLE_TEMPLATE_DESCRIPTION);
        props.put(CaseTemplatesModule.PROP_WORKFLOWS,
                (Serializable) Arrays.asList(ACTIVITY_ADHOC, ACTIVITY_REVIEW));
        caseTemplateRef = nodeService.createNode(templatesRootRef, ContentModel.ASSOC_CONTAINS,
                QName.createQName(TEST_SIMPLE_TEMPLATE_NAME), SimpleCaseModel.TYPE_CASE_SIMPLE, props).getChildRef();
        NodeRef adminRef = personService.getPerson(AuthenticationUtil.getAdminUserName());
        nodeService.createAssociation(caseTemplateRef, adminRef, OpenESDHModel.ASSOC_CASE_OWNERS);
    }

    private void addTemplateDocs() {
        NodeRef caseDocumentsFolder = caseService.getDocumentsFolder(caseTemplateRef);
        addTemplateDoc(TEMPLATE_DOC_NAME1, caseDocumentsFolder);
        addTemplateDoc(TEMPLATE_DOC_NAME2, caseDocumentsFolder);
    }

    private void addTemplateDoc(String documentName, NodeRef caseDocumentsFolder) {
        final Map<QName, Serializable> properties = new HashMap<>();
        properties.put(ContentModel.PROP_NAME, documentName);
        properties.put(OpenESDHModel.PROP_DOC_TYPE, getFirstDocumentType().getNodeRef().toString());
        properties.put(OpenESDHModel.PROP_DOC_CATEGORY, getFirstDocumentCategory().getNodeRef().toString());
        nodeService.createNode(caseDocumentsFolder, ContentModel.ASSOC_CONTAINS,
                QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, documentName), ContentModel.TYPE_CONTENT,
                properties).getChildRef();
    }

    private DocumentType getFirstDocumentType() {
        return documentTypeService.getDocumentTypes().stream().findFirst().get();
    }

    private DocumentCategory getFirstDocumentCategory() {
        return documentCategoryService.getDocumentCategories().stream().findFirst().get();
    }
}
