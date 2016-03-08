package dk.openesdh.project.rooms.services;

import static dk.openesdh.repo.model.CaseDocumentJson.MAIN_DOC_NODE_REF;
import static dk.openesdh.repo.model.CaseDocumentJson.NODE_REF;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
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

import dk.openesdh.project.rooms.model.CaseSite;
import dk.openesdh.repo.helper.CaseDocumentTestHelper;
import dk.openesdh.repo.helper.CaseHelper;
import dk.openesdh.repo.model.CaseDocument;
import dk.openesdh.repo.model.CaseDocumentAttachment;
import dk.openesdh.repo.model.OpenESDHModel;
import dk.openesdh.repo.model.ResultSet;
import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.DocumentCategoryServiceImpl;
import dk.openesdh.repo.services.documents.DocumentService;
import dk.openesdh.repo.services.documents.DocumentTypeServiceImpl;
import dk.openesdh.repo.services.lock.OELockService;

@RunWith(RemoteTestRunner.class)
@Remote(runnerClass = SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:alfresco/application-context.xml",
        "classpath:alfresco/extension/openesdh-test-context.xml" })
public class CaseSitesServiceImplIT {

    @Autowired
    @Qualifier("CaseDocumentTestHelper")
    private CaseDocumentTestHelper docTestHelper;

    @Autowired
    @Qualifier("SiteService")
    private SiteService siteService;

    @Autowired
    @Qualifier(CaseSitesService.BEAN_ID)
    private CaseSitesService caseSiteService;

    @Autowired
    @Qualifier("CaseService")
    private CaseService caseService;

    @Autowired
    @Qualifier("AuthorityService")
    private AuthorityService authorityService;

    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;

    @Autowired
    @Qualifier("CaseSiteDocumentsService")
    private CaseSiteDocumentsService caseSiteDocumentsService;

    @Autowired
    @Qualifier("OELockService")
    private OELockService oeLockService;

    @Autowired
    @Qualifier("DocumentService")
    private DocumentService documentService;

    @Autowired
    @Qualifier("CheckOutCheckInService")
    private CheckOutCheckInService checkOutCheckInService;

    @Autowired
    @Qualifier("ContentService")
    private ContentService contentService;

    @Autowired
    @Qualifier("VersionService")
    private VersionService versionService;

    @Autowired
    @Qualifier("DocumentTypeService")
    private DocumentTypeServiceImpl documentTypeService;

    @Autowired
    @Qualifier("DocumentCategoryService")
    private DocumentCategoryServiceImpl documentCategoryService;

    @Autowired
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;

    private static final String TEST_FOLDER_NAME = "DocumentServiceImpIT";
    private static final String TEST_CASE_NAME1 = "TestCase1";
    private static final String TEST_DOCUMENT_NAME = "TestDocument";
    private static final String TEST_DOCUMENT_FILE_NAME = TEST_DOCUMENT_NAME + ".txt";
    private static final String TEST_DOCUMENT_ATTACHMENT_NAME = "TestDocumentAttachment";
    private static final String TEST_DOCUMENT_ATTACHMENT_FILE_NAME = TEST_DOCUMENT_ATTACHMENT_NAME + ".txt";
    private static final String NEW_SITE_DOC_NAME = "New site document";

    private NodeRef testFolder;
    private NodeRef testCase1;
    private NodeRef testDocument;
    private String testCaseId;
    private NodeRef testDocumentRecFolder;
    private NodeRef testDocumentAttachment;

    @Before
    public void setUp() throws Exception {
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        testFolder = docTestHelper.createFolder(TEST_FOLDER_NAME);
        testCase1 = docTestHelper.createCaseBehaviourOn(TEST_CASE_NAME1, testFolder, CaseHelper.DEFAULT_USERNAME);
        testCaseId = caseService.getCaseId(testCase1);
        testDocument = docTestHelper.createCaseDocument(TEST_DOCUMENT_FILE_NAME, testCase1);
        testDocumentRecFolder = nodeService.getPrimaryParent(testDocument).getParentRef();
        testDocumentAttachment = docTestHelper.createCaseDocumentAttachment(TEST_DOCUMENT_ATTACHMENT_FILE_NAME,
                testDocumentRecFolder);
    }

    @After
    public void tearDown() throws Exception {
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        List<NodeRef> folders = Arrays.asList(testFolder);
        List<NodeRef> cases = Arrays.asList(testCase1);
        List<String> users = Arrays.asList(CaseHelper.DEFAULT_USERNAME);
        try {
            docTestHelper.removeNodesAndDeleteUsersInTransaction(folders, cases, users);
        } catch (Exception ignored) {
        }

        if (siteService.hasSite(TEST_CASE_NAME1)) {
            siteService.deleteSite(TEST_CASE_NAME1);
        }
    }

    @Test
    public void shouldCreateEmptySiteForCase() {
        caseSiteService.createCaseSite(site());
        Assert.assertTrue("Should create empty site for case", siteService.hasSite(TEST_CASE_NAME1));
    }
    
