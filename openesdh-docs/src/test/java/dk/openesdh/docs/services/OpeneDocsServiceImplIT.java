package dk.openesdh.docs.services;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
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

import dk.openesdh.repo.helper.CaseDocumentTestHelper;
import dk.openesdh.repo.helper.CaseHelper;
import dk.openesdh.repo.model.CaseDocument;
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
public class OpeneDocsServiceImplIT {

    private static final String TEST_FOLDER_NAME = "OpeneDocsServiceImplIT";
    private static final String TEST_CASE_NAME = "Test Case For MS Office docs";
    private static final String TEST_DOC_NAME = "Test document from template_";
    private static final String DOCX = "docx";
    private static final String XLSX = "xlsx";
    private static final String PPTX = "pptx";

    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;
    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;
    @Autowired
    @Qualifier("TestCaseHelper")
    private CaseHelper caseHelper;
    @Autowired
    @Qualifier("CaseDocumentTestHelper")
    private CaseDocumentTestHelper docTestHelper;
    @Autowired
    @Qualifier("PersonService")
    private PersonService personService;
    @Autowired
    @Qualifier(CaseService.BEAN_ID)
    private CaseService caseService;
    @Autowired
    @Qualifier(DocumentService.BEAN_ID)
    private DocumentService documentService;
    @Autowired
    @Qualifier("DocumentTypeService")
    private DocumentTypeService documentTypeService;
    @Autowired
    @Qualifier("DocumentCategoryService")
    private DocumentCategoryService documentCategoryService;
    @Autowired
    @Qualifier("OpeneDocsService")
    private OpeneDocsService openeDocsService;

    private NodeRef caseRef;
    private NodeRef caseDocsFolderRef;
    private NodeRef testFolder;
    private NodeRef adminNodeRef;

    @Before
    public void setUp() throws Exception {
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        tr.runInTransaction(() -> {
            testFolder = docTestHelper.createFolder(TEST_FOLDER_NAME);
            adminNodeRef = personService.getPerson(AuthenticationUtil.getAdminUserName());
            final Map<QName, Serializable> properties = new HashMap<>();
            properties.put(ContentModel.PROP_NAME, TEST_CASE_NAME);
            caseRef = caseHelper.createCase(testFolder, TEST_CASE_NAME, SimpleCaseModel.TYPE_CASE_SIMPLE,
                    properties, Arrays.asList(adminNodeRef), false);
            return null;
        });
        caseDocsFolderRef = caseService.getDocumentsFolder(caseRef);
    }

    @After
    public void tearDown() throws Exception {
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        tr.runInTransaction(() -> {
            if (caseRef != null && nodeService.exists(caseRef)) {
                nodeService.deleteNode(caseRef);
            }
            return null;
        });
    }

    @Test
    public void shouldCreateCaseWordDocFromTemplate() {
        testCreateDocument(DOCX);
    }

    @Test
    public void shouldCreateCaseExcelDocFromTemplate() {
        testCreateDocument(XLSX);
    }

    @Test
    public void shouldCreateCasePptDocFromTemplate() {
        testCreateDocument(PPTX);
    }

    private void testCreateDocument(String tplExtension) {
        NodeRef docxTplRef = openeDocsService.getDocumentTemplates(tplExtension).get(0);
        String docName = TEST_DOC_NAME + tplExtension;
        tr.runInNewTransaction(() -> {
            final Map<QName, Serializable> properties = new HashMap<>();
            properties.put(ContentModel.PROP_TITLE, docName);
            properties.put(OpenESDHModel.PROP_DOC_TYPE, getFirstDocumentType().getNodeRef().toString());
            properties.put(OpenESDHModel.PROP_DOC_CATEGORY, getFirstDocumentCategory().getNodeRef().toString());
            openeDocsService.createDocument(caseDocsFolderRef, docxTplRef, properties);
            return null;
        });
        NodeRef docRef = documentService.getDocumentsForCase(caseRef).stream().map(ChildAssociationRef::getChildRef)
                .findAny().get();
        CaseDocument document = documentService.getCaseDocument(docRef);
        Assert.assertEquals("Wrong name of the case document", docName, document.getTitle());
    }

    private DocumentType getFirstDocumentType() {
        return documentTypeService.getDocumentTypes().stream().findFirst().get();
    }

    private DocumentCategory getFirstDocumentCategory() {
        return documentCategoryService.getDocumentCategories().stream().findFirst().get();
    }
}
