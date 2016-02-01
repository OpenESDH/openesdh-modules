package dk.openesdh.project.rooms.model;

public class CaseSiteDocument {

    private String name;
    private String nodeRef;
    private String type;

    public CaseSiteDocument() {

    }

    public CaseSiteDocument(String name, String nodeRef, String type) {
        super();
        this.name = name;
        this.nodeRef = nodeRef;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(String nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
