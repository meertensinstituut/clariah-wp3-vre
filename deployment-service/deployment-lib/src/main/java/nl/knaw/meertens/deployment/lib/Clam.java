/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.knaw.meertens.deployment.lib;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.mpi.tla.util.Saxon;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
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
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

//import java.util.Iterator;
//import sun.util.logging.PlatformLogger;

/**
 * @author vic
 */
public class Clam implements RecipePlugin {
  public URL serviceUrl;
  protected int counter = 0;
  protected Boolean isFinished = false;
  protected Boolean userConfigRemoteError = false;
  protected String projectName;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public static String readStringFromUrl(URL requestUrl) throws IOException {
    try (Scanner scanner = new Scanner(requestUrl.openStream(),
      StandardCharsets.UTF_8.toString())) {
      scanner.useDelimiter("\\A");
      return scanner.hasNext() ? scanner.next() : "";
    }
  }

  /**
   * Initiate the current plugin
   *
   * @param projectName
   * The 'project name' in the sense that Clam requires a project name to work with.
   *
   * @param service
   * The recepie record in the registry known as service
   * @throws IOException
   * IOException
   * @throws SaxonApiException
   * XML related exception
   */
  @Override
  public void init(String projectName, Service service) throws RecipePluginException {
    logger.info("init CLAM plugin");
    JSONObject json = null;
    try {
      json = new DeploymentLib().parseSymantics(service.getServiceSymantics());
    } catch (ConfigurationException e) {
      e.printStackTrace();
    }
    this.projectName = projectName;
    try {
      this.serviceUrl = new URL((String) json.get("serviceLocation"));
    } catch (MalformedURLException e) {
      throw new RecipePluginException("service url is invalid", e);
    }
    logger.info("finish init CLAM plugin");

  }

  @Override
  public JSONObject execute() {
    logger.info("## Start plugin execution ##");

    JSONObject json = new JSONObject();
    json.put("key", projectName);
    json.put("status", 202);
    JSONObject userConfig = new JSONObject();
    try {
      userConfig = new DeploymentLib().parseUserConfig(projectName);

      // Check user config against remote service record
      logger.info("## Checking user config against remote server ##");
      if (!this.checkUserConfigOnRemoteServer(this.getSymanticsFromRemote(), userConfig)) {
        logger.info("bad user config according to remote server!");
        this.userConfigRemoteError = true;
        json.put("status", 500);
        return json;
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
      int looper = 0;
      while (!ready) {
        logger.info(String.format("polling {%s}", looper));
        looper++;
        Thread.sleep(3000);
        JSONObject projectStatus = this.getProjectStatus(projectName);
        Long completionCode = (Long) projectStatus.get("completion");
        Long statusCode = (Long) projectStatus.get("statuscode");
        Boolean successCode = (Boolean) projectStatus.get("success");
        ready = (completionCode == 100L && statusCode == 2L && successCode);
      }

      logger.info("## Download result ##");
      this.downloadProject(projectName);

      this.isFinished = true;

    } catch (IOException | InterruptedException | ConfigurationException |
      SaxonApiException | JDOMException ex) {
      logger.error(String.format("## Execution ERROR: {%s}", ex.getLocalizedMessage()), ex);
    }

    return json;
  }
  
  public JSONObject runProject(String key) throws IOException, JDOMException {

    JSONObject json = new JSONObject();
    JSONParser parser = new JSONParser();
    String user;
    String accessToken;

    json = this.getAccessToken(key);
    user = (String) json.get("user");
    accessToken = (String) json.get("accessToken");

    /*
    set output template
    */
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("xml", "1");

    StringBuilder postData = new StringBuilder();
    for (Map.Entry<String, Object> param : params.entrySet()) {
      if (postData.length() != 0) {
        postData.append('&');
      }
      postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
      postData.append('=');
      postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
    }
    byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
    // /*
    // end of set output template
    // */

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" + key + "/?user=" + user + "&accesstoken=" + accessToken,
      null
    );
    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();

    httpCon.setDoOutput(true);
    httpCon.setRequestMethod("POST");
    httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    httpCon.setRequestProperty("Accept", "application/json");
    httpCon.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
    httpCon.getOutputStream().write(postDataBytes);

    // json = (JSONObject) parser.parse(this.getUrlBody(httpCon));
    json.put("status", httpCon.getResponseCode());
    json.put("message", httpCon.getResponseMessage());

    httpCon.disconnect();
    return json;

  }

