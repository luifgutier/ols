package uk.ac.ebi.spot.ols.controller.ui;

import java.io.BufferedWriter;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import uk.ac.ebi.spot.ols.exception.ErrorMessage;
import uk.ac.ebi.spot.ols.model.OntologyDocument;
import uk.ac.ebi.spot.ols.neo4j.model.Term;
import uk.ac.ebi.spot.ols.model.Status;
import uk.ac.ebi.spot.ols.neo4j.service.OntologyTermGraphService;
import uk.ac.ebi.spot.ols.service.OntologyRepositoryService;
import uk.ac.ebi.spot.ols.util.OLSEnv;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import uk.ac.ebi.spot.ols.config.YamlConfigParser;

/**
 * @author Simon Jupp
 * @date 15/07/2015
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
@Controller
@RequestMapping("/ontologies")
public class OntologyControllerUI {

    @Autowired
    private HomeController homeController;

    @Autowired
    OntologyRepositoryService repositoryService;


    // Reading these from application.properties
    @Value("${ols.downloads.folder:}")
    private String downloadsFolder;

    @Autowired
    ResourceLoader resourceLoader;
    
    
    @RequestMapping(path = "", method = RequestMethod.GET)
    String getAll(Model model) {
        List list = repositoryService.getAllDocuments(new Sort(new Sort.Order(Sort.Direction.ASC, "ontologyId")));
        model.addAttribute("all_ontologies", list);
        return "browse";
    }

    @RequestMapping(path = "/{onto}", method = RequestMethod.GET)
    String getTerm(
            @PathVariable("onto") String ontologyId,
            Model model) throws ResourceNotFoundException {

        ontologyId = ontologyId.toLowerCase();
        if (ontologyId != null) {
            OntologyDocument document = repositoryService.get(ontologyId);
            if (document == null) {
                throw new ResourceNotFoundException("Ontology called " + ontologyId + " not found");
            }

            String contact = document.getConfig().getMailingList();
            try {
                InternetAddress address = new InternetAddress(contact, true);
                contact = "mailto:" + contact;
            } catch (Exception e) {
              // only thrown if not valid e-mail, so contact must be URL of some sort
            }
            model.addAttribute("contact", contact);

            model.addAttribute("ontologyDocument", document);
        }
        else {
            return homeController.doSearch(
                    "*",
                    null,
                    null,null, null, false, null, false, false, null, 10,0,model);
        }
        return "ontology";
    }

    @RequestMapping(path = "/{onto}", produces = "application/rdf+xml", method = RequestMethod.GET)
    public @ResponseBody FileSystemResource getOntologyDirectDownload(@PathVariable("onto") String ontologyId, HttpServletResponse response) throws ResourceNotFoundException {
        return getDownloadOntology(ontologyId, response);
    }


    @RequestMapping(path = "/{onto}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE, method = RequestMethod.GET)
    public @ResponseBody  FileSystemResource getDownloadOntology(@PathVariable("onto") String ontologyId, HttpServletResponse response) throws ResourceNotFoundException {
        try {
            response.setHeader( "Content-Disposition", "filename=" + ontologyId + ".owl" );
            return new FileSystemResource(getDownloadFile(ontologyId));
        } catch (FileNotFoundException e) {
            throw new ResourceNotFoundException("This ontology is not available for download");
        }
    }
    
    @RequestMapping(path = "/{onto}/forceupdate", method = RequestMethod.GET)
    public @ResponseBody  Boolean forceUpdateOntology(@PathVariable("onto") String ontologyId, HttpServletResponse response) throws ResourceNotFoundException {
        Boolean retorno = true;
        try{
            ontologyId = ontologyId.toLowerCase();
            if (ontologyId != null) {
                OntologyDocument document = repositoryService.get(ontologyId);
                if (document == null) {
                    retorno = false;
                }else{
                    document.setStatus(Status.TOLOAD);
                    repositoryService.update(document);
                }
                
                /**
                * Se inicia el proceso de carga de la ontologia
                * 
                * Los pasos para la carga de la ontologia son:
                * 
                * se supone que la aplicación esta instalada en /opt/OLS
                * 
                * 
                * 1 . Apagar tomcat (./shutdown.sh)
                * 2 . Ir al directorio (cd /opt/OLS/ols-apps/ols-loading-app/target)
                * 3 . Ejecutar el .jar para crear los indices de la ontologia (java -jar ols-indexer.jar)
                * 4 . Ir al directorio donde esta el tomcat  (cd tomcat/bin/)
                * 5. Iniciar tomcat (./startup.sh)
                *
                **/


               
            }
            else {
                retorno = false;
            }
        }catch(Exception e){
            retorno = false;
        }
                
        return retorno;
            
        
    }    

    @RequestMapping(path = "/{onto}/delete", method = RequestMethod.GET)
    public String deleteOntology(@PathVariable("onto") String ontologyId, HttpServletResponse response,Model model) throws ResourceNotFoundException {
        String mensaje  = "Ontologia eliminada con exito, en estos momentos la aplicación estara fuera de funcionamiento, por favor espera un momento";
        try{
            ontologyId = ontologyId.toLowerCase();
            FileInputStream fw = new FileInputStream(new File("/opt/OLS/ols-apps/ols-config-importer/src/main/resources/ols-config.yaml"));
            YamlConfigParser yamlConfigParser = new YamlConfigParser(new Resource() {
                @Override
                public boolean exists() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public boolean isReadable() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public boolean isOpen() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public URL getURL() throws IOException {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public URI getURI() throws IOException {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public File getFile() throws IOException {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public long contentLength() throws IOException {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public long lastModified() throws IOException {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public Resource createRelative(String string) throws IOException {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String getFilename() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public String getDescription() {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            });
            
            Boolean estado = yamlConfigParser.doDeleteOntology(ontologyId, fw);
            if(estado){
                try{

                     /**
                     * Se inicia el proceso de carga de la ontologia
                     * 
                     * Los pasos para la carga de la ontologia son:
                     * 
                     * se supone que la aplicación esta instalada en /opt/OLS
                     * 
                     * 1 . Ir a la ruta /opt/OLS/ols-apps/ols-config-importer (cd /opt/OLS/ols-apps/ols-config-importer)
                     * 2 . Realizar una compilación limpia de la aplicación (mvn clean install)
                     * 3 . Ir a target donde se encuentran los .jar generado (cd /opt/OLS/ols-apps/ols-config-importer/target)
                     * 4 . Ejecutar el .jar generado (java -jar ols-config-importer.jar)
                     * 5 . Ir al directorio donde esta el tomcat (cd tomcat/bin/)
                     * 6 . Apagar tomcat (./shutdown.sh)
                     * 7 . Ir al directorio (cd /opt/OLS/ols-apps/ols-loading-app/target)
                     * 8 . Ejecutar el .jar para crear los indices de la ontologia (java -jar ols-indexer.jar)
                     * 9 . Ir al directorio donde esta el tomcat  (cd tomcat/bin/)
                     * 10. Iniciar tomcat (./startup.sh)
                     *
                     **/

                    File ruta = new File("/opt/OLS/ols-core");
                    //Paso 2.
                    InputStream is = Runtime.getRuntime().exec("mvn clean install",null,ruta).getInputStream();
                    imprimirLog(is);
                    //Paso 1.
                    ruta = new File("/opt/OLS/ols-apps/ols-config-importer");
                    //Paso 2.
                    is = Runtime.getRuntime().exec("mvn clean install",null,ruta).getInputStream();
                    imprimirLog(is);
                    //Paso 3.
                    ruta = new File("/opt/OLS/ols-apps/ols-config-importer/target");
                    //Paso 4.
                    is = Runtime.getRuntime().exec("java -jar ols-config-importer.jar",null,ruta).getInputStream();
                    imprimirLog(is);

                }catch(Exception e){

                    mensaje  = "Lo sentimos hubo un error en la eliminacion de la ontoligia si la aplicación no esta en funcionamiento por favor comunicate con nosotros";
                }
            }else{
                mensaje  = "Lo sentimos hubo un error en la eliminacion de la ontoligia si la aplicación no esta en funcionamiento por favor comunicate con nosotros";
            }
        }catch(Exception e){
             mensaje  = "Lo sentimos hubo un error en la eliminacion de la ontoligia si la aplicación no esta en funcionamiento por favor comunicate con nosotros";
        }
        model.addAttribute("message", mensaje);
        model.addAttribute("estado", "La aplicación no esta en funcionamiento, por favor espera ...");
        
        return "delete"; 
    }    

    
    
    public void imprimirLog(InputStream is){
        try{
            int i = is.read();
            FileWriter fw2 = new FileWriter("/opt/log.txt", true);
            BufferedWriter bw2 = new BufferedWriter(fw2);
            PrintWriter out2 = new PrintWriter(bw2);
            while(i > 0) {
                out2.write(i);
                i = is.read();
            }
            out2.close();
            bw2.close();
            fw2.close();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    private File getDownloadFile (String ontologyId) throws FileNotFoundException {
        File file = new File (getDownloadsFolder(), ontologyId.toLowerCase());
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        return file;
    }


    private String getDownloadsFolder ( ) {
        if (downloadsFolder.equals("")) {
            return OLSEnv.getOLSHome() + File.separator + "downloads";
        }
        return downloadsFolder;
    }

}
