package dk.openesdh.project.rooms.services;

import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;

import dk.openesdh.project.rooms.model.CaseSiteDocument;

public interface CaseSiteDocumentsService {

    /**
     * Creates a new version of the provided case document and it's attachments.
     * 
     * @param caseDocumentFolder
     *            Node ref of the case document record to create new version for
     * @param newVersionDocumentFolder
     *            Node ref of the folder to find content for main document and
     *            attachments for the new document version
     */
    public void createNewVersionOfCaseDocument(NodeRef caseDocumentFolder, NodeRef newVersionDocumentFolder);

    /**
     * Creates a new version of the case main document
     * 
     * @param caseDocumentFolder
     *            Node ref of the case document record to create new version for
     * @param newVersionDocumentContent
     *            Node ref of the content for new version of the document
     * @return Node ref of the new document version
     */
    public NodeRef createNewVersionOfCaseMainDocument(NodeRef caseDocumentFolder,
            NodeRef newVersionDocumentContent);

    /**
     * Retrieves case or site document by provided document node ref
     * 
     * @param nodeRef
     *            Node ref of the document to retrieve
     * @return requested document object
     */
    public CaseSiteDocument getCaseSiteDocument(NodeRef nodeRef);

    /**
     * Retrieves documents of the provided case
     * 
     * @param caseNodeRef
     *            Node ref of the case to retrieve documents from
     * @return documents of the provided case
     */
    public List<CaseSiteDocument> getCaseDocuments(NodeRef caseNodeRef);

    /**
     * Creates a copy of the provided content document in the provided target
     * folder
     * 
     * @param documentFile
     *            Document to copy
     * @param targetDocumentFolder
     *            Folder to copy document into
     * @return Node ref of the created document copy
     */
    public NodeRef copyDocumentFileToDocumentFolder(CaseSiteDocument documentFile, NodeRef targetDocumentFolder);
}
