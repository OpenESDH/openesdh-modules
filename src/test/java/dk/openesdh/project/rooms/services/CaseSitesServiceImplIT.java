package dk.openesdh.project.rooms.services;

import static dk.openesdh.repo.model.CaseDocumentJson.MAIN_DOC_NODE_REF;
import static dk.openesdh.repo.model.CaseDocumentJson.NODE_REF;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import dk.openesdh.repo.model.ResultSet;
import dk.openesdh.repo.services.TransactionRunner;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.documents.DocumentService;
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
    @Qualifier("CaseSitesService")
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
    @Qualifier("TransactionRunner")
    private TransactionRunner tr;

    private static final String TEST_FOLDER_NAME = "DocumentServiceImpIT";
    private static final String TEST_CASE_NAME1 = "TestCase1";
    private static final String TEST_DOCUMENT_NAME = "TestDocument";
    private static final String TEST_DOCUMENT_FILE_NAME = TEST_DOCUMENT_NAME + ".txt";
    private static final String TEST_DOCUMENT_ATTACHMENT_NAME = "TestDocumentAttachment";
    private static final String TEST_DOCUMENT_ATTACHMENT_FILE_NAME = TEST_DOCUMENT_ATTACHMENT_NAME + ".txt";

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
        List<NodeRef> folders = Arrays.asList(new NodeRef[] { testFolder });
        List<NodeRef> cases = Arrays.asList(new NodeRef[] { testCase1 });
        List<String> users = Arrays.asList(new String[] { CaseHelper.DEFAULT_USERNAME });
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
        Map<String, Serializable> versionProps = new HashMap<>();
        versionProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);
        NodeRef workingCopy = checkOutCheckInService.checkout(testDocument);
        ContentWriter writer = contentService.getWriter(workingCopy, ContentModel.PROP_CONTENT, true);
        writer.setMimetype("text");
        writer.putContent("some new content");
        checkOutCheckInService.checkin(workingCopy, versionProps);

        versionProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MINOR);
        NodeRef attachmentWorkingCopy = checkOutCheckInService.checkout(testDocumentAttachment);
        ContentWriter attachmentWriter = contentService.getWriter(attachmentWorkingCopy, ContentModel.PROP_CONTENT,
                true);
        attachmentWriter.setMimetype("text");
        attachmentWriter.putContent("attachment some new content");
        checkOutCheckInService.checkin(attachmentWorkingCopy, versionProps);
    }
}