  public JSONObject prepareProject(String key)
      throws IOException, JDOMException, ConfigurationException {
    JSONObject jsonResult = new JSONObject();
    JSONObject json = new JSONObject();
    json = new DeploymentLib().parseUserConfig(key);

    JSONArray params = (JSONArray) json.get("params");

    for (Object param : params) {
      JSONObject objParam = new JSONObject();
      objParam = (JSONObject) param;
      String inputTemplate = (String) objParam.get("name");
      String type = (String) objParam.get("type");
      String value = (String) objParam.get("value");


      JSONArray innerParams = new JSONArray();
      innerParams = (JSONArray) objParam.get("params");

      String author = "";
      String language = "";

      for (Object r : innerParams) {
        JSONObject obj = (JSONObject) r;

        logger.info(r.toString());
        switch ((String) obj.get("name")) {
          case "author":
            author = (String) obj.get("value");
            break;
          case "language":
            language = (String) obj.get("value");
            break;
          default:
            author = (String) obj.get("value");
        }
      }

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
   * Description.
   *
   * <p>project id also known as project name, key and working directory
   * @return status
   *
   */
  @Override
  public JSONObject getStatus() {
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
   * @param key
   * The project name, known as well as the folder
   * @throws IOException
   * IOException
   * @throws MalformedURLException
   * URL Exception
   * @throws JDOMException
   * JDOM Exception
   */
  public JSONObject getProjectStatus(String key) throws IOException, MalformedURLException, JDOMException {
    try {
      return this.pollProject(key);
    } catch (ParseException ex) {
      logger.error("could not parse result while polling project", ex);
    }
    return null;
  }

  public JSONObject pollProject(String projectName) throws IOException, JDOMException, ParseException {
    JSONObject json = new JSONObject();
    String user;
    String accessToken;

    json = this.getAccessToken(projectName);
    user = (String) json.get("user");
    accessToken = (String) json.get("accessToken");

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" + projectName + "/status/?user=" + user + "&accesstoken=" + accessToken,
      null
    );
    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    httpCon.setDoOutput(true);
    httpCon.setRequestMethod("GET");

    JSONParser parser = new JSONParser();
    json = (JSONObject) parser.parse(this.getUrlBody(httpCon));

    Long completionCode = (Long) json.get("completion");
    Long statusCode = (Long) json.get("statuscode");
    Boolean successCode = (Boolean) json.get("success");

    json.put("status", httpCon.getResponseCode());
    json.put("message", httpCon.getResponseMessage());
    json.put("finished", this.isFinished);
    
    httpCon.disconnect();

    return json;
  }

  public JSONObject getAccessToken(String projectName) throws IOException, JDOMException {
    JSONObject json = new JSONObject();

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" + projectName,
      null
    );
    String urlString = url.toString();
    String xmlString = readStringFromUrl(url);

    SAXBuilder saxBuilder = new SAXBuilder();
    Document doc = saxBuilder.build(new StringReader(xmlString));
    Element rootNode = doc.getRootElement();
    String user = rootNode.getAttributeValue("user");
    String accessToken = rootNode.getAttributeValue("accesstoken");

    json.put("user", user);
    json.put("accessToken", accessToken);

    return json;
  }

  public JSONObject uploadFile(String projectName, String filename, String language, String inputTemplate,
                               String author)
      throws IOException, JDOMException, ConfigurationException {
    JSONObject jsonResult = new JSONObject();
    JSONObject json = new JSONObject();
    json = this.getAccessToken(projectName);
    DeploymentLib dplib = new DeploymentLib();

    String path = Paths.get(dplib.getWd(), projectName, dplib.getInputDir(), filename).normalize().toString();
    logger.info("### File path to be uploaded:" + path + " ###");

    jsonResult.put("pathUploadFile", path);
    File file = new File(path);
    String filenameOnly = file.getName();
    jsonResult.put("filenameOnly", filenameOnly);

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" + projectName + "/input/" + filenameOnly + "?inputtemplate=" + inputTemplate +
        "&language=" + language + "&documentid=&author=" + author + "&filename=" + filenameOnly,
      null
    );
    logger.info("### Upload URL:" + url.toString() + " ###");

    try {
      String boundary = Long.toHexString(System.currentTimeMillis());

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

      PrintWriter writer = new PrintWriter(new OutputStreamWriter(
        connection.getOutputStream(),
        StandardCharsets.UTF_8
      ));

      String lineFeed = "\r\n";
      writer.append("--" + boundary).append(lineFeed);
      writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + filenameOnly + "\"")
            .append(lineFeed);
      writer.append("Content-Type: text/plain").append(lineFeed);
      writer.append(lineFeed);
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        for (String line; (line = reader.readLine()) != null; ) {
          writer.append(line).append(lineFeed);
        }
      } finally {
        if (reader != null) {
          reader.close();
        }
      }

