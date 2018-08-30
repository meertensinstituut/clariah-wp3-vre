/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
 
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
/**
 *
 * @author Vic
 */
public class FoliaEditor implements RecipePlugin {
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
        System.out.print("init Folia Editor plugin");
        JSONObject json = this.parseSymantics(service.getServiceSymantics());
        this.projectName = projectName;
        this.serviceUrl = new URL((String)json.get("serviceLocation"));;
        System.out.print("finish init Folia Editor plugin");

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
            Logger.getLogger(FoliaEditor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(FoliaEditor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConfigurationException ex) {
            Logger.getLogger(FoliaEditor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JDOMException ex) {
            Logger.getLogger(FoliaEditor.class.getName()).log(Level.SEVERE, null, ex);
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
        userConfig.put("parse user config", "failed");
        return userConfig;
    }
    
    public static void convertXmlToHtml(Source xml, Source xslt, File file) {
        StringWriter sw = new StringWriter();

        try {

            FileWriter fw = new FileWriter(file.getPath());
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer trasform = tFactory.newTransformer(xslt);
            trasform.transform(xml, new StreamResult(sw));
            fw.write(sw.toString());
            fw.close();

            System.out.println("### Generated successfully! ###");

        } catch (IOException | TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerFactoryConfigurationError e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }	
    }
    
    public JSONObject runProject(String key) throws IOException, MalformedURLException, MalformedURLException, JDOMException, ParseException, ConfigurationException {
        final String outputPathConst = "output";
        final String inputPathConst = "input";
        
        JSONObject json = new JSONObject();
        DeploymentLib dplib = new DeploymentLib();
        
        String workDir = dplib.getWd();
//        String userConfFile = dplib.getConfFile();
        JSONObject userConfig = this.parseUserConfig(key);
        JSONArray params = (JSONArray) userConfig.get("params");
        
        JSONObject inputOjbect = (JSONObject) params.get(0);
        String inputFile = (String) inputOjbect.get("value");
        String fullInputPath = Paths.get(workDir, projectName, inputPathConst, inputFile).normalize().toString();
        String inputPath = Paths.get(workDir, projectName, inputPathConst).normalize().toString();
        System.out.println(String.format("### inputPath: %s ###", inputPath));
        System.out.println(String.format("### Full Input Path: %s ###", fullInputPath));
        
        Source xml = new StreamSource(new File(fullInputPath));
        
        URL url = new URL("https://raw.githubusercontent.com/proycon/folia/master/foliatools/folia2html.xsl");
//        Source xslt = new StreamSource(new File(Paths.get(inputPath, "folia2html.xsl").normalize().toString()));
        Source xslt = new StreamSource(url.openStream());
        
        JSONObject outputOjbect;
        String outputFile;
        if (params.size() > 1) {
            outputOjbect = (JSONObject) params.get(1);
            outputFile = (String) outputOjbect.get("value");
        } else {
            outputFile = inputFile;
        }
        
//        JSONObject outputOjbect = (JSONObject) params.get(1);
//        String outputFile = (String) outputOjbect.get("value");
        String outputPath = Paths.get(workDir, projectName, outputPathConst).normalize().toString();
        String fullOutputPath = Paths.get(workDir, projectName, outputPathConst, outputFile).normalize().toString();
        System.out.println(String.format("### outputPath: %s ###", outputPath));
        System.out.println(String.format("### Full outputPath: %s ###", fullOutputPath));
        
        File outputPathAsFile = new File(Paths.get(fullOutputPath).getParent().normalize().toString());
        if (! outputPathAsFile.exists()) {
            System.out.println(String.format("### Creating folder: %s ###", outputPathAsFile.toString()));
            outputPathAsFile.mkdirs();
        }
        
        File file = new File(fullOutputPath);        
        convertXmlToHtml(xml, xslt, file);
                        
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
        System.out.println(String.format("### symantics in parseSymantics before parsing: %s ###", symantics));
        JSONObject json = new JSONObject();
        JSONObject parametersJson = new JSONObject();

        Map<String,String> NS = new LinkedHashMap<>();
        NS.put("cmd", "http://www.clarin.eu/cmd/1");
        NS.put("cmdp", "http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1527668176011");
        
        StringReader reader = new StringReader(symantics);
        XdmNode service = Saxon.buildDocument(new StreamSource(reader));

        String serviceName = Saxon.xpath2string(service, "//cmdp:Service/cmdp:Name",null,NS);
        String serviceDescription = Saxon.xpath2string(service, "//cmdp:Service/cmdp:Description",null,NS);
        String serviceLocation = Saxon.xpath2string(service, "//cmdp:ServiceDescriptionLocation/cmdp:Location",null,NS);
                
        String inputName = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:Name",null,NS);
        String inputLabel = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:Label",null,NS);
        String inputType = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:MIMEType",null,NS);
        String inputCardinalityMin = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:MinimumCardinality",null,NS);
        String inputCardinalityMax = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:MaximumCardinality",null,NS);        

        String outputName = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Output/cmdp:Parameter/cmdp:Name",null,NS);
//        String outputLabel = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Output/cmdp:Parameter/cmdp:Label",null,NS);
        String outputType = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Output/cmdp:Parameter/cmdp:MIMEType",null,NS);
        String outputCardinalityMin = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Output/cmdp:Parameter/cmdp:MinimumCardinality",null,NS);
        String outputCardinalityMax = Saxon.xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Output/cmdp:Parameter/cmdp:MaximumCardinality",null,NS);        

        json.put("serviceName", serviceName);
        json.put("serviceDescription", serviceDescription);
        json.put("serviceLocation", serviceLocation);
        
        json.put("inputName", inputName);
        json.put("inputLabel", inputLabel);
        json.put("inputType", inputType);
        json.put("inputCardinalityMin", inputCardinalityMin);
        json.put("inputCardinalityMax", inputCardinalityMax);

        json.put("outputName", outputName);
//        json.put("outputLabel", outputLabel);
        json.put("outputType", outputType);
        json.put("outputCardinalityMin", outputCardinalityMin);
        json.put("outputCardinalityMax", outputCardinalityMax);
        
        return json;
        
    }
        
    public JSONObject uploadFile(String projectName, String filename, String language, String inputTemplate, String author) throws MalformedURLException, IOException, JDOMException, FileNotFoundException, ParseException, ParseException, ConfigurationException {
        JSONObject jsonResult = new JSONObject();
        JSONObject json = new JSONObject();

        String charset = "UTF-8";
        String param = "value";
        File textFile = new File("/path/to/file.txt");
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        DeploymentLib dplib = new DeploymentLib();
        
        String path = Paths.get(dplib.getWd(), projectName, dplib.getInputDir(), filename).normalize().toString();
        System.out.println(path);
        
        jsonResult.put("pathUploadFile", path);
        File file = new File(path);
        String filenameOnly = file.getName();
        jsonResult.put("filenameOnly", filenameOnly);
        
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        String payload = new String(encoded, "UTF-8");

        URL url = new URL(
                this.serviceUrl.getProtocol(), 
                this.serviceUrl.getHost(), 
                this.serviceUrl.getPort(),
                this.serviceUrl.getFile() + "/pub/upload", 
                null
        );

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestMethod("POST");
            
        try (
            OutputStream output = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
        ) {
            // Send text file.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; file=\"" + textFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
            writer.append(CRLF).flush();
            Files.copy(textFile.toPath(), output);
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.            
            writer.append("--" + boundary + "--").append(CRLF).flush();
            
            int responseCode = ((HttpURLConnection) connection).getResponseCode();
            System.out.println(responseCode); // Should be 200

            connection.disconnect();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return jsonResult;
    }
}
