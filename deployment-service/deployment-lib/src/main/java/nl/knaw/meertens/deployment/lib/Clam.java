/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.mpi.tla.util.Saxon;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import static org.hamcrest.core.IsEqual.equalTo;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import static org.junit.Assert.assertThat;
//import sun.util.logging.PlatformLogger;

/**
 *
 * @author vic
 */
public class Clam implements RecipePlugin {
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
        System.out.print("init CLAM plugin");
        JSONObject json = this.parseSymantics(service.getServiceSymantics());
        this.projectName = projectName;
        this.serviceUrl = new URL((String)json.get("serviceLocation"));
        System.out.print("finish init CLAM plugin");

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
            
            // Check user config against remote service record
            logger.info("## Checking user config against remote server ##");
            if (!this.checkUserConfigOnRemoteServer(this.getSymanticsFromRemote(), userConfig)) {
                logger.info("bad user config according to remote server!");
                this.userConfigRemoteError = true;
                json.put("status", 500);
                return json.toString();
            }
            
            logger.info("## Creating project ##");
            this.createProject(projectName);
            
            logger.info("## upload files ##");
            this.prepareProject(projectName);
            
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
                JSONObject projectStatus = this.getProjectStatus(projectName);        
                Long completionCode = (Long)projectStatus.get("completion");
                Long statusCode = (Long)projectStatus.get("statuscode");
                Boolean successCode = (Boolean)projectStatus.get("success");
                ready = (completionCode == 100L && statusCode == 2L && successCode);
            }
            
            logger.info("## Download result ##");
            this.downloadProject(projectName);
            
