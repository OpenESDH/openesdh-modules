package dk.openesdh.casetemplates.services.audit;

import static dk.openesdh.repo.services.audit.AuditEntryHandler.REC_TYPE.ATTACHMENT;
import static dk.openesdh.repo.services.audit.AuditEntryHandler.REC_TYPE.DOCUMENT;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_ACTION;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_ASPECT_ADD;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_PROPERTIES_ADD;
import static dk.openesdh.repo.services.audit.entryhandlers.TransactionPathAuditEntryHandler.TRANSACTION_TYPE;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.openesdh.casetemplates.services.CaseTemplatesFolderService;
import dk.openesdh.repo.model.OpenESDHModel;
import dk.openesdh.repo.services.audit.AuditEntry;
import dk.openesdh.repo.services.audit.AuditEntryHandler;
import dk.openesdh.repo.services.audit.AuditSearchService;
import dk.openesdh.repo.services.audit.AuditUtils;

@Component
public class CaseTemplateDocCopyAuditEntryHandler extends AuditEntryHandler {

    private static final String TRANSACTION_ACTION_COPY = "COPY";
    private static final String TRANSACTION_COPY_FROM_PATH = "/esdh/transaction/copy/from/path";
    private static final String DOC_TYPE_PREFIX = OpenESDHModel.DOC_PREFIX + ":";
    private static final String DOC_TYPE_SIMPLE = DOC_TYPE_PREFIX + OpenESDHModel.TYPE_DOC_SIMPLE.getLocalName();

    @Autowired
    private AuditSearchService auditSearchService;

    @PostConstruct
    public void init() {
        auditSearchService.addTransactionPathEntryHandler(this::canHandle, this);
    }

    public boolean canHandle(Map<String, Serializable> values) {
        String trAction = (String) values.get(TRANSACTION_ACTION);
        return TRANSACTION_ACTION_COPY.equals(trAction)
                && ((String) values.get(TRANSACTION_COPY_FROM_PATH)).startsWith(CaseTemplatesFolderService.CASE_TEMPLATES_ROOT_FOLDER_PATH);
    }

    @Override
    public Optional<AuditEntry> handleEntry(AuditEntry auditEntry, Map<String, Serializable> values) {
        String type = (String) values.get(TRANSACTION_TYPE);
        Set<QName> aspectsAdd = (Set<QName>) values.get(TRANSACTION_ASPECT_ADD);
        Map<QName, Serializable> properties = (Map<QName, Serializable>) values.get(TRANSACTION_PROPERTIES_ADD);
        if (aspectsAdd.contains(OpenESDHModel.ASPECT_DOC_IS_MAIN_FILE)) {
            return Optional.empty();
            // Adding main doc, don't log an entry because you would
            // get two entries when adding a document: one for the record
            // and one for the main file
        }
        if (type.startsWith(DOC_TYPE_PREFIX)) {
            Optional<String> title = AuditUtils.getLocalizedProperty(properties, ContentModel.PROP_TITLE);
            if (!title.isPresent()) {
                return Optional.empty();
            }
            if (type.equals(DOC_TYPE_SIMPLE)) {
                auditEntry.setType(DOCUMENT);
                auditEntry.setAction("auditlog.label.document.added");
                auditEntry.addData("title", title.get());
            } else {
                auditEntry.setType(ATTACHMENT);
                auditEntry.setAction("auditlog.label.attachment.added");
                auditEntry.addData("title", title.get());
            }
        } else {
            return Optional.empty();
        }
        return Optional.of(auditEntry);
    }
}