      writer.append(lineFeed);
      writer.append("--" + boundary + "--").append(lineFeed);
      writer.append(lineFeed);
      writer.flush();
      writer.close();

      connection.disconnect();
      System.out
        .println("### File uplaoded! " + connection.getResponseCode() + connection.getResponseMessage() + " ###");

    } catch (Exception e) {
      logger.info("### File upload failed ###");
      throw new RuntimeException(e.getMessage());
    }

    return jsonResult;
  }

  public JSONObject getOutputFiles(String projectName) throws IOException, SaxonApiException {
    JSONObject json = new JSONObject();

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" + projectName,
      null
    );

    String urlString = url.toString();

    Map<String, String> nameSpace = new LinkedHashMap<>();
    nameSpace.put("xlink", "http://www.w3.org/1999/xlink");
    XdmNode doc = Saxon.buildDocument(new StreamSource(urlString));
    for (XdmItem file : Saxon.xpath(doc, "/clam/output/file")) {
      String href = Saxon.xpath2string(file, "@xlink:href", null, nameSpace);
      String name = Saxon.xpath2string(file, "name");
      json.put(name, href);
    }

    return json;

  }

  public JSONObject downloadProject(String projectName) throws IOException, SaxonApiException, ConfigurationException {
    final String outputPathConst = "output";

    DeploymentLib dplib = new DeploymentLib();
    String workDir = dplib.getWd();
    String outputDir = dplib.getOutputDir();
    logger.info(String.format("### current outputPath: %s ###", outputDir));

    String outputPath = Paths.get(workDir, projectName, outputPathConst).normalize().toString();
    logger.info(String.format("### outputPath: %s ###", outputPath));
    String path = Paths.get(workDir, projectName, outputDir).normalize().toString();

    JSONObject jsonFiles = this.getOutputFiles(projectName);
    JSONObject json = new JSONObject();
    /* create output directory if not there */
    File theDir = new File(path);
    // if the directory does not exist, create it
    if (!theDir.exists()) {
      try {
        theDir.mkdir();
      } catch (SecurityException se) {
        System.err.println(se.getMessage());
      }
    }
    /* end create output directory */

    Set<String> keys = jsonFiles.keySet();
    json = jsonFiles;

    keys.forEach((key) -> {
      File file = new File(Paths.get(path, key).normalize().toString());
      logger.info(Paths.get(path, key).normalize().toString());
      URL url = null;

      try {
        String urlString = (String) jsonFiles.get(key);
        urlString = urlString.replace("127.0.0.1", this.serviceUrl.getHost());
        logger.info(urlString);
        url = new URL(urlString);
        FileUtils.copyURLToFile(url, file, 10000, 10000);
      } catch (IOException ex) {
        logger.error(String.format("could not copy to file [%s]", url), ex);
      }

    });
    return json;
  }

  public JSONObject createProject(String projectName) throws IOException {

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

    JSONObject json = new JSONObject();
    json.put("status", httpCon.getResponseCode());
    json.put("message", httpCon.getResponseMessage());
    httpCon.disconnect();
    return json;
  }

  public JSONObject deleteProject(String projectName) throws IOException {
    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" + projectName,
      null
    );
    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    httpCon.setDoOutput(true);
    httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    httpCon.setRequestMethod("DELETE");

    JSONObject json = new JSONObject();
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

    while ((currentLine = in.readLine()) != null) {
      response.append(currentLine);
    }

    in.close();

    return response.toString();
  }


  public JSONObject getSymanticsFromRemote() {
    JSONObject json = new JSONObject();

    return json;

  }

  private Boolean checkUserConfigOnRemoteServer(JSONObject remoteSymantics, JSONObject userSymantics) {
    //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose
    // Tools | Templates.
    return true;
  }

}