    @Test
    public void shouldCreateSiteCopyCaseDocumentNoAttachmentsAndLock() throws JSONException {

        CaseSite site = siteWithDocument();
        CaseDocument document = site.getSiteDocuments().get(0);
        caseSiteService.createCaseSite(site);

        Assert.assertTrue("Should create site for case", siteService.hasSite(TEST_CASE_NAME1));

        JSONArray siteDocuments = caseSiteDocumentsService.getCaseSiteDocumentsJson(TEST_CASE_NAME1);
        Assert.assertEquals("Created site should contain a document", 1, siteDocuments.length());
        JSONObject siteDocument = (JSONObject) siteDocuments.get(0);
        Assert.assertFalse("Site document SHOULD contain copy of case document",
                document.getNodeRef().equals(siteDocument.get(NODE_REF)));
        String siteMainDocument = siteDocument.getString(MAIN_DOC_NODE_REF);
        Assert.assertTrue("Site document DOES NOT contain main document", StringUtils.isNotEmpty(siteMainDocument));

        Assert.assertTrue("Case document SHOULD be locked after copy to site",
                oeLockService.isLocked(testDocument));

        ResultSet<CaseDocumentAttachment> attachments = documentService
                .getDocumentVersionAttachments(new NodeRef(siteMainDocument), 0, 1000);
        Assert.assertEquals("Site document SHOULD NOT contain attachments.", 0, attachments.getTotalItems());
    }

    @Test
    public void shouldCreateSiteCopyLockDocumentWithAttachmentRetainVersionLabels() throws JSONException {

        createNewVersionOfCaseDocAndAttachment();

        caseSiteService.createCaseSite(siteWithDocumentAndAttachment());

        JSONArray siteDocuments = caseSiteDocumentsService.getCaseSiteDocumentsJson(TEST_CASE_NAME1);
        JSONObject siteDocument = (JSONObject) siteDocuments.get(0);
        String siteMainDocument = siteDocument.getString(MAIN_DOC_NODE_REF);

        Assert.assertTrue("Case document should be locked after copy to site",
                oeLockService.isLocked(testDocument));

        NodeRef siteMainDocRef = new NodeRef(siteMainDocument);
        Version siteMainDocVersion = versionService.getCurrentVersion(siteMainDocRef);
        Assert.assertEquals("Wrong site document version label retained", "2.0",
                siteMainDocVersion.getVersionLabel());

        ResultSet<CaseDocumentAttachment> attachments = documentService
                .getDocumentVersionAttachments(siteMainDocRef, 0, 1000);
        Assert.assertEquals("Site document SHOULD contain attachments.", 1, attachments.getTotalItems());
        String siteDocAttachmentRef = attachments.getResultList().get(0).getNodeRef();
        Version siteDocAttachmentVersion = versionService.getCurrentVersion(new NodeRef(siteDocAttachmentRef));
        Assert.assertEquals("Wrong document attachment version label retained", "1.1",
                siteDocAttachmentVersion.getVersionLabel());
    }

    @Test
    public void shouldCloseSiteCopySiteDocumentsBackToCase() {
        caseSiteService.createCaseSite(siteWithDocumentAndAttachment());
        final String SITE_DOC_CONTENT = "Site doc content";
        final String SITE_DOC_ATTACHMENT_CONTENT = "Site doc attachment content";
        List<CaseDocument> siteDocs = caseSiteDocumentsService
                .getCaseSiteDocumentsWithAttachments(TEST_CASE_NAME1);
        CaseDocument siteDoc = siteDocs.get(0);
        createNewDocVersion(new NodeRef(siteDoc.getMainDocNodeRef()), SITE_DOC_CONTENT, VersionType.MAJOR);
        createNewDocVersion(siteDoc.getAttachments().get(0).nodeRefObject(), SITE_DOC_ATTACHMENT_CONTENT,
                VersionType.MINOR);

        CaseSite site = caseSiteService.getCaseSite(TEST_CASE_NAME1);
        site.setSiteDocuments(siteDocs);

        caseSiteService.closeCaseSite(site);

        Assert.assertFalse("The site should be deleted when project room is closed",
                siteService.hasSite(TEST_CASE_NAME1));

        Version caseDocVersion = versionService.getCurrentVersion(testDocument);
        Assert.assertEquals("Wrong case document version after copy back to case", "2.0",
                caseDocVersion.getVersionLabel());
        String docResultContent = contentService.getReader(testDocument, ContentModel.PROP_CONTENT)
                .getContentString();
        Assert.assertEquals("Wrong case doc content after copy back to case", SITE_DOC_CONTENT, docResultContent);

        Version caseDocAttachmentVersion = versionService.getCurrentVersion(testDocumentAttachment);
        Assert.assertEquals("Wrong case document attachment version after copy back to case", "1.1",
                caseDocAttachmentVersion.getVersionLabel());
        String attachmentResultContent = contentService.getReader(testDocumentAttachment, ContentModel.PROP_CONTENT)
                .getContentString();
        Assert.assertEquals("Wrong case doc content after copy back to case", SITE_DOC_ATTACHMENT_CONTENT,
                attachmentResultContent);
    }

