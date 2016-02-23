package dk.openesdh.project.rooms.audit;

import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_ACTION;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_ACTION_CREATE;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_ACTION_DELETE;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_PATH;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import javax.annotation.PostConstruct;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.stereotype.Component;

import dk.openesdh.project.rooms.services.CaseSitesService;
import dk.openesdh.repo.services.audit.AuditEntryHandler;
import dk.openesdh.repo.services.audit.AuditSearchService;

@Component
public class CaseSitesAuditEntryHandlers extends AuditEntryHandler {

    @Autowired
    private AuditSearchService auditSearchService;

    @PostConstruct
    public void init() {
        auditSearchService.addTransactionPathEntryHandler(this::canHandle, this);
    }

    private boolean canHandle(Map<String, Serializable> values) {
        String path = (String) values.get(TRANSACTION_PATH);
        return siteShortNameMatcher(path).find();
    }

    @Override
    public Optional<JSONObject> handleEntry(String user, long time, Map<String, Serializable> values) {
        String path = (String) values.get(TRANSACTION_PATH);
        Matcher matcher = siteShortNameMatcher(path);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String siteShortName = matcher.group(1);
        String transactionAction = (String) values.get(TRANSACTION_ACTION);

        JSONObject auditEntry = createNewAuditEntry(user, time);
        auditEntry.put("type", I18NUtil.getMessage("auditlog.label.type.site"));

        if (TRANSACTION_ACTION_CREATE.equals(transactionAction)) {
            auditEntry.put("action", I18NUtil.getMessage("auditlog.label.site.created", siteShortName));
            return Optional.of(auditEntry);
        } else if (TRANSACTION_ACTION_DELETE.equals(transactionAction)) {
            auditEntry.put("action", I18NUtil.getMessage("auditlog.label.site.deleted", siteShortName));
            return Optional.of(auditEntry);
        }

        return Optional.empty();
    }

    private Matcher siteShortNameMatcher(String path) {
        return CaseSitesService.SITE_PATH_SHORT_NAME_CASE_ID_REG_EX.matcher(path);
    }

}
