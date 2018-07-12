/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
//import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.mpi.tla.util.Saxon;
import org.apache.commons.configuration.ConfigurationException;

import org.jdom2.JDOMException;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author vic
 */
public class Text implements RecipePlugin {
    protected int counter = 0;
    protected Boolean isFinished = false;
    protected Boolean userConfigRemoteError = false;
    
    protected String projectName;
    public URL serviceUrl;
    
    /**
     *
     * @param projectName
     * @param service
     * @throws JDOMException
     * @throws IOException
     * @throws SaxonApiException
     */
    @Override
    public void init(String projectName, Service service) throws JDOMException, IOException, SaxonApiException {
        System.out.print("init Text plugin");
        JSONObject json = this.parseSymantics(service.getServiceSymantics());
        this.projectName = projectName;
        this.serviceUrl = new URL((String)json.get("serviceLocation"));
        System.out.print("finish init Text plugin");

    }
    
    @Override
    public Boolean finished() {
        return isFinished;
    }
    
    @Override
    public String execute(String projectName, Logger logger) {
        logger.info("## Start plugin execution ##");
                
        JSONObject json = new JSONObject();
        json.put("key", projectName);
        json.put("status", 202);
        JSONObject userConfig = new JSONObject(); 
        try {
            userConfig = this.parseUserConfig(projectName);
            logger.info("## userConfig:  ##");
            System.out.println(userConfig.toJSONString());
            
            logger.info("## Running project ##");
            this.runProject(projectName);
            
            // keep polling project
            logger.info("## Polling the service ##");
            boolean ready = false;
            int i = 0;
            while (!ready) {
                logger.info(String.format("polling {%s}", i));
                i++;
                Thread.sleep(3000);
                
                // TODO: check if output file exists, if so, ready = true, else false
                ready = 1==1;
            }

            this.isFinished = true;
            
        } catch (IOException | InterruptedException ex ) {
            logger.info(String.format("## Execution ERROR: {%s}", ex.getLocalizedMessage()));
            Logger.getLogger(Text.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException | ConfigurationException | JDOMException ex) {
            Logger.getLogger(Text.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return json.toString();
    }
    
    /**
     *
     * @param key
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws org.json.simple.parser.ParseException
     * @throws org.apache.commons.configuration.ConfigurationException
     */
    @Override
    public JSONObject parseUserConfig(String key) throws ParseException, ConfigurationException {
        DeploymentLib dplib = new DeploymentLib();
        
        String workDir = dplib.getWd();
        String userConfFile = dplib.getConfFile();
        JSONParser parser = new JSONParser();
        
        try {
            String path = Paths.get(workDir, key, userConfFile).normalize().toString();
            JSONObject userConfig = (JSONObject) parser.parse(new FileReader(path)); 
            
            return userConfig;
        } catch (Exception ex) {
            System.out.println(ex.getLocalizedMessage());
        }
        JSONObject userConfig = new JSONObject();
        userConfig.put("parseuserconfig", "failed");
        return userConfig;
    }
    
    public JSONObject runProject(String key) throws IOException, MalformedURLException, MalformedURLException, JDOMException, ParseException {
        
        JSONObject json = new JSONObject();
        JSONParser parser = new JSONParser();
        String user, accessToken;
        
//        json = this.getAccessToken(key);
        user = (String)json.get("user");
        accessToken = (String)json.get("accessToken");
        
        /*
        set output template
        */
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("xml", "1");

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String,Object> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        byte[] postDataBytes = postData.toString().getBytes("UTF-8");
        /*
        end of set output template
        */
        
        URL url = new URL(
                this.serviceUrl.getProtocol(), 
                this.serviceUrl.getHost(), 
                this.serviceUrl.getPort(), 
                this.serviceUrl.getFile() + "/" + key + "/?user="+user+"&accesstoken="+accessToken, 
                null
        );
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("POST");
        httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpCon.setRequestProperty("Accept", "application/json");
        httpCon.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        httpCon.getOutputStream().write(postDataBytes);
        
//        json = (JSONObject) parser.parse(this.getUrlBody(httpCon));
        json.put("status", httpCon.getResponseCode());
        json.put("message", httpCon.getResponseMessage());
                
        httpCon.disconnect();
        return json;
        
    }
    
    /**
     *
     * @param key
     * @return
     */
    @Override
    public JSONObject getStatus(String pid) {
        // JSONObject status to return
        JSONObject status = new JSONObject();
        if (this.isFinished) {
            status.put("status", 200);
            status.put("message", "Task finished");
            status.put("finished", true);
        } else {
            status.put("status", 202);
            status.put("message", "Task running");
            status.put("finished", false);
        }
        return status;
    }   
    
    public static String readStringFromURL(URL requestURL) throws IOException {
        try (Scanner scanner = new Scanner(requestURL.openStream(),
                StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
    
    public JSONObject getOutputFiles(String projectName) throws MalformedURLException, IOException, JDOMException, SaxonApiException {
        JSONObject json = new JSONObject();
        
        URL url = new URL(
            this.serviceUrl.getProtocol(), 
            this.serviceUrl.getHost(), 
            this.serviceUrl.getPort(),
            this.serviceUrl.getFile() + "/" + projectName, 
            null
        );
                
        String urlString = url.toString();

        Map<String,String> NS = new LinkedHashMap<>();
        NS.put("xlink", "http://www.w3.org/1999/xlink");
        XdmNode doc = Saxon.buildDocument(new StreamSource(urlString));
        for (XdmItem file:Saxon.xpath(doc, "/clam/output/file")) {
            String href = Saxon.xpath2string(file, "@xlink:href", null, NS);
            String name = Saxon.xpath2string(file, "name");
            json.put(name, href);
        }
        
        return json;
    
    }
    
    @Override
    public JSONObject parseSymantics(String symantics) throws JDOMException, SaxonApiException {
        JSONObject json = new JSONObject();
        JSONObject parametersJson = new JSONObject();

        Map<String,String> NS = new LinkedHashMap<>();
        NS.put("cmd", "http://www.clarin.eu/cmd/1");
        NS.put("cmdp", "http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795");
        
        StringReader reader = new StringReader(symantics);
        XdmNode service = Saxon.buildDocument(new StreamSource(reader));

        String serviceName = Saxon.xpath2string(service, "cmdp:Name",null,NS);
        String serviceDescription = Saxon.xpath2string(service, "//cmdp:Service/cmdp:Description",null,NS);
        String serviceLocation = Saxon.xpath2string(service, "//cmdp:ServiceDescriptionLocation/cmdp:Location",null,NS);
                
        int counter = 0;
        for (XdmItem param:Saxon.xpath(service,"//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:Parameter",null,NS)) {
            
            JSONObject parameterJson = new JSONObject();
            String parameterName = Saxon.xpath2string(param, "cmdp:Name",null,NS);
            String parameterDescription = Saxon.xpath2string(param, "cmdp:Description",null,NS);
            String parameterType = Saxon.xpath2string(param, "cmdp:MIMEType",null,NS);
            
            parameterJson.put("parameterName", parameterName);
            parameterJson.put("parameterDescription", parameterDescription);
            parameterJson.put("parameterType", parameterType);
            
            int valueCounter = 0;
            for (XdmItem paramValue:Saxon.xpath(service,"//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:Parameter/cmdp:ParameterValue",null,NS)) {
                
                JSONObject parameterValueJson = new JSONObject();
                
                String parameterValueValue = Saxon.xpath2string(paramValue, "cmdp:Value",null,NS);
                String parameterValueDescription = Saxon.xpath2string(paramValue, "cmdp:Description",null,NS);
                
                parameterValueJson.put("parameterValueValue", parameterValueValue);
                parameterValueJson.put("parameterValueDescription", parameterValueDescription);
                
                parameterJson.put("value"+Integer.toString(valueCounter), parameterValueJson);
                
                valueCounter++;
            }
            
            parametersJson.put("parameter"+Integer.toString(counter), parameterJson);
            
            counter++;
        }

        json.put("serviceName", serviceName);
        json.put("serviceDescription", serviceDescription);
        json.put("serviceLocation", serviceLocation);
        json.put("counter", counter);
        json.put("parameters", parametersJson);
        return json;
        
    }
    
}