    @Test
    public void shouldCloseSiteAndCopyNewSiteDocumentToCase() {
        caseSiteService.createCaseSite(site());
        tr.runInTransaction(() -> {
            createNewSiteDocument();
            return null;
        });
        CaseSite site = caseSiteService.getCaseSite(TEST_CASE_NAME1);
        List<CaseDocument> siteDocs = caseSiteDocumentsService.getCaseSiteDocumentsWithAttachments(TEST_CASE_NAME1);
        site.setSiteDocuments(siteDocs);

        List<CaseDocument> caseDocsBeforeSiteClosed = documentService.getCaseDocumentsWithAttachments(testCaseId);
        
        Optional<CaseDocument> newSiteDocInCase = caseDocsBeforeSiteClosed.stream()
                .filter(doc -> NEW_SITE_DOC_NAME.equals(doc.getTitle()))
                .findAny();
        
        Assert.assertFalse("The case shouldn't contain site document before site is closed", newSiteDocInCase.isPresent());
        
        caseSiteService.closeCaseSite(site);

        List<CaseDocument> caseDocsAfterSiteClosed = documentService.getCaseDocumentsWithAttachments(testCaseId);
        newSiteDocInCase = caseDocsAfterSiteClosed.stream()
                .filter(doc -> NEW_SITE_DOC_NAME.equals(doc.getTitle()))
                .findAny();
        Assert.assertTrue("The case should contain newly created site document after site is closed",
                newSiteDocInCase.isPresent());
    }

    private void createNewSiteDocument() {
        CaseSite site = caseSiteService.getCaseSite(TEST_CASE_NAME1);
        NodeRef siteDocsFolder = new NodeRef(site.getDocumentsFolderRef());
        Map<QName, Serializable> props = new HashMap<>();
        props.put(ContentModel.PROP_NAME, NEW_SITE_DOC_NAME);
        props.put(OpenESDHModel.PROP_DOC_TYPE,
                documentTypeService.getDocumentTypes().stream().skip(1).findFirst().get().getNodeRef());
        props.put(OpenESDHModel.PROP_DOC_CATEGORY,
                documentCategoryService.getDocumentCategories().stream().skip(1).findFirst().get().getNodeRef());
        NodeRef siteDocRef = nodeService.createNode(siteDocsFolder, ContentModel.ASSOC_CONTAINS,
                QName.createQName("newSiteDoc"), ContentModel.TYPE_CONTENT, props).getChildRef();
        ContentWriter writer = contentService.getWriter(siteDocRef, ContentModel.PROP_CONTENT, true);
        writer.setMimetype("text");
        writer.putContent("site document content");
    }

    private CaseSite site() {
        CaseSite site = new CaseSite();
        site.setShortName(TEST_CASE_NAME1);
        site.setTitle(TEST_CASE_NAME1);
        site.setCaseId(testCaseId);
        return site;
    }

    private CaseSite siteWithDocument() {
        CaseSite site = site();
        site.getSiteDocuments().add(document());
        return site;
    }

    private CaseSite siteWithDocumentAndAttachment() {
        CaseSite site = site();
        site.getSiteDocuments().add(documentWithAttachment());
        return site;
    }

    private CaseDocument document() {
        CaseDocument document = new CaseDocument();
        document.setNodeRef(testDocumentRecFolder.toString());
        document.setMainDocNodeRef(testDocument.toString());
        return document;
    }

    private CaseDocument documentWithAttachment() {
        CaseDocument document = document();
        document.getAttachments().add(attachment());
        return document;
    }

    private CaseDocumentAttachment attachment() {
        CaseDocumentAttachment attachment = new CaseDocumentAttachment();
        attachment.setNodeRef(testDocumentAttachment.toString());
        return attachment;
    }

    private void createNewVersionOfCaseDocAndAttachment() {
        createNewDocVersion(testDocument, "some new content", VersionType.MAJOR);
        createNewDocVersion(testDocumentAttachment, "attachment some new content", VersionType.MINOR);
    }

    private void createNewDocVersion(NodeRef docRef, String content, VersionType versionType) {
        Map<String, Serializable> versionProps = new HashMap<>();
        versionProps.put(VersionModel.PROP_VERSION_TYPE, versionType);
        NodeRef workingCopy = checkOutCheckInService.checkout(docRef);
        ContentWriter writer = contentService.getWriter(workingCopy, ContentModel.PROP_CONTENT, true);
        writer.setMimetype("text");
        writer.putContent(content);
        checkOutCheckInService.checkin(workingCopy, versionProps);
    }
}
