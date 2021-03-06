package dk.openesdh.doctemplates.services.officetemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.odftoolkit.odfdom.doc.OdfDocument;
import org.odftoolkit.odfdom.dom.element.text.TextUserFieldDeclElement;
import org.odftoolkit.simple.TextDocument;
import org.odftoolkit.simple.common.field.VariableField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.w3c.dom.NodeList;

import com.google.common.base.Joiner;

import dk.openesdh.doctemplates.api.model.OfficeTemplate;
import dk.openesdh.doctemplates.api.model.OfficeTemplateField;
import dk.openesdh.doctemplates.api.model.OfficeTemplateMerged;
import dk.openesdh.doctemplates.api.services.OfficeTemplateService;
import dk.openesdh.repo.model.CaseInfo;
import dk.openesdh.repo.model.OpenESDHModel;
import dk.openesdh.repo.services.cases.CaseService;
import dk.openesdh.repo.services.cases.CaseTypeService;
import dk.openesdh.repo.services.documents.DocumentEmailService;
import dk.openesdh.repo.services.documents.DocumentService;
import dk.openesdh.repo.webscripts.contacts.ContactUtils;

/**
 * Created by syastrov on 9/23/15.
 */
@Service("OfficeTemplateService")
public class OfficeTemplateServiceImpl implements OfficeTemplateService {

    private final Logger logger = LoggerFactory.getLogger(OfficeTemplateService.class);
    private static final String DEFAULT_TARGET_MIME_TYPE = MimetypeMap.MIMETYPE_PDF;
    /**
     * filter out fields filled automaticaly.
     * Template field names starting with "case." "user." "receiver."
     */
    private static final String AUTO_FILLED_FIELD_REGEX = "^(case|user|receiver)\\..+";

    @Autowired
    @Qualifier("NodeService")
    private NodeService nodeService;
    @Autowired
    @Qualifier("FileFolderService")
    private FileFolderService fileFolderService;
    @Autowired
    @Qualifier("repositoryHelper")
    private Repository repositoryHelper;
    @Autowired
    @Qualifier("ContentService")
    private ContentService contentService;
    @Autowired
    @Qualifier("mimetypeService")
    private MimetypeService mimetypeService;
    @Autowired
    @Qualifier(CaseService.BEAN_ID)
    private CaseService caseService;
    @Autowired
    @Qualifier("CaseTypeService")
    private CaseTypeService caseTypeService;
    @Autowired
    @Qualifier("PersonService")
    private PersonService personService;
    @Autowired
    @Qualifier(DocumentService.BEAN_ID)
    private DocumentService documentService;
    @Autowired
    @Qualifier(DocumentEmailService.BEAN_ID)
    private DocumentEmailService documentEmailService;

    @Override
    public NodeRef saveTemplate(String title, String description, NodeRef docType, NodeRef docCategory,
            String fileName, InputStream contentInputStream, String mimetype) {
        NodeRef templateStorageDir = getTemplateDirectory();
        FileInfo fileInfo = fileFolderService.create(templateStorageDir, fileName, ContentModel.TYPE_CONTENT);
        ContentWriter writer = contentService.getWriter(fileInfo.getNodeRef(), ContentModel.PROP_CONTENT, true);
        writer.setMimetype(getRealMimetype(fileName, mimetype));
        writer.setEncoding("UTF-8");
        writer.putContent(contentInputStream);
        nodeService.setProperty(fileInfo.getNodeRef(), ContentModel.PROP_TITLE, title);
        nodeService.setProperty(fileInfo.getNodeRef(), OpenESDHModel.PROP_DOC_TYPE, docType);
        nodeService.setProperty(fileInfo.getNodeRef(), OpenESDHModel.PROP_DOC_CATEGORY, docCategory);
        nodeService.setProperty(fileInfo.getNodeRef(), ContentModel.PROP_TITLE, title);
        if (StringUtils.isNotBlank(description)) {
            nodeService.setProperty(fileInfo.getNodeRef(), ContentModel.PROP_DESCRIPTION, description);
        }
        return fileInfo.getNodeRef();
    }

