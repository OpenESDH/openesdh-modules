package dk.openesdh.doctemplates.services.documenttemplate;

import dk.openesdh.doctemplates.model.DocumentTemplateInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

/**
 * Created by Arnas on 21/09/15.
 */
public interface DocumentTemplateService {

    /**
     * @param filter
     * @param size
     * @return
     */
    public List<DocumentTemplateInfo> findTemplates(String filter, int size);

    public DocumentTemplateInfo getTemplateInfo(NodeRef templateNodeRef);

    public JSONArray buildDocTemplateJSON(List<DocumentTemplateInfo> templates)  throws JSONException;

}
