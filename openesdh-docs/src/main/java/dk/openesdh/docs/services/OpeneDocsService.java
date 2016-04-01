package dk.openesdh.docs.services;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.simple.JSONArray;

public interface OpeneDocsService {

    List<NodeRef> getDocumentTemplates(String... extensions);

    NodeRef createDocument(NodeRef targetFolderRef, NodeRef sourceTemplateRef, Map<QName, Serializable> props);

    JSONArray getDocumentTemplatesJson(String... extensions);
}
