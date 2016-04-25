package dk.openesdh.office.model;

import org.alfresco.service.namespace.QName;

public class OutlookModel {

    public static final String OFFICE_URI = "http://openesdh.dk/model/office/1.0";
    public static final String OFFICE_PREFIX = "office";
    public static final String FROM_OUTLOOK = "fromOutlook";

    /**
     * Models
     */
    public static final QName ASPECT_OFFICE_OUTLOOK_RECEIVABLE = QName.createQName(OFFICE_URI, "outlookReceivable");
    public static final QName PROP_OFFICE_OUTLOOK_RECEIVED = QName.createQName(OFFICE_URI, FROM_OUTLOOK);

}
