package dk.openesdh.project.rooms.model;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.cmr.site.SiteVisibility;

public class CaseSite {

    private String caseId;
    private String sitePreset;
    private String shortName;
    private String title;
    private String description;
    private SiteVisibility visibility;
    private String nodeRef;
    // Node refs of the documents record folders
    private List<String> siteDocuments = new ArrayList<String>();
    private List<SiteMember> siteMembers = new ArrayList<CaseSite.SiteMember>();
    private List<SiteParty> siteParties = new ArrayList<CaseSite.SiteParty>();

    private PersonInfo creator;

    public CaseSite() {

    }

    public CaseSite(String caseId, String sitePreset, String shortName, String title, String description,
            SiteVisibility visibility) {
        super();
        this.caseId = caseId;
        this.sitePreset = sitePreset;
        this.shortName = shortName;
        this.title = title;
        this.description = description;
        this.visibility = visibility;
    }

    public CaseSite(String caseId, String sitePreset, String shortName, String title, String description,
            SiteVisibility visibility, String nodeRef, PersonInfo creator) {
        super();
        this.caseId = caseId;
        this.sitePreset = sitePreset;
        this.shortName = shortName;
        this.title = title;
        this.description = description;
        this.visibility = visibility;
        this.nodeRef = nodeRef;
        this.creator = creator;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSitePreset() {
        return sitePreset;
    }

    public void setSitePreset(String sitePreset) {
        this.sitePreset = sitePreset;
    }

    public SiteVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(SiteVisibility visibility) {
        this.visibility = visibility;
    }

    public String getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(String nodeRef) {
        this.nodeRef = nodeRef;
    }

    /**
     * 
     * @return List of the documents record folders node refs
     */
    public List<String> getSiteDocuments() {
        return siteDocuments;
    }

    /**
     * Sets list of the doucments record folders node refs
     * 
     * @param siteDocuments
     */
    public void setSiteDocuments(List<String> siteDocuments) {
        this.siteDocuments = siteDocuments;
    }

    public List<SiteMember> getSiteMembers() {
        return siteMembers;
    }

    public void setSiteMembers(List<SiteMember> siteMembers) {
        this.siteMembers = siteMembers;
    }

    public List<SiteParty> getSiteParties() {
        return siteParties;
    }

    public void setSiteParties(List<SiteParty> siteParties) {
        this.siteParties = siteParties;
    }

    public PersonInfo getCreator() {
        return creator;
    }

    public void setCreator(PersonInfo creator) {
        this.creator = creator;
    }

    public static class SiteMember {
        private String authority;
        private String role;

        public SiteMember() {

        }

        public SiteMember(String authority, String role) {
            super();
            this.authority = authority;
            this.role = role;
        }

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class SiteParty {
        private String contactId;
        private String nodeRef;
        private String role;

        public SiteParty() {

        }

        public SiteParty(String contactId, String nodeRef, String role) {
            super();
            this.contactId = contactId;
            this.nodeRef = nodeRef;
            this.role = role;
        }

        public String getContactId() {
            return contactId;
        }

        public void setContactId(String contactId) {
            this.contactId = contactId;
        }

        public String getNodeRef() {
            return nodeRef;
        }

        public void setNodeRef(String nodeRef) {
            this.nodeRef = nodeRef;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