    private String getRealMimetype(String filename, String mimetype) {
        if (mimetype.equals(MimetypeMap.MIMETYPE_BINARY)) {
            return mimetypeService.guessMimetype(filename);
        }
        return mimetype;
    }

    @Override
    public void deleteTemplate(NodeRef nodeRef) {
        nodeService.deleteNode(nodeRef);
    }

    /**
     * Returns the a nodeRef representing the templates directory root.
     *
     * @return
     */
    private NodeRef getTemplateDirectory() {
        NodeRef rootNode = repositoryHelper.getCompanyHome();
        try {
            FileInfo folderInfo = fileFolderService.resolveNamePath(rootNode, Arrays.asList(OPENESDH_DOC_TEMPLATES_DEFAULT_PATH.split("/")));
            return folderInfo.getNodeRef();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to locate the folder where templates are stored.", e);
        }
    }

    @Override
    public List<OfficeTemplate> getTemplates() {
        NodeRef templateDirNode = getTemplateDirectory();
        return fileFolderService.listFiles(templateDirNode).stream()
                .map(fileInfo -> getTemplate(fileInfo.getNodeRef(), false, true))
                .collect(Collectors.toList());
    }

    @Override
    public OfficeTemplate getTemplate(NodeRef templateNodeRef) {
        return getTemplate(templateNodeRef, true, true);
    }

    @Override
    public OfficeTemplate getTemplate(NodeRef templateNodeRef, boolean withFields, boolean skipAutoFilledfields) {
        Map<QName, Serializable> properties = nodeService.getProperties(templateNodeRef);
        OfficeTemplate template = new OfficeTemplate();
        template.setNodeRef(templateNodeRef);
        template.setName((String) properties.get(ContentModel.PROP_NAME));
        template.setTitle((String) properties.get(ContentModel.PROP_TITLE));
        template.setDescription((String) properties.get(ContentModel.PROP_DESCRIPTION));
        template.setDocType((NodeRef) properties.get(OpenESDHModel.PROP_DOC_TYPE));
        template.setDocCategory((NodeRef) properties.get(OpenESDHModel.PROP_DOC_CATEGORY));
        if (withFields) {
            try {
                List<OfficeTemplateField> templateFields = getTemplateFields(templateNodeRef, skipAutoFilledfields);
                template.setFields(templateFields);
            } catch (Exception ex) {
                throw new RuntimeException("Cannot read template \"" + template.getName() + "\" fields", ex);
            }
        }
        return template;
    }

    private List<OfficeTemplateField> getTemplateFields(NodeRef templateNodeRef, boolean skipAutoFilled) throws Exception {
        List<OfficeTemplateField> templateFields = new ArrayList<>();
        ContentReader templateReader = contentService.getReader(templateNodeRef, ContentModel.PROP_CONTENT);
        InputStream inputStream = templateReader.getContentInputStream();
        OdfDocument doc = OdfDocument.loadDocument(inputStream);
        NodeList nodes = doc.getContentRoot().getElementsByTagName(TextUserFieldDeclElement.ELEMENT_NAME.getQName());
        for (int i = 0; i < nodes.getLength(); i++) {
            TextUserFieldDeclElement element = (TextUserFieldDeclElement) nodes.item(i);
            String fieldName = element.getTextNameAttribute();
            if (skipAutoFilled && isAutoFilledField(fieldName)) {
                continue;
            }
            OfficeTemplateField field = new OfficeTemplateField();
            field.setName(fieldName);
            field.setType(element.getOfficeValueTypeAttribute());
            // TODO: Implement field mappings. For now, the name is also the
            // mapping.
            field.setMappedFieldName(fieldName);

            // TODO: Support getting values of different value types
            field.setValue(element.getOfficeStringValueAttribute());
            templateFields.add(field);
        }
        return templateFields;
    }

    private boolean isAutoFilledField(String fieldName) {
        return fieldName.matches(AUTO_FILLED_FIELD_REGEX);
    }

