package dk.openesdh.googledocs.services;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import dk.openesdh.googledocs.model.OpenEsdhGoogleDocsModel;
import dk.openesdh.repo.services.documents.DocumentService;

@Component
public class GoogleDocInitComponent {

    @Autowired
    @Qualifier("DocumentService")
    private DocumentService documentService;

    @PostConstruct
    public void init() {
        documentService.addOtherPropNamespaceUris(OpenEsdhGoogleDocsModel.GOOGLEDOCS_MODEL_URI);
    }

}
