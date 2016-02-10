package dk.openesdh.project.rooms.services;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteService;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dk.openesdh.project.rooms.model.CaseSiteDocument;
import dk.openesdh.repo.services.xsearch.CaseDocumentsSearchServiceImpl;

@Service("CaseSiteDocumentsService")
public class CaseSiteDocumentsServiceImpl extends CaseDocumentsSearchServiceImpl
        implements CaseSiteDocumentsService {

    @Autowired
    @Qualifier("SiteService")
    private SiteService siteService;
    @Autowired
    @Qualifier("CheckOutCheckInService")
    private CheckOutCheckInService checkOutCheckInService;
    @Autowired
    @Qualifier("ContentService")
    private ContentService contentService;

    @Override
    public CaseSiteDocument getCaseSiteDocument(NodeRef nodeRef) {
        String name = nodeService.getProperty(nodeRef, ContentModel.PROP_NAME).toString();
        String type = nodeService.getType(nodeRef).toString();
        return new CaseSiteDocument(name, nodeRef.toString(), type);
    }

    @Override
    public List<CaseSiteDocument> getCaseDocuments(NodeRef caseNodeRef) {
        return documentService.getDocumentsForCase(caseNodeRef)
                .stream()
                .map(assoc -> getCaseSiteDocument(assoc.getChildRef()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CaseSiteDocument> getCaseSiteDocuments(String siteShortName) {
        return getSiteDocumentsRefs(siteShortName).map(this::getCaseSiteDocument)
                .collect(Collectors.toList());
    }
    
    @Override
    public JSONArray getCaseSiteDocumentsJson(String siteShortName) {
        List<NodeRef> siteDocsRefs = getSiteDocumentsRefs(siteShortName).collect(Collectors.toList());
        return this.getNodesJSON(siteDocsRefs);
    }

    private Stream<NodeRef> getSiteDocumentsRefs(String siteShortName) {
        NodeRef documentLibrary = siteService.getContainer(siteShortName, SiteService.DOCUMENT_LIBRARY);
        return nodeService.getChildAssocs(documentLibrary)
                .stream()
                .map(ChildAssociationRef::getChildRef);
    }
}
