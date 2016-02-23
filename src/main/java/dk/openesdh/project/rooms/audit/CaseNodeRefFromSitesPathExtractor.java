package dk.openesdh.project.rooms.audit;

import java.util.regex.Matcher;

import javax.annotation.PostConstruct;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dk.openesdh.project.rooms.services.CaseSitesService;
import dk.openesdh.repo.audit.CaseNodeRefExtractor;
import dk.openesdh.repo.services.cases.CaseService;

@Component
public class CaseNodeRefFromSitesPathExtractor {

    @Autowired
    private CaseService caseService;
    @Autowired
    @Qualifier(CaseNodeRefExtractor.BEAN_ID)
    private CaseNodeRefExtractor caseNodeRefExtractor;

    @PostConstruct
    public void init() {
        caseNodeRefExtractor.addNodeRefFromPathExtractor(this::pathStartsWithSitesRoot, this::getNodeRefFromSitesPath);
    }

    private boolean pathStartsWithSitesRoot(String path) {
        return path.startsWith(CaseSitesService.SITES_ROOT);
    }

    private String getNodeRefFromSitesPath(String path) {
        String caseId = getCaseIdFromSitePath(path);
        if (caseId == null) {
            return null;
        }
        NodeRef caseNodeRef = caseService.getCaseById(caseId);
        if (caseNodeRef != null) {
            return caseNodeRef.toString();
        }
        return null;
    }

    private String getCaseIdFromSitePath(String path) {
        Matcher matcher = CaseSitesService.SITE_PATH_SHORT_NAME_CASE_ID_REG_EX.matcher(path);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }
}