    @Override
    public OfficeTemplateMerged renderTemplate(OfficeTemplate template, String caseId, NodeRef receiver,
            Map<String, Serializable> model) throws IOException {
        model.putAll(getCaseInfo(caseId));
        model.putAll(getUserInfo());
        if (receiver != null) {
            model.putAll(getReceiverInfo(receiver));
        }

        //add empty values for all other fields
        template.getFields().forEach(field -> {
            if (!model.containsKey(field.getName())) {
                model.put(field.getName(), "");
            }
        });

        ContentReader templateReader = contentService.getReader(template.getNodeRef(), ContentModel.PROP_CONTENT);

        //The option is needed for transformation to work on windows platform.
        //Seems like OpenOffice on windows requires file extension to be present.
        //The OOoContentTransformer uses sourceNodeRef to retrieve file name with extension.
        TransformationOptions options = new TransformationOptions();
        options.setSourceNodeRef(template.getNodeRef());

        InputStream inputStream = templateReader.getContentInputStream();

        File file = File.createTempFile("office-template-renderer-", null);
        ContentReader transformedReader = null;
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            renderODFTemplate(inputStream, outputStream, model);
            ContentReader reader = new FileContentReader(file);
            reader.setMimetype(mimetypeService.guessMimetype(null, templateReader));
            transformedReader = transformContent(reader, DEFAULT_TARGET_MIME_TYPE, options);
            reader.setMimetype(DEFAULT_TARGET_MIME_TYPE);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            boolean delete = file.delete();
            if (!delete) {
                file.deleteOnExit();
            }
        }
        if (transformedReader == null) {
            throw new RuntimeException("Failed to render template");
        }
        OfficeTemplateMerged merged = new OfficeTemplateMerged();

        String mergedFileName = Joiner.on("_").skipNulls().join(
                FilenameUtils.removeExtension(template.getName()),
                StringUtils.replace((String) model.get("receiver.name"), " ", "_"));

