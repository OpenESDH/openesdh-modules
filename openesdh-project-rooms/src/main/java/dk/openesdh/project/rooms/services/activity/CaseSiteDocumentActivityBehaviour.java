package dk.openesdh.project.rooms.services.activity;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateChildAssociationPolicy;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.PolicyScope;
import org.alfresco.repo.version.VersionServicePolicies.OnCreateVersionPolicy;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.project.rooms.services.CaseSitesService;
import dk.openesdh.repo.model.OpenESDHModel;

@Service
public class CaseSiteDocumentActivityBehaviour {

    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;
    @Autowired
    @Qualifier("policyComponent")
    private PolicyComponent policyComponent;
    @Autowired
    @Qualifier("NamespaceService")
    private NamespaceService namespaceService;
    @Autowired
    @Qualifier("FileFolderService")
    private FileFolderService fileFolderService;
    @Autowired
    @Qualifier("SearchService")
    private SearchService searchService;

    @Autowired
    @Qualifier("repositoryHelper")
    private Repository repositoryHelper;
    @Autowired
    @Qualifier(CaseSiteActivityService.BEAN_ID)
    private CaseSiteActivityService caseSiteActivityService;

    @PostConstruct
    public void init() {

        this.policyComponent.bindAssociationBehaviour(OnCreateChildAssociationPolicy.QNAME,
                OpenESDHModel.TYPE_DOC_SIMPLE, ContentModel.ASSOC_CONTAINS, new JavaBehaviour(this,
                        "onNewSiteDocument", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

        this.policyComponent.bindClassBehaviour(OnCreateVersionPolicy.QNAME, OpenESDHModel.TYPE_DOC_FILE,
                new JavaBehaviour(this, "onNewSiteDocumentVersion"));
    }

    public void onNewSiteDocument(ChildAssociationRef childAssocRef, boolean isNewNode) {
        NodeRef docNodeRef = childAssocRef.getChildRef();
        if (!nodeService.exists(docNodeRef)) {
            return;
        }
        Optional<NodeRef> siteNodeRef = getCaseSiteNodeRef(docNodeRef);
        if (!siteNodeRef.isPresent()) {
            return;
        }
        if (nodeService.countChildAssocs(childAssocRef.getParentRef(), true) == 1) {
            caseSiteActivityService.postOnCaseSiteNewDocument(siteNodeRef.get(), docNodeRef);
        } else {
            caseSiteActivityService.postOnCaseSiteNewDocumentAttachment(siteNodeRef.get(), docNodeRef);
        }
    }
    
    public void onNewSiteDocumentVersion(QName classRef, NodeRef versionableNode,
            Map<String, Serializable> versionProperties, PolicyScope nodeDetails) {
        Optional<NodeRef> siteNodeRef = getCaseSiteNodeRef(versionableNode);
        if (!siteNodeRef.isPresent()) {
            return;
        }
        if (nodeService.hasAspect(versionableNode, OpenESDHModel.ASPECT_DOC_IS_MAIN_FILE)) {
            caseSiteActivityService.postOnCaseSiteNewDocumentVersion(siteNodeRef.get(), versionableNode);
        } else {
            caseSiteActivityService.postOnCaseSiteNewDocumentAttachmentVersion(siteNodeRef.get(), versionableNode);
        }
    }

    private Optional<NodeRef> getCaseSiteNodeRef(NodeRef docNodeRef) {
        Path docPath = nodeService.getPath(docNodeRef);
        String docPathPrefixString = docPath.toPrefixString(namespaceService);
        if (!docPathPrefixString.startsWith(CaseSitesService.SITES_ROOT)) {
            return Optional.empty();
        }
        
        List<NodeRef> refs = searchService.selectNodes(repositoryHelper.getRootHome(), docPath.subPath(3).toPrefixString(namespaceService), null,
                namespaceService, false);
        if (refs.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(refs.get(0));
    }
}
