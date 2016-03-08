package dk.openesdh.project.rooms.model;

import org.alfresco.service.namespace.QName;

public interface ProjectRoomsModule {

    String MODULE_ID = "openesdh-project-rooms";

    String PR_URI = "http://openesdh.dk/model/projectrooms/1.0";

    QName PROP_PR_UPDATED = QName.createQName(PR_URI, "updated");
}