        merged.setFileName(mergedFileName + ".pdf");
        merged.setMimetype(transformedReader.getMimetype());
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        transformedReader.getContent(content);
        merged.setContent(content.toByteArray());
        merged.setDocumentType(template.getDocType());
        merged.setDocumentCategory(template.getDocCategory());
        merged.setReceiver(receiver);
        return merged;
    }

    private void renderODFTemplate(InputStream inputStream, OutputStream outputStream,
            Map<String, Serializable> model) throws Exception {
        TextDocument doc = TextDocument.loadDocument(inputStream);
        model.forEach((fieldName, value) -> {
            if (value != null) {
                VariableField variableField = doc.getVariableFieldByName(fieldName);
                if (variableField != null) {
                    variableField.updateField(value.toString(), null);
                } else {
                    logger.debug("Supposed to fill in field \"{}\" but it does not exist in the template", fieldName);
                }
            }
        });
        doc.save(outputStream);
    }

    private ContentReader transformContent(ContentReader sourceReader, String targetMimetype,
            TransformationOptions options) {
        // Create a temporary writer
        ContentWriter writer = contentService.getTempWriter();
        writer.setMimetype(DEFAULT_TARGET_MIME_TYPE);

        ContentTransformer transformer = contentService.getTransformer(sourceReader.getContentUrl(),
                sourceReader.getMimetype(), sourceReader.getSize(), targetMimetype, options);

        if (transformer == null) {
            throw new RuntimeException("Transformer to " + targetMimetype + " unavailable for "
                    + sourceReader.getMimetype());
        }
        contentService.transform(sourceReader, writer, options);
        return writer.getReader();
    }

    private Map<String, Serializable> getCaseInfo(String caseId) {
        Map<String, Serializable> props = new HashMap<>();
        CaseInfo caseInfo = caseService.getCaseInfo(caseId);
        props.put("case.id", caseId);
        props.put("case.title", caseInfo.getTitle());
        props.put("case.description", caseInfo.getDescription());
        props.put("case.journalKey", caseInfo.getCustomProperty(OpenESDHModel.PROP_OE_JOURNALKEY));
        props.put("case.journalFacet", caseInfo.getCustomProperty(OpenESDHModel.PROP_OE_JOURNALFACET));
        props.put("case.type", caseTypeService.getCaseTypeTitle(caseInfo.getNodeRef()));
        return cleanMap(props);
    }

    private Map<String, Serializable> getUserInfo() {
        NodeRef personNodeRef = personService.getPerson(AuthenticationUtil.getFullyAuthenticatedUser());
        Map<String, Serializable> props = new HashMap<>();
        Map<QName, Serializable> nodeProps = cleanMap(nodeService.getProperties(personNodeRef));
        nodeProps.forEach((key, value) -> props.put("user." + key.getLocalName(), value));
        //extra props
        props.put("user.name", (props.getOrDefault("user.firstName", "") + " " + props.getOrDefault("user.lastName", "")).trim());
        return props;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Serializable> getReceiverInfo(NodeRef receiver) {
        Map<QName, Serializable> contactProps = nodeService.getProperties(receiver);
        JSONObject contactJson = ContactUtils.createContactJson(receiver, contactProps);
        Map<String, Serializable> props = new HashMap<>();
        contactJson.forEach((key, value) -> props.put("receiver." + key, (Serializable) value));
        //extra props
        if (nodeService.getType(receiver).equals(OpenESDHModel.TYPE_CONTACT_ORGANIZATION)) {
            props.put("receiver.name", contactProps.get(OpenESDHModel.TYPE_CONTACT_ORGANIZATION));
        } else {
            props.put("receiver.name", (props.get("receiver.firstName") + " " + props.get("receiver.lastName")).trim());
        }
        return cleanMap(props);
    }

    private <K> Map<K, Serializable> cleanMap(Map<K, Serializable> map) {
        return map.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void saveToFolder(NodeRef targetFolderRef, List<OfficeTemplateMerged> merged){
        merged.forEach(document -> saveMergedToFolder(targetFolderRef, document));
    }
    
    private NodeRef saveMergedToFolder(NodeRef targetFolderRef, OfficeTemplateMerged document) {
        return documentService.createCaseDocumentInFolder(
                targetFolderRef,
                document.getFileName(),
                document.getFileName(),
                document.getDocumentType(),
                document.getDocumentCategory(),
                writer -> {
                    writer.setMimetype(document.getMimetype());
                    writer.putContent(new ByteArrayInputStream(document.getContent()));
                });
    }

    @Override
    public void sendToEmail(String caseId, NodeRef targetFolder, List<OfficeTemplateMerged> merged, String subject,
            String message) {
        merged.forEach(document -> sendMergedToEmail(caseId, targetFolder, document, subject, message));
    }

    private void sendMergedToEmail(String caseId, NodeRef targetFolder, OfficeTemplateMerged document, String subject, String message) {
        NodeRef caseDocNodeRef = documentService.getMainDocument(saveMergedToFolder(targetFolder, document));
        documentEmailService.send(caseId,
                Arrays.asList(document.getReceiver()),
                subject,
                message,
                Arrays.asList(caseDocNodeRef));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<OfficeTemplateMerged> getMergedTemplates(NodeRef templateNodeRef, String caseId, JSONObject json) throws IOException {
        OfficeTemplate template = getTemplate(templateNodeRef, true, false);
        JSONObject fieldData = (JSONObject) json.get("fieldData");
        JSONArray receivers = (JSONArray) fieldData.get("receivers");
        if (receivers.isEmpty()) {
            return Arrays.asList(renderTemplate(template, caseId, null, fieldData));
        }
        List<OfficeTemplateMerged> merged = new ArrayList<>();
        for (Object r : receivers) {
            JSONObject receiver = (JSONObject) r;
            merged.add(renderTemplate(
                    template,
                    caseId,
                    new NodeRef((String) receiver.get("nodeRef")),
                    fieldData));
        }
        return merged;
    }
}
