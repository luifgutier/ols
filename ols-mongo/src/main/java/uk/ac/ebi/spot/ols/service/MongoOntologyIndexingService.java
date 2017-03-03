package uk.ac.ebi.spot.ols.service;

import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.ols.config.OntologyResourceConfig;
import uk.ac.ebi.spot.ols.exception.IndexingException;
import uk.ac.ebi.spot.ols.exception.OntologyLoadingException;
import uk.ac.ebi.spot.ols.loader.OntologyLoader;
import uk.ac.ebi.spot.ols.loader.OntologyLoaderFactory;
import uk.ac.ebi.spot.ols.model.Status;
import uk.ac.ebi.spot.ols.model.OntologyDocument;
import uk.ac.ebi.spot.ols.model.OntologyIndexer;
import uk.ac.ebi.spot.ols.xrefs.DatabaseService;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Simon Jupp
 * @date 04/03/2015
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Component
public class MongoOntologyIndexingService implements OntologyIndexingService{

    private Logger log = LoggerFactory.getLogger(getClass());

    public Logger getLog() {
        return log;
    }

    @Autowired
    OntologyRepositoryService ontologyRepositoryService;

    @Autowired(required=false)
    List<OntologyIndexer> indexers;

    @Autowired
    DatabaseService databaseService;

    @Override
    public void indexOntologyDocument(OntologyDocument document) throws IndexingException {

        OntologyLoader loader = null;
        Collection<IRI> classes;
        Collection<IRI> properties;
        Collection<IRI> individuals;
        String message = "";
        Status status = Status.FAILED;

        try {
            loader = OntologyLoaderFactory.getLoader(document.getConfig(), databaseService);
            if (document.getLocalPath() != null) {
                // if updated get local path, and set location to local file
                loader.setOntologyResource(new FileSystemResource(document.getLocalPath()));
            }
            classes = loader.getAllClasses();
            properties = loader.getAllObjectPropertyIRIs();
            individuals = loader.getAllIndividualIRIs();


            // this means that file parsed, but had nothing in it, which is a bit suspect - indexing should fail until we undertand why/how this could happen
            if (classes.size() + properties.size() + individuals.size()== 0) {
                getLog().error("A suspiciously small or zero classes or properties found in latest version of " + loader.getOntologyName() + ": Won't index!");
                message = "Failed to load - last update had no classes or properties so was rejected";
                document.setStatus(Status.FAILED);
                document.setMessage(message);
                ontologyRepositoryService.update(document);
                // don't try to index, just return
                throw new IndexingException("Empty ontology found", new RuntimeException());
            }

        } catch (Exception e) {
            message = e.getMessage() + ":" + e.getCause().getMessage();
            getLog().error(message);
            document.setStatus(Status.FAILED);
            document.setMessage(message);
            ontologyRepositoryService.update(document);
            // just set document to failed and return
            return;
        }

        document.setStatus(Status.LOADING);
        ontologyRepositoryService.update(document);
        // if we get to here, we should have at least loaded the ontology
        try {

            // get all the available indexers
            for (OntologyIndexer indexer : indexers) {
                // create the new index
                indexer.dropIndex(loader);
                indexer.createIndex(loader);
            }

            // update any ontology meta data
            OntologyResourceConfig config = document.getConfig();

            if (loader.getTitle() != null) {
                config.setTitle(loader.getTitle());
            }
            if (loader.getOntologyDescription() != null) {
                config.setDescription(loader.getOntologyDescription());
            }
            if (loader.getHomePage() != null) {
                config.setHomepage(loader.getHomePage());
            }
            if (loader.getMailingList() != null) {
                config.setMailingList(loader.getMailingList());
            }
            if (!loader.getCreators().isEmpty()) {
                config.setCreators(loader.getCreators());
            }
            if (!loader.getOntologyAnnotations().keySet().isEmpty()) {
                config.setAnnotations(loader.getOntologyAnnotations());
            }
            if (loader.getOntologyVersionIRI() != null) {
                config.setVersionIri(loader.getOntologyVersionIRI().toString());
            }
            if (!loader.getInternalMetadataProperties().isEmpty()) {
                config.setInternalMetadataProperties(loader.getInternalMetadataProperties());
            }

            // check for a version number or set to today date
            if (loader.getVersionNumber() != null) {
                config.setVersion(loader.getVersionNumber());
            }
            else {
                config.setVersion(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
            }
            document.setConfig(config);
            document.setNumberOfTerms(classes.size());
            document.setNumberOfProperties(properties.size());
            document.setNumberOfIndividuals(individuals.size());
            status = Status.LOADED;
            document.setLoaded(new Date());

        } catch (Exception e) {
            getLog().error("Error indexing " + document.getOntologyId(), e.getMessage());
            status = Status.FAILED;
            message = e.getMessage();
            throw e;
        }
        finally {

            document.setStatus(status);
            document.setUpdated(new Date());
            document.setMessage(message);
            ontologyRepositoryService.update(document);
        }
    }

    @Override
    public void eraseONtologyDocument(OntologyDocument document) throws IndexingException {
        OntologyLoader loader = null;
        Collection<IRI> classes;
        Collection<IRI> properties;
        Collection<IRI> individuals;
        String message = "";
        Status status = Status.FAILED;

        try {
            loader = OntologyLoaderFactory.getLoader(document.getConfig(), databaseService);
            if (document.getLocalPath() != null) {
                // if updated get local path, and set location to local file
                loader.setOntologyResource(new FileSystemResource(document.getLocalPath()));
            }
            classes = loader.getAllClasses();
            properties = loader.getAllObjectPropertyIRIs();
            individuals = loader.getAllIndividualIRIs();


            // this means that file parsed, but had nothing in it, which is a bit suspect - indexing should fail until we undertand why/how this could happen
            if (classes.size() + properties.size() + individuals.size()== 0) {
                getLog().error("A suspiciously small or zero classes or properties found in latest version of " + loader.getOntologyName() + ": Won't index!");
                message = "Failed to load - last update had no classes or properties so was rejected";
                
                ontologyRepositoryService.update(document);
                // don't try to index, just return
                throw new IndexingException("Empty ontology found", new RuntimeException());
            }

        } catch (Exception e) {
            message = e.getMessage() + ":" + e.getCause().getMessage();
            getLog().error(message);
            
            
            return;
        }

        try {

            for (OntologyIndexer indexer : indexers) {
                getLog().info("Borrando ontologia ");
                indexer.dropIndex(loader);
                
            }

            
        } catch (Exception e) {
            getLog().error("Error ereasing " + document.getOntologyId(), e.getMessage());
            status = Status.FAILED;
            message = e.getMessage();
            throw e;
        }
    }
        
}
