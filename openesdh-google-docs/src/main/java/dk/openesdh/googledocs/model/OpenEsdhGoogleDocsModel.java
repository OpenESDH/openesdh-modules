package dk.openesdh.googledocs.model;

import org.alfresco.service.namespace.QName;

public interface OpenEsdhGoogleDocsModel {

    public static final String GOOGLEDOCS_MODEL_URI = "http://www.alfresco.org/model/googledocs/2.0";

    public static final QName ASPECT_EDITING_IN_GOOGLE = QName.createQName(GOOGLEDOCS_MODEL_URI, "editingInGoogle");
    public static final QName ASPECT_SHARED_IN_GOOGLE = QName.createQName(GOOGLEDOCS_MODEL_URI, "sharedInGoogle");

    public static final QName PROP_RESOURCE_ID = QName.createQName(GOOGLEDOCS_MODEL_URI, "resourceID");
    public static final QName PROP_LOCKED = QName.createQName(GOOGLEDOCS_MODEL_URI, "locked");
    public static final QName PROP_EDITOR_URL = QName.createQName(GOOGLEDOCS_MODEL_URI, "editorURL");
    public static final QName PROP_DRIVE_WORKING_DIR = QName.createQName(GOOGLEDOCS_MODEL_URI, "driveWorkingDir");
    public static final QName PROP_REVISION_ID = QName.createQName(GOOGLEDOCS_MODEL_URI, "revisionID");
    public static final QName PROP_PERMISSIONS = QName.createQName(GOOGLEDOCS_MODEL_URI, "permissions");
    public static final QName PROP_CURRENT_PERMISSIONS = QName.createQName(GOOGLEDOCS_MODEL_URI, "currentPermissions");
}
