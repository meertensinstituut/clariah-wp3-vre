/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Vic
 */
public class DeploymentLib {

    final private String defaultConfiPath = "/conf/conf.xml";
    String fullPath;
    File config;
    String userConfFile;
    String inputDirectory;
    String outputDirectory;
    
    public DeploymentLib() throws ConfigurationException {
        
        this.config = new File(this.defaultConfiPath);
    }
    
    public File getConfigFile() {
        return this.config;
    }
    
//    public Boolean checkService(String serviceId) {
//        // TODO: should check the serviceId with remote database
//        // now set default to UCTO for testing purpose
//        // NOTE: service id is ucto but recipe should be the full class uri 
//        // for example nl.knaw.meertens.deployment.lib.Clam
//        // The class name is case sensitive
//        
//        String remoteServiceId = "UCTO";
//
//        return remoteServiceId.toLowerCase().equals(serviceId.toLowerCase());
//    }
        
    public Service getServiceByName(String serviceName) throws MalformedURLException, IOException, ParseException, ConfigurationException {
        // valid service, fetch data from db and return
        JSONObject json = new JSONObject();
        JSONParser parser = new JSONParser();
        String dbSessionToken = System.getenv("SERVICES_TOKEN");
        String dbApiKey = System.getenv("APP_KEY_SERVICES");
        DeploymentLib dplib = new DeploymentLib();
        
        String urlString ="http://dreamfactory:80/api/v2/services/_table/service/?filter=name%3D" + serviceName;
        URL url = new URL(urlString);
        
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(false);
        httpCon.setRequestMethod("GET");
        httpCon.setRequestProperty("Content-Type", "application/json");
        httpCon.setRequestProperty("accept", "application/json");
        httpCon.setRequestProperty("X-DreamFactory-Api-Key", dbApiKey);

        String rawString = dplib.getUrlBody(httpCon);
        httpCon.disconnect();
        
        try {
            json = (JSONObject) parser.parse(rawString);
        } catch (ParseException ex) {
            System.err.println(ex);
            return null;
        }
        
        JSONArray resource = (JSONArray) json.get("resource");
        if (resource instanceof JSONArray) {
            if (resource.size()>0) {
                JSONObject record = (JSONObject) resource.get(0);
                String serviceRecipe = (String)record.get("recipe");
                String serviceSemantics = (String)record.get("semantics");
                String serviceId = (String)record.get("id");

                return new Service(serviceId, serviceName, serviceRecipe, serviceSemantics, "", true);
            }
        }
        
        return null;
        
    }
    
    public Boolean serviceExists(String serviceName) throws ConfigurationException, IOException, MalformedURLException, ParseException {
        Service service = this.getServiceByName(serviceName);
        return service instanceof Service;
    }
    
    public void parseConfig(File config) throws ConfigurationException {
        XMLConfiguration xml = new XMLConfiguration(config);
        this.fullPath = xml.getString("workingFolder");
        this.userConfFile = xml.getString("userConfFile");
        this.inputDirectory = xml.getString("inputDirectory");
        this.outputDirectory = xml.getString("outputDirectory");
    }
    
    public void parseConfig(String config) throws ConfigurationException {
        File configFile = new File(config);
        this.parseConfig(configFile);
    }
    public String getWd() {
        return this.fullPath;
    }
    public String getConfFile() {
        return this.userConfFile;
    }
    
    public String getUrlBody(HttpURLConnection conn) throws IOException {

        // handle error response code it occurs
        int responseCode = conn.getResponseCode();
        InputStream inputStream;
        if (200 <= responseCode && responseCode <= 299) {
            inputStream = conn.getInputStream();
        } else {
            inputStream = conn.getErrorStream();
        }

        BufferedReader in = new BufferedReader(
            new InputStreamReader(
                inputStream));

        StringBuilder response = new StringBuilder();
        String currentLine;

        while ((currentLine = in.readLine()) != null) 
            response.append(currentLine);

        in.close();

        return response.toString();
    }
    
}
