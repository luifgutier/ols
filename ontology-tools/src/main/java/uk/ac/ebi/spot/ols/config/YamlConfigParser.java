package uk.ac.ebi.spot.ols.config;

import java.io.File;
import java.io.FileInputStream;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;
import uk.ac.ebi.spot.ols.loader.DocumentLoadingService;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Simon Jupp
 * @date 09/07/2015
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class YamlConfigParser {

    private Resource yamlFile;
    private boolean isObo = false;

    public YamlConfigParser(Resource yamlFile)  {

        this(yamlFile, false);

    }

    public YamlConfigParser(Resource yamlFile, boolean isObo)  {
        this.yamlFile = yamlFile;
        this.isObo = isObo;
    }

    public Collection<YamlBasedLoadingService> getDocumentLoadingServices() throws IOException {

        Yaml yaml = new Yaml();

        LinkedHashMap linkedHashMap = (LinkedHashMap)yaml.load(yamlFile.getInputStream());

        Collection<YamlBasedLoadingService> documentLoadingServices = new ArrayList<YamlBasedLoadingService>();

        LinkedHashMap contextInfos = (LinkedHashMap)linkedHashMap.get("@context");
        //Get the @base property which will be the first part of the url to the owl file of the ontology.
        String base = null;
        if (contextInfos != null) {
            base = (String)contextInfos.get("@base");
        }



        ArrayList<LinkedHashMap> ontologies = (ArrayList<LinkedHashMap>)linkedHashMap.get("ontologies");

        for (LinkedHashMap ontology : ontologies) {

            boolean _isObo = isObo;
            if ( ontology.containsKey("is_foundry")) {
                _isObo = ((Boolean)ontology.get("is_foundry"));
            }

            boolean obsolete = false;
            if ( ontology.containsKey("is_obsolete")) {
                obsolete = ((Boolean)ontology.get("is_obsolete"));
            }

            if  (base == null && _isObo) {
                base = "http://purl.obolibrary.org/obo/";
            }

            if (!obsolete) {
                YamlBasedLoadingService yamlBasedLoadingService = new YamlBasedLoadingService(ontology, base, _isObo);
                documentLoadingServices.add(yamlBasedLoadingService);
            }

        }


        return documentLoadingServices;


    }
    
    public Boolean doDeleteOntology(String id,FileInputStream file) throws IOException {
        Boolean retorno = true;
        Yaml yaml = new Yaml();
        LinkedHashMap linkedHashMap = (LinkedHashMap)yaml.load(file);
        Collection<LinkedHashMap> documentLoadingServices = new ArrayList<LinkedHashMap>();
        LinkedHashMap contextInfos = (LinkedHashMap)linkedHashMap.get("@context");        
        ArrayList<LinkedHashMap> ontologies = (ArrayList<LinkedHashMap>)linkedHashMap.get("ontologies");        
        try{      
            PrintWriter yml = new PrintWriter("/opt/OLS/ols-apps/ols-config-importer/src/main/resources/ols-config.yaml", "UTF-8");
            for (LinkedHashMap ontology : ontologies) {
                if(!ontology.get("id").equals(id)){
                    documentLoadingServices.add(ontology);
                }
            }            
            yml.println("\"@context\":");
            yml.println("");
            yml.println("");
            yml.println("");
            yml.println("ontologies:");
            yml.println("");
            yml.println("");
            for (LinkedHashMap ontology : documentLoadingServices) {
                yml.println("## "+ontology.get("id").toString().toUpperCase());
                yml.println("");
                yml.println("");
                List<String> keys = new ArrayList<>(ontology.keySet());
                for(String key : keys){
                    if(key.equals("id")){
                        yml.println("  - "+key+": "+ontology.get(key));
                    }else{
                        if(ontology.get(key).getClass().equals(ArrayList.class)){
                            List<String> items = (ArrayList)ontology.get(key);
                            yml.println("    "+key+":");
                            for (String item : items) {
                                yml.println("      - "+item);
                            }
                        }else{
                            yml.println("    "+key+": "+ontology.get(key));
                        }   
                    }   
                }
                yml.println("");
                yml.println("");
            }
            yml.close();   
        }catch(Exception e){
           retorno = false;
        }
        return retorno;
    }
    
    public Boolean searchRepeatOntology(InputStream uploadedFile,FileInputStream file) throws IOException {
        Boolean retorno = false;
        Yaml yaml = new Yaml();
        LinkedHashMap linkedHashMap = (LinkedHashMap)yaml.load(file);
        ArrayList subida = (ArrayList)yaml.load(uploadedFile);        
        ArrayList<LinkedHashMap> ontologies = (ArrayList<LinkedHashMap>)linkedHashMap.get("ontologies");        
        try{      
            ArrayList<String> ids = new ArrayList<>();
            for (LinkedHashMap onto : (ArrayList<LinkedHashMap>)subida) {   
                ids.add((String)onto.get("id"));
            }  
            for (LinkedHashMap ontology : ontologies) {
                if(ids.contains((String)ontology.get("id"))){
                    retorno = true;
                }
            }
        }catch(Exception e){
           retorno = true;
        }
        return retorno;
    }

}
