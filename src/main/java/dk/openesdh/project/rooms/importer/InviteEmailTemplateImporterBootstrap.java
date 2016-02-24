package dk.openesdh.project.rooms.importer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.alfresco.repo.importer.ImporterBootstrap;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.view.ImporterBinding.UUID_BINDING;
import org.alfresco.service.namespace.NamespaceService;

import dk.openesdh.project.rooms.model.ProjectRoomsModule;
import dk.openesdh.repo.services.TransactionRunner;

public class InviteEmailTemplateImporterBootstrap extends ImporterBootstrap {

    private static final String[] ALFRESCO_LOCALES = new String[] { "de", "es", "fr", "it", "ja", "nl" };
    private static final String INVITE_TEMPLATES_HOME_PATH = "/app:company_home/app:dictionary/app:email_templates/cm:invite";
    private static final String INVITE_TEMPLATE_NAME = "invite-email";
    private static final String INVITE_TEMPLATE_EXT = ".html.ftl";
    private static final String INVITE_TEMPLATE_PATH = INVITE_TEMPLATES_HOME_PATH + "/cm:" + INVITE_TEMPLATE_NAME
            + INVITE_TEMPLATE_EXT;
    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private Repository repositoryHelper;
    private NamespaceService namespaceService;
    private SearchService searchService;
    private TransactionRunner transactionRunner;

    public InviteEmailTemplateImporterBootstrap() {
        setUseExistingStore(true);
        Properties bootStrapView = new Properties();
        bootStrapView.put(VIEW_PATH_PROPERTY, INVITE_TEMPLATES_HOME_PATH);
        bootStrapView.put(VIEW_LOCATION_VIEW,
                "alfresco/templates/invite-email-templates/invite-email-templates.xml");
        bootStrapView.put(VIEW_UUID_BINDING, UUID_BINDING.UPDATE_EXISTING.name());
        setBootstrapViews(Arrays.asList(bootStrapView));
    }

    @Override
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
        super.setNodeService(nodeService);
    }

    @Override
    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
        super.setNamespaceService(namespaceService);
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        this.fileFolderService = fileFolderService;
    }

    public void setRepositoryHelper(Repository repositoryHelper) {
        this.repositoryHelper = repositoryHelper;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setTransactionRunner(TransactionRunner transactionRunner) {
        this.transactionRunner = transactionRunner;
    }

    @Override
    public void bootstrap() {
        transactionRunner.runAsSystem(() -> {
            return transactionRunner.runInTransaction(() -> {
                bootstrapImpl();
                return null;
            });
        });
    }

    private void bootstrapImpl() {
        try {
            boolean performBootstrap = removeDefaultTemplates();
            if (!performBootstrap) {
                return;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        super.bootstrap();
    }

    private boolean removeDefaultTemplates() throws FileNotFoundException {

        List<NodeRef> refs = searchService.selectNodes(repositoryHelper.getRootHome(), INVITE_TEMPLATE_PATH, null,
                namespaceService, false);
        if (refs.size() != 1) {
            return false;
        }

        NodeRef inviteTemplateRef = refs.get(0);
        
        Boolean alreadyUpdated = (Boolean) nodeService.getProperty(inviteTemplateRef,
                ProjectRoomsModule.PROP_PR_UPDATED);
        if (Boolean.TRUE.equals(alreadyUpdated)) {
            return true;
        }
        
        NodeRef inviteTemplatesFolderRef = nodeService.getPrimaryParent(inviteTemplateRef)
                .getParentRef();

        for (String locale : ALFRESCO_LOCALES) {
            String localeTemplateName = INVITE_TEMPLATE_NAME + "_" + locale + INVITE_TEMPLATE_EXT;
            removeTemplate(inviteTemplatesFolderRef, localeTemplateName);
        }

        nodeService.deleteNode(inviteTemplateRef);
        
        return true;
    }

    private void removeTemplate(NodeRef folderRef, String templateName) {
        NodeRef templateRef = fileFolderService.searchSimple(folderRef, templateName);
        if(Objects.isNull(templateRef)){
            return;
        }
        nodeService.deleteNode(templateRef);
    }

}
