package nl.knaw.meertens.deployment.lib.recipe;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DeploymentResponse;
import nl.knaw.meertens.deployment.lib.DeploymentStatus;
import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import nl.mpi.tla.util.Saxon;
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
import java.io.IOException;
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

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.CREATED;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.ERROR;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.deployment.lib.SystemConf.INPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;

public class Clam implements RecipePlugin {
  private URL serviceUrl;
  private DeploymentStatus status;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private String workDir;

  /**
   * Clam needs an alphanumeric string without dashes
   */
  private String projectName;

  /**
   * Initiate the current plugin
   *
   * @param workDir The 'project name' in the sense that Clam requires a project name to work with.
   * @param service The recepie record in the registry known as service
   */
  @Override
  public void init(String workDir, Service service) throws RecipePluginException {
    status = CREATED;

    logger.info(format("init [%s]", workDir));

    this.workDir = workDir;
    this.projectName = workDir.replace("-", "");

    final String serviceSemantics = service.getServiceSemantics();
    JSONObject semantics = DeploymentLib.parseSemantics(serviceSemantics);

    String serviceLocation = (String) semantics.get("serviceLocation");
    try {
      this.serviceUrl = new URL(serviceLocation);
    } catch (MalformedURLException e) {
      throw new RecipePluginException(format("service url [%s] is invalid", serviceLocation), e);
    }
    logger.info("finish init CLAM plugin");
  }

  @Override
  public DeploymentResponse execute() throws RecipePluginException {
    logger.info("start plugin execution");

    JSONObject userConfig;
    try {
      userConfig = DeploymentLib.parseUserConfig(workDir);

      if (!this.checkUserConfigOnRemoteServer(this.getSymenticsFromRemote(), userConfig)) {
        logger.error("bad user config according to remote server");
        return ERROR.toDeploymentResponse();
      }

      logger.info(format("creating project [%s]", workDir));
      this.createProject();

      logger.info(format("upload files of [%s]", workDir));
      this.prepareProject();

      logger.info(format("running [%s]", workDir));
      this.runProject();

      logger.info(format("polling [%s]", workDir));
      pollDeployment();

      logger.info("download result");
      this.downloadProject();

      this.status = FINISHED;

    } catch (IOException | InterruptedException | JDOMException ex) {
      logger.error(format("execution of [%s] failed", workDir), ex);
    }

    return status.toDeploymentResponse();
  }

  private void pollDeployment() throws InterruptedException, RecipePluginException {
    boolean ready = false;
    int looper = 0;
    while (!ready) {
      logger.info(format("polling [%s]", looper));
      looper++;
      Thread.sleep(ofSeconds(3).toMillis());
      JSONObject projectStatus = this.pollProject();
      Long completionCode = (Long) projectStatus.get("completion");
      Long statusCode = (Long) projectStatus.get("statuscode");
      Boolean successCode = (Boolean) projectStatus.get("success");
      ready = (completionCode == 100L && statusCode == 2L && successCode);
    }
  }

  private JSONObject runProject() throws IOException, JDOMException {

    JSONObject json = this.getAccessToken(projectName);
    String user = (String) json.get("user");
    String accessToken = (String) json.get("accessToken");

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

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" + projectName + "/?user=" + user + "&accesstoken=" + accessToken,
      null
    );
    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
    httpCon.setDoOutput(true);
    httpCon.setRequestMethod("POST");
    httpCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    httpCon.setRequestProperty("Accept", "application/json");
    httpCon.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
    httpCon.getOutputStream().write(postDataBytes);

    json.put("status", httpCon.getResponseCode());
    json.put("message", httpCon.getResponseMessage());