//            logger.info("## Removing project ##");
//            this.deleteProject(projectName);

            this.isFinished = true;
            
        } catch (IOException | ParseException | JDOMException | SaxonApiException | ConfigurationException | InterruptedException ex ) {
            logger.info(String.format("## Execution ERROR: {%s}", ex.getLocalizedMessage()));
            Logger.getLogger(Clam.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return json.toString();
    }
    
    @Override
    public JSONObject parseUserConfig(String key) throws FileNotFoundException, IOException, ParseException, ConfigurationException {
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
        
        json = this.getAccessToken(key);
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
    
    public JSONObject prepareProject(String key) throws IOException, MalformedURLException, JDOMException, FileNotFoundException, ParseException, ConfigurationException {
        JSONObject jsonResult = new JSONObject();
        JSONObject json = new JSONObject();
        json = parseUserConfig(key);
        
        JSONArray params = (JSONArray)json.get("params");

        for (Object param : params) {
            JSONObject objParam = new JSONObject();
            objParam = (JSONObject)param;
            String inputTemplate = (String)objParam.get("name");
            String type = (String)objParam.get("type");
            String value = (String)objParam.get("value");
            
            
            JSONArray innerParams = new JSONArray();
            innerParams = (JSONArray)objParam.get("params");
            
            String author = "";
            String language = "";
            
            for (Object r: innerParams) {
                JSONObject obj = (JSONObject) r;
                
                System.out.println(r);
                switch ((String)obj.get("name")) {
                    case "author":
                        author = (String)obj.get("value");
                        break;
                    case "language":
                        language = (String)obj.get("value");
                }
            }
            
//            if (innerParams.get(0) instanceof JSONObject) {
//                JSONObject innerParam = (JSONObject)innerParams.get(0);
//                author = (String)innerParam.get("author");
//                language = (String)innerParam.get("language");
//            } 
            
            if ("file".equals(type)) {
                jsonResult = this.uploadFile(key, value, language, inputTemplate, author);
                jsonResult.put("key", key);
                jsonResult.put("value", value);
                jsonResult.put("language", language);
                jsonResult.put("inputTemplate", inputTemplate);
                jsonResult.put("author", author);
            }
        }
        return jsonResult;
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
    
    /**
     *
     * @param key
     * @return
     * @throws IOException
     * @throws MalformedURLException
     * @throws JDOMException
     */
    public JSONObject getProjectStatus(String key) throws IOException, MalformedURLException, JDOMException {
        try {
            return this.pollProject(key);
        } catch (ParseException ex) {
            Logger.getLogger(Clam.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }   

    
//    public JSONObject loadConfigureFile(String wd) throws ConfigurationException {
//        JSONObject json = new JSONObject();
//        DeploymentLib dplib = new DeploymentLib();
//        String path = dplib.getWd();
//        String fullPath = path + wd;
//        
//        String defaultConfiPath = "";
//        return json;
//    }
    
    public JSONObject pollProject(String projectName) throws IOException, IOException, MalformedURLException, MalformedURLException, JDOMException, JDOMException, JDOMException, ParseException {
        JSONObject json = new JSONObject();
        JSONParser parser = new JSONParser();
        String user, accessToken;
        
        json = this.getAccessToken(projectName);
        user = (String)json.get("user");
        accessToken = (String)json.get("accessToken");
        
        URL url = new URL(
                this.serviceUrl.getProtocol(), 
                this.serviceUrl.getHost(), 
                this.serviceUrl.getPort(),
                this.serviceUrl.getFile() + "/" + projectName + "/status/?user="+user+"&accesstoken="+accessToken, 
                null
        );
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("GET");
        
        json = (JSONObject) parser.parse(this.getUrlBody(httpCon));
        
        Long completionCode = (Long)json.get("completion");
        Long statusCode = (Long)json.get("statuscode");
        Boolean successCode = (Boolean)json.get("success");
        
//        if (completionCode == 100L && statusCode == 2L && successCode) {
//            this.isFinished = true;
//        }
             
        json.put("status", httpCon.getResponseCode());
        json.put("message", httpCon.getResponseMessage());
        json.put("finished", this.finished());
        
        
        httpCon.disconnect();
        
//        new Thread() {
//            @Override
//            public void run() {
//                Queue queue = new Queue();
//                queue.cleanFinishedTask();
//            }
//        }.start(); 
        return json;
    }
    
    public JSONObject getAccessToken(String projectName) throws MalformedURLException, IOException, JDOMException {
        JSONObject json = new JSONObject();
        
        URL url = new URL(
                this.serviceUrl.getProtocol(), 
                this.serviceUrl.getHost(), 
                this.serviceUrl.getPort(),
                this.serviceUrl.getFile() + "/" + projectName, 
                null
        );
        String urlString = url.toString();
        String xmlString = readStringFromURL(url);
        
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(new StringReader(xmlString));
        Element rootNode = doc.getRootElement();
        String user = rootNode.getAttributeValue("user");
        String accessToken = rootNode.getAttributeValue("accesstoken");
        
        json.put("user", user);
        json.put("accessToken", accessToken);
        
        return json;
    }
        
//    public static String readStringFromURL(String requestURL) throws IOException {
//        try (Scanner scanner = new Scanner(new URL(requestURL).openStream(),
//                StandardCharsets.UTF_8.toString())) {
//            scanner.useDelimiter("\\A");
//            return scanner.hasNext() ? scanner.next() : "";
//        }
//    }
    
    public static String readStringFromURL(URL requestURL) throws IOException {
        try (Scanner scanner = new Scanner(requestURL.openStream(),
                StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
        
    public JSONObject uploadFile(String projectName, String filename, String language, String inputTemplate, String author) throws MalformedURLException, IOException, JDOMException, FileNotFoundException, ParseException, ParseException, ConfigurationException {
        JSONObject jsonResult = new JSONObject();
        JSONObject json = new JSONObject();
        json = this.getAccessToken(projectName);
        DeploymentLib dplib = new DeploymentLib();
        
        String path = Paths.get(dplib.getWd(), projectName, dplib.getInputDir(), filename).normalize().toString();
        System.out.println("### File path to be uploaded:" + path + " ###");
        
        jsonResult.put("pathUploadFile", path);
        File file = new File(path);
        String filenameOnly = file.getName();
        jsonResult.put("filenameOnly", filenameOnly);

        URL url = new URL(
                this.serviceUrl.getProtocol(), 
                this.serviceUrl.getHost(), 
                this.serviceUrl.getPort(),
                this.serviceUrl.getFile() + "/" +projectName + "/input/" + filenameOnly + "?inputtemplate="+inputTemplate+"&language="+language+"&documentid=&author="+author+"&filename="+filenameOnly, 
                null
        );
        System.out.println("### Upload URL:" + url.toString() + " ###");
            
        try {
            String boundary = Long.toHexString(System.currentTimeMillis());
            String LINE_FEED = "\r\n";
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
            
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + filenameOnly + "\"").append(LINE_FEED);
            writer.append("Content-Type: text/plain").append(LINE_FEED);
            writer.append(LINE_FEED);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                for (String line; (line = reader.readLine()) != null; ) {
                    writer.append(line).append(LINE_FEED);
                }
            } finally {
                if (reader != null) try {
                    reader.close();
                } catch (IOException logOrIgnore) {
                }
            }

            writer.append(LINE_FEED);
            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();
            writer.close();

            connection.disconnect();
            System.out.println("### File uplaoded! " + connection.getResponseCode() + connection.getResponseMessage() + " ###");

        } catch (Exception e) {
            System.out.println("### File upload failed ###");
            throw new RuntimeException(e.getMessage());
        }

        return jsonResult;
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
        
    public JSONObject downloadProject(String projectName) throws MalformedURLException, IOException, JDOMException, SaxonApiException, ConfigurationException {
        final String outputPathConst = "output";
        
        JSONObject jsonFiles = this.getOutputFiles(projectName);
        JSONObject json = new JSONObject();
        DeploymentLib dplib = new DeploymentLib();
        String workDir = dplib.getWd();
        String outputDir = dplib.getOutputDir();
        System.out.println(String.format("### current outputPath: %s ###", outputDir));
        
        String outputPath = Paths.get(workDir, projectName, outputPathConst).normalize().toString();
        System.out.println(String.format("### outputPath: %s ###", outputPath));
        String path = Paths.get(workDir, projectName, outputDir).normalize().toString();
        
        /* create output directory if not there */
        File theDir = new File(path);
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            try{
                theDir.mkdir();
            } 
            catch(SecurityException se){
                System.err.println(se.getMessage());
            }        
        }
        /* end create output directory */
        
        Set<String> keys = jsonFiles.keySet();
        json = jsonFiles;
        
        keys.forEach((key) -> {
            File file = new File(Paths.get(path, key).normalize().toString());
            System.out.println(Paths.get(path, key).normalize().toString());
            URL url;
            
            try {
                String urlString = (String)jsonFiles.get(key);
                urlString = urlString.replace("127.0.0.1", this.serviceUrl.getHost());
                System.out.println(urlString);
                url = new URL(urlString);
                FileUtils.copyURLToFile(url, file, 10000, 10000);
            } catch (MalformedURLException ex) {
                Logger.getLogger(Clam.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Clam.class.getName()).log(Level.SEVERE, null, ex);
            } 
            
        });
        return json;
    }
    
    public JSONObject createProject(String projectName) throws MalformedURLException, IOException, ConfigurationException {
        JSONObject json = new JSONObject();
                
        URL url = new URL(
                this.serviceUrl.getProtocol(), 
                this.serviceUrl.getHost(), 
                this.serviceUrl.getPort(),
                this.serviceUrl.getFile() + "/" + projectName, 
                null
        );

        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");
        OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
        out.write("Resource content");
        out.close();
        httpCon.getInputStream();
        json.put("status", httpCon.getResponseCode());
        json.put("message", httpCon.getResponseMessage());
        httpCon.disconnect();
        return json;
    }
    
    public JSONObject deleteProject(String projectName) throws MalformedURLException, IOException {
        JSONObject json = new JSONObject();
        URL url = new URL(
                this.serviceUrl.getProtocol(), 
                this.serviceUrl.getHost(), 
                this.serviceUrl.getPort(),
                this.serviceUrl.getFile() + "/" + projectName, 
                null
        );        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded" );
        httpCon.setRequestMethod("DELETE");
        json.put("status", httpCon.getResponseCode());
        json.put("message", httpCon.getResponseMessage());
        httpCon.disconnect();
        return json;
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
    
    @Override
    public JSONObject parseSymantics(String symantics) throws JDOMException, IOException, SaxonApiException {
        JSONObject json = new JSONObject();
        JSONObject parametersJson = new JSONObject();

        Map<String,String> NS = new LinkedHashMap<>();
        NS.put("cmd", "http://www.clarin.eu/cmd/1");
        NS.put("cmdp", "http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1527668176011");
        
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

    public JSONObject getSymanticsFromRemote() {
        JSONObject json = new JSONObject();

        return json;
        
    }
        
    private Boolean checkUserConfigOnRemoteServer(JSONObject remoteSymantics, JSONObject userSymantics) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return true;
    }
    
}
