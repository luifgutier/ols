package uk.ac.ebi.spot.ols.controller.ui;

/**
 * @author Simon Jupp
 * @date 08/07/2015
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import uk.ac.ebi.spot.ols.model.OntologyDocument;
import uk.ac.ebi.spot.ols.model.Status;
import uk.ac.ebi.spot.ols.service.OntologyRepositoryService;

import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.spot.ols.config.YamlConfigParser;

@Controller
@RequestMapping("")
public class HomeController {

    @Autowired
    OntologyRepositoryService repositoryService;

    @Autowired
    Environment environment;

    @ModelAttribute("all_ontologies")
    public List<OntologyDocument> getOntologies() {
        return repositoryService.getAllDocuments(new Sort(new Sort.Order(Sort.Direction.ASC, "ontologyId")));
    }

    @RequestMapping({"", "/"})
    public String goHome () {
        return "redirect:index";
    }
    //
    @RequestMapping({"/index"})
    public String showHome(Model model) {

        Date lastUpdated = repositoryService.getLastUpdated();
        int numberOfOntologies = repositoryService.getNumberOfOntologies();
        int numberOfTerms = repositoryService.getNumberOfTerms();
        int numberOfProperties = repositoryService.getNumberOfProperties();
        int numberOfIndividuals = repositoryService.getNumberOfIndividuals();

        SummaryInfo summaryInfo = new SummaryInfo(lastUpdated, numberOfOntologies, numberOfTerms, numberOfProperties, numberOfIndividuals, getClass().getPackage().getImplementationVersion());

        model.addAttribute("summary", summaryInfo);
        return "index";
    }

    @RequestMapping("/browse.do")
    public ModelAndView redirectOldUrls (
            @RequestParam(value = "ontName", required = true) String ontologyName,
            @RequestParam(value = "termId", required = false) String termId,
            Model model
    )  {

        ontologyName = ontologyName.toLowerCase();
        String url = "ontologies/" + ontologyName;
        if (termId != null) {
            url = "ontologies/" + ontologyName + "/terms?obo_id=" + termId;
        }
        RedirectView rv = new RedirectView(url);
        rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        rv.setUrl(url);
        return new ModelAndView(rv);
    }

    @RequestMapping("/search")
    public String doSearch(
            @RequestParam(value = "q", defaultValue = "*") String query,
            @RequestParam(value = "ontology", required = false) Collection<String> ontologies,
            @RequestParam(value = "type", required = false) Collection<String> types,
            @RequestParam(value= "slim", required = false) Collection<String> slims,
            @RequestParam(value = "queryFields", required = false) Collection<String> queryFields,
            @RequestParam(value = "exact", required = false) boolean exact,
            @RequestParam(value = "groupField", required = false) String groupField,
            @RequestParam(value = "obsoletes", defaultValue = "false") boolean queryObsoletes,
            @RequestParam(value = "local", defaultValue = "false") boolean isLocal,
            @RequestParam(value = "childrenOf", required = false) Collection<String> childrenOf,
            @RequestParam(value = "rows", defaultValue = "10") Integer rows,
            @RequestParam(value = "start", defaultValue = "0") Integer start,
            Model model

    ) {

        AdvancedSearchOptions searchOptions = new AdvancedSearchOptions(
                query,
                queryObsoletes,
                exact,
                isLocal,
                rows,
                start
        );

        if (ontologies != null) {
            searchOptions.setOntologies(ontologies);
        }

        if (queryFields != null) {
            searchOptions.setQueryField(queryFields);
        }

        if (types != null) {
            searchOptions.setTypes(types);
        }

        if (slims != null) {
            searchOptions.setSlims(slims);
        }

        if (groupField != null) {
            searchOptions.setGroupField(groupField);
        }


        model.addAttribute("searchOptions", searchOptions);
        return "search";
    }


    @RequestMapping({"contact"})
    public String showContact() {
        return "contact";
    }

    @RequestMapping({"sparql"})
    public String showSparql() {
        return "comingsoon";
    }
    @RequestMapping({"about"})
    public String showAbout() {
        return "redirect:docs/about";
    }

    @RequestMapping({"docs"})
    public String showDocsIndex(Model model) {
        return "redirect:docs/index";
    }
    // ok, this is bad, need to find a way to deal with trailing slashes and constructing relative URLs in the thymeleaf template...
    @RequestMapping({"docs/"})
    public String showDocsIndex2(Model model) {
        return "redirect:index";
    }
    @RequestMapping({"docs/{page}"})
    public String showDocs(@PathVariable("page") String pageName, Model model) {
        model.addAttribute("page", pageName);
        return "docs-template";
    }

    @RequestMapping({"/ontologiesi"})
    public String showOntologies() {

        return "ontologies";
    }
    
    @RequestMapping({"/uploadfile"})
    public String uploadFile(Model model) {
        Date lastUpdated = repositoryService.getLastUpdated();
        int numberOfOntologies = repositoryService.getNumberOfOntologies();
        int numberOfTerms = repositoryService.getNumberOfTerms();
        int numberOfProperties = repositoryService.getNumberOfProperties();
        int numberOfIndividuals = repositoryService.getNumberOfIndividuals();

        SummaryInfo summaryInfo = new SummaryInfo(lastUpdated, numberOfOntologies, numberOfTerms, numberOfProperties, numberOfIndividuals, getClass().getPackage().getImplementationVersion());

        model.addAttribute("summary", summaryInfo);
        return "uploadfile";
    }
    
    @RequestMapping({"/doupload"})
    public String doUploadFile(
            @RequestParam("uploadfile") MultipartFile uploadfile,
            Model model
    ) {
        //cargo los datos necesarios para mostrar el resumen sobre ontologias, terminos etc
        Date lastUpdated = repositoryService.getLastUpdated();
        int numberOfOntologies = repositoryService.getNumberOfOntologies();
        int numberOfTerms = repositoryService.getNumberOfTerms();
        int numberOfProperties = repositoryService.getNumberOfProperties();
        int numberOfIndividuals = repositoryService.getNumberOfIndividuals();
        Boolean continuar = false;
        //guardo los datos en el objeto de tipo SummaryInfo
        SummaryInfo summaryInfo = new SummaryInfo(lastUpdated, numberOfOntologies, numberOfTerms, numberOfProperties, numberOfIndividuals, getClass().getPackage().getImplementationVersion());
        
        //envio el objeto a la vista
        model.addAttribute("summary", summaryInfo);
        
        String s = null;
        String mensaje  = "Ontologia registrada con exito, en estos momentos la aplicación estara fuera de funcionamiento mientras se realiza la carga de la ontoligia, por favor espera un momento";
        
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
        
        //cargo el archivo donde debo concatenar el contenido del archivo que ha sido subido a la aplicación
        
        try(
            FileWriter fw = new FileWriter("/opt/OLS/ols-apps/ols-config-importer/src/main/resources/ols-config.yaml", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            //FileInputStream ontologiasRegistradas = new FileInputStream(new File("/opt/OLS/ols-apps/ols-config-importer/src/main/resources/ols-config.yaml"));
            
                out.println("");
                out.println("");
                InputStream in = uploadfile.getInputStream();
                int c = in.read();
                while(c > 0){
                    out.write(c);
                    c = in.read();
                }
                //cierro el archivo en el cual escribí el contenido nuevo
                out.close();
                bw.close();
                fw.close();

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
                   
            //en caso de error envio el mensaje de error a la vista
            //mensaje = e.getMessage()+" "+e.getLocalizedMessage()+ "  " +e.getStackTrace().toString() +" "+e.getCause();
            mensaje  ="Lo sentimos hubo un error en la carga de la ontoligia si la aplicación no esta en funcionamiento por favor comunicate con nosotros";
        }
        model.addAttribute("message", mensaje);
        model.addAttribute("estado", "La aplicación no esta en funcionamiento, por favor espera ...");
        return "doupload";
    }
    
    @RequestMapping({"/terminarAplicacion"})
    public @ResponseBody Boolean terminarAplicacion() {
        try{
            File ruta = new File("/opt/tomcat/bin");
            InputStream is = Runtime.getRuntime().exec("/bin/bash shutdown.sh",null,ruta).getInputStream();
            imprimirLog(is);
            PrintWriter writer = new PrintWriter("/opt/log2.txt", "UTF-8");
            writer.println("actualizar");
            writer.close();
            
        }catch(Exception e){
            
           return  false;
        }
        
        return true;
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
    
    private class SummaryInfo {
        Date lastUpdated;
        int numberOfOntologies;
        int numberOfTerms;
        int numberOfProperties;
        int numberOfIndividuals;
        String softwareVersion;

        public SummaryInfo(Date lastUpdated, int numberOfOntologies, int numberOfTerms, int numberOfProperties, int numberOfIndividuals, String softwareVersion) {
            this.lastUpdated = lastUpdated;
            this.numberOfOntologies = numberOfOntologies;
            this.numberOfTerms = numberOfTerms;
            this.numberOfProperties = numberOfProperties;
            this.numberOfIndividuals = numberOfIndividuals;
            this.softwareVersion = softwareVersion;
        }

        public Date getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(Date lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public int getNumberOfOntologies() {
            return numberOfOntologies;
        }

        public void setNumberOfOntologies(int numberOfOntologies) {
            this.numberOfOntologies = numberOfOntologies;
        }

        public int getNumberOfTerms() {
            return numberOfTerms;
        }

        public void setNumberOfTerms(int numberOfTerms) {
            this.numberOfTerms = numberOfTerms;
        }

        public int getNumberOfProperties() {
            return numberOfProperties;
        }

        public void setNumberOfProperties(int numberOfProperties) {
            this.numberOfProperties = numberOfProperties;
        }

        public int getNumberOfIndividuals() {
            return numberOfIndividuals;
        }

        public void setNumberOfIndividuals(int numberOfIndividuals) {
            this.numberOfIndividuals = numberOfIndividuals;
        }

        public String getSoftwareVersion() {
            return softwareVersion;
        }

        public void setSoftwareVersion(String softwareVersion) {
            this.softwareVersion = softwareVersion;
        }
    }


}