    httpCon.disconnect();
    return json;

  }

  private void prepareProject()
    throws IOException, RecipePluginException {
    JSONObject json = DeploymentLib.parseUserConfig(workDir);

    JSONArray params = (JSONArray) json.get("params");

    for (Object param : params) {
      JSONObject objParam = (JSONObject) param;
      String inputTemplate = (String) objParam.get("name");
      String value = (String) objParam.get("value");


      JSONArray innerParams = (JSONArray) objParam.get("params");
      String author = "";
      String language = "";

      if (isNull(innerParams)) {
        throw new IllegalArgumentException("inner params should not be null");
      }

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

      String type = (String) objParam.get("type");
      if ("file".equals(type)) {
        json = this.uploadFile(value, language, inputTemplate, author);
        json.put("workDir", workDir);
        json.put("clamProjectName", projectName);
        json.put("value", value);
        json.put("language", language);
        json.put("inputTemplate", inputTemplate);
        json.put("author", author);
      }
    }
  }

  @Override
  public DeploymentResponse getStatus() {
    return status.toDeploymentResponse();
  }

  private JSONObject pollProject() throws RecipePluginException {
    try {
      JSONObject json = this.getAccessToken(projectName);
      String user = (String) json.get("user");
      String accessToken = (String) json.get("accessToken");

      URL url;
      try {
        url = new URL(
          this.serviceUrl.getProtocol(),
          this.serviceUrl.getHost(),
          this.serviceUrl.getPort(),
          this.serviceUrl.getFile() + "/" + projectName + "/status/?user=" + user + "&accesstoken=" + accessToken,
          null
        );
      } catch (MalformedURLException e) {
        throw new RecipePluginException("Could not create polling url", e);
      }
      HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
      httpCon.setDoOutput(true);
      httpCon.setRequestMethod("GET");

      JSONParser parser = new JSONParser();
      json = (JSONObject) parser.parse(DeploymentLib.getResponseBody(httpCon));

      json.put("status", httpCon.getResponseCode());
      json.put("message", httpCon.getResponseMessage());
      json.put("finished", this.status);

      httpCon.disconnect();

      return json;
    } catch (IOException | JDOMException | ParseException e) {
      throw new RecipePluginException("Could not create polling url", e);

    }
  }

  private JSONObject getAccessToken(String projectName) throws IOException, JDOMException {
    JSONObject json = new JSONObject();

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" + projectName,
      null
    );
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

  private static String readStringFromUrl(URL requestUrl) throws IOException {
    try (Scanner scanner = new Scanner(requestUrl.openStream(),
      StandardCharsets.UTF_8.toString())) {
      scanner.useDelimiter("\\A");
      return scanner.hasNext() ? scanner.next() : "";
    }
  }

  private JSONObject uploadFile(
    String filename,
    String language,
    String inputTemplate,
    String author
  ) throws IOException {
    JSONObject jsonResult = new JSONObject();

    String path = Paths.get(
      ROOT_WORK_DIR,
      workDir,
      INPUT_DIR,
      filename
    ).normalize().toString();
    jsonResult.put("pathUploadFile", path);

    File file = new File(path);
    String filenameOnly = file.getName();
    jsonResult.put("filenameOnly", filenameOnly);

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" +
        projectName + "/input/" + filenameOnly + "?inputtemplate=" + inputTemplate +
        "&language=" + language + "&documentid=&author=" + author + "&filename=" + filenameOnly,
      null
    );

    logger.info(format("upload [%s]", url.toString()));

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
      try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
        for (String line; (line = reader.readLine()) != null; ) {
          writer.append(line).append(lineFeed);
        }
      }

      writer.append(lineFeed);
      writer.append("--" + boundary + "--").append(lineFeed);
      writer.append(lineFeed);
      writer.flush();
      writer.close();

      connection.disconnect();
      logger.info(format(
        "File uploaded; response: [%d][%s]",
        connection.getResponseCode(), connection.getResponseMessage()
      ));

    } catch (Exception e) {
      logger.info("File upload failed");
      throw new RuntimeException(e.getMessage());
    }

    return jsonResult;
  }

  private JSONObject getOutputFiles() {
    try {
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
      logger.info("xml doc:" + doc.toString());
      for (XdmItem file : Saxon.xpath(doc, "/clam/output/file")) {
        String href = Saxon.xpath2string(file, "@xlink:href", null, nameSpace);
        String name = Saxon.xpath2string(file, "name");
        json.put(name, href);
      }

      logger.info("received file list: " + json.toJSONString());
      return json;
    } catch (IOException | SaxonApiException e) {
      logger.error(format("Could not get output file list for [%s]", workDir));
      return new JSONObject();
    }

  }

  private JSONObject downloadProject() {
    String outputPath = Paths.get(ROOT_WORK_DIR, workDir, OUTPUT_DIR)
                             .normalize().toString();
    logger.info(format("outputPath: %s", outputPath));

    JSONObject jsonFiles = this.getOutputFiles();
    File outputDir = new File(outputPath);
    if (!outputDir.exists()) {
      try {
        outputDir.mkdir();
      } catch (SecurityException se) {
        logger.error(se.getMessage(), se);
      }
    }

    Set<String> files = jsonFiles.keySet();
    JSONObject json = jsonFiles;

    files.forEach((outputFile) -> {
      File file = new File(Paths.get(outputPath, outputFile).normalize().toString());
      URL url = null;

      try {
        String urlString = (String) jsonFiles.get(outputFile);
        urlString = urlString.replace("127.0.0.1", this.serviceUrl.getHost());
        url = new URL(urlString);
        FileUtils.copyURLToFile(url, file, 10000, 10000);
        logger.info(format("create file [%s] from url [%s]", file.toPath().toString(), url.toString()));
      } catch (IOException ex) {
        logger.error(format("could not copy file from [%s]", url), ex);
      }

    });
    return json;
  }

  private void createProject() throws RecipePluginException {
    String errorMsg = "Could not create project";
    try {
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
      int responseCode = httpCon.getResponseCode();
      String responseMessage = httpCon.getResponseMessage();
      json.put("status", responseCode);
      json.put("message", responseMessage);
      httpCon.disconnect();
      if (responseCode / 100 != 2) {
        throw new RecipePluginException(format(
          "%s: [%d][%s]", errorMsg, responseCode, responseMessage
        ));
      }
    } catch (IOException ex) {
      throw new RecipePluginException(errorMsg, ex);
    }
  }

  // TODO: get semantics from remove service in case of Clam
  private JSONObject getSymenticsFromRemote() {
    JSONObject json = new JSONObject();
    return json;

  }

  // TODO: check the remote configuration instead of returning true
  private Boolean checkUserConfigOnRemoteServer(JSONObject remoteSymantics, JSONObject userSymantics) {
    return true;
  }

}
