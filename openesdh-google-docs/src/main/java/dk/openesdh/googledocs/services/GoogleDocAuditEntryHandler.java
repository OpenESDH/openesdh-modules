package dk.openesdh.googledocs.services;

import static dk.openesdh.repo.services.audit.AuditEntryHandler.REC_TYPE.DOCUMENT;
import static dk.openesdh.repo.services.audit.AuditUtils.getTitle;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_ACTION;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_ACTION_CREATE_VERSION;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_PROPERTIES_FROM;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_PROPERTIES_TO;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.openesdh.googledocs.model.OpenEsdhGoogleDocsModel;
import dk.openesdh.repo.services.audit.AuditEntry;
import dk.openesdh.repo.services.audit.AuditEntryHandler;
import dk.openesdh.repo.services.audit.AuditSearchService;

@Component
public class GoogleDocAuditEntryHandler extends AuditEntryHandler {

    private static final String TRANSACTION_ASPECTS_DELETE = "/esdh/transaction/aspects/delete";

    @Autowired
    private AuditSearchService auditSearchService;

    @PostConstruct
    public void init() {
        auditSearchService.registerIgnoredAspects(OpenEsdhGoogleDocsModel.ASPECT_EDITING_IN_GOOGLE);
        auditSearchService.addTransactionPathEntryHandler(this::canHandle, this);
    }

    private boolean canHandle(Map<String, Serializable> values) {
        if (values.get(TRANSACTION_ACTION).equals(TRANSACTION_ACTION_CREATE_VERSION)) {
            if (values.containsKey(TRANSACTION_ASPECTS_DELETE)) {
                Set<QName> aspectDelete = (Set<QName>) values.get(TRANSACTION_ASPECTS_DELETE);
                return aspectDelete.contains(OpenEsdhGoogleDocsModel.ASPECT_EDITING_IN_GOOGLE);
            }
        }
        return false;
    }

    @Override
    public Optional<AuditEntry> handleEntry(AuditEntry auditEntry, Map<String, Serializable> values) {
        String oldVersion = (String) getFromPropertyMap(
                values, TRANSACTION_PROPERTIES_FROM, ContentModel.PROP_VERSION_LABEL);
        String newVersion = (String) getFromPropertyMap(
                values, TRANSACTION_PROPERTIES_TO, ContentModel.PROP_VERSION_LABEL);

        auditEntry.setType(DOCUMENT);
        auditEntry.setAction("GOOGLE.DOCS.auditlog.EDIT");
        auditEntry.addData("title", getTitle(values));
        auditEntry.addData("oldVersion", oldVersion);
        auditEntry.addData("newVersion", newVersion);
        return Optional.of(auditEntry);
    }

    private Serializable getFromPropertyMap(Map<String, Serializable> values, String mapProperty, QName name) {
        return values.containsKey(mapProperty) ? ((Map<QName, Serializable>) values.get(mapProperty)).get(name) : null;
    }
}
