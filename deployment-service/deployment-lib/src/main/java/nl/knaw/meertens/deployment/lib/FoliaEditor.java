/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.knaw.meertens.deployment.lib;

import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.mpi.tla.util.Saxon;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom2.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.Charset.forName;


/**
 * This is the Folia editor recipe.
 *
 * @author Vic
 */
public class FoliaEditor implements RecipePlugin {
  public URL serviceUrl;
  protected int counter = 0;
  protected Boolean isFinished = false;
  protected Boolean userConfigRemoteError = false;
  protected String projectName;
  protected String resultFileNameString = "";
  private static Logger logger = LoggerFactory.getLogger(RecipePlugin.class);

  public static void writeToHtml(String content, File file) throws IOException {
    FileUtils.writeStringToFile(new File(file.toString()), content, forName("UTF-8"));
    logger.info("Generated successfully");
  }

  /**
   * Initiate the recipe.
   */
  @Override
  public void init(String workDir, Service service) throws RecipePluginException {
    logger.info("init Folia Editor plugin");
    JSONObject json = DeploymentLib.parseSemantics(service.getServiceSemantics());
    this.projectName = workDir;
    try {
      this.serviceUrl = new URL((String) json.get("serviceLocation"));
    } catch (MalformedURLException e) {
      throw new RecipePluginException("Url is not correct: " + serviceUrl);
    }
    logger.info("finish init Folia Editor plugin");

  }

  @Override
  public JSONObject execute() throws RecipePluginException {
    logger.info("Start plugin execution");

    JSONObject json = new JSONObject();
    json.put("key", projectName);
    json.put("status", 202);
    JSONObject userConfig = new JSONObject();
    try {
      DeploymentLib dplib = new DeploymentLib();
      userConfig = dplib.parseUserConfig(projectName);
      logger.info("userConfig: ");
      logger.info(userConfig.toJSONString());

      logger.info("Running project");
      this.runProject(projectName);

      // keep polling project
      logger.info("Polling the service");
      boolean ready = false;
      int counter = 0;
      while (!ready) {
        logger.info(String.format("polling {%s}", counter));
        counter++;
        Thread.sleep(3000);

        // TODO: check if output file exists, if so, ready = true, else false
        ready = true;
      }

      this.isFinished = true;

    } catch (IOException | InterruptedException | UnirestException | ConfigurationException ex) {
      logger.error(String.format("Execution ERROR: {%s}", ex.getLocalizedMessage()), ex);
    }

    return json;
  }

  public JSONObject runProject(String key) throws IOException, ConfigurationException, UnirestException {
    final String outputPathConst = "output";
    final String inputPathConst = "input";

    DeploymentLib dplib = new DeploymentLib();

    JSONObject userConfig = null;
    userConfig = dplib.parseUserConfig(key);
    JSONArray params = (JSONArray) userConfig.get("params");

    JSONObject inputOjbect = (JSONObject) params.get(0);
    String inputFile = (String) inputOjbect.get("value");
    String fullInputPath =
      Paths.get(SystemConf.systemWorkDir, projectName, inputPathConst, inputFile).normalize().toString();
    String inputPath = Paths.get(SystemConf.systemWorkDir, projectName, inputPathConst).normalize().toString();
    logger.info(String.format("inputPath: %s", inputPath));
    logger.info(String.format("Full Input Path: %s", fullInputPath));


    JSONObject outputOjbect;
    String outputFile;
    if (params.size() > 1) {
      outputOjbect = (JSONObject) params.get(1);
      outputFile = (String) outputOjbect.get("value");
    } else {
      outputFile = inputFile;
    }

    String outputPath = Paths.get(SystemConf.systemWorkDir, projectName, outputPathConst).normalize().toString();
    String fullOutputPath =
      Paths.get(SystemConf.systemWorkDir, projectName, outputPathConst, outputFile).normalize().toString();
    logger.info(String.format("outputPath: %s", outputPath));
    logger.info(String.format("Full outputPath: %s", fullOutputPath));

    File outputPathAsFile = new File(Paths.get(fullOutputPath).getParent().normalize().toString());
    if (!outputPathAsFile.exists()) {
      logger.info(String.format("Creating folder: %s", outputPathAsFile.toString()));
      outputPathAsFile.mkdirs();
    }

    JSONObject urlJson = uploadFile(key, fullInputPath);
    JSONObject json = new JSONObject();
    File file = new File(fullOutputPath);
    logger.info(String.format("Generating output file: %s", fullOutputPath));
    writeToHtml(String.format(
      "<iframe src=\"%s\" width=\"100%%\" height=\"800px\">Text to display when iframe is not supported</iframe>",
      (String) urlJson.get("url")), file);
    json.put("url", urlJson.get("url"));
    return json;

  }

  /**
   * Project ID also known as key, working directory
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
   * Get output file.
   *
   * @param projectName project name also knows as project ID, working directory and key
   * @throws MalformedURLException Wrongly formed URL
   * @throws IOException           Disk read/write Exception
   * @throws JDOMException         Invalide JDOM
   * @throws SaxonApiException     Saxon Exception
   */
  public JSONObject getOutputFiles(String projectName) throws MalformedURLException, IOException, SaxonApiException {
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

  public static String[] explodeString(String stringToExplode, String separator) {
    return StringUtils.splitPreserveAllTokens(stringToExplode, separator);
  }

  public void downloadResultFile(String url) throws IOException {
    downloadResultFile(new URL(url));
  }

  public void downloadResultFile(URL url) throws IOException {
    logger.info("Download URL:" + url + "");
    FileUtils.copyURLToFile(
      url,
      new File("resultFile.xml"),
      60,
      60);
    logger.info("Download successful");
  }

  public JSONObject uploadFile(String projectName, String filename, String language, String inputTemplate,
                               String author) throws IOException, ConfigurationException, UnirestException {
    return uploadFile(projectName, filename);
  }

  public JSONObject uploadFile(String projectName, String filename)
      throws IOException, ConfigurationException, UnirestException {
    JSONObject jsonResult = new JSONObject();
    DeploymentLib dplib = new DeploymentLib();

    String path = filename;
    logger.info("File path to be uploaded:" + path + "");

    jsonResult.put("pathUploadFile", path);
    File file = new File(path);
    String filenameOnly = file.getName();
    jsonResult.put("filenameOnly", filenameOnly);

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/pub/upload/",
      null
    );
    logger.info("Upload URL:" + url.toString() + "");

    com.mashape.unirest.http.HttpResponse<JsonNode> jsonResponse = Unirest
      .post(url.toString())
      .header("accept", "application/json")
      .header("file", file.toString())
      .field("file", file)
      .asJson();

    logger.info(String.format("Response code: %s", jsonResponse.getCode()));
    Headers headers = jsonResponse.getHeaders();
    String returnUrlString = headers.get("location").get(0);
    logger.info(String.format("Response full headers: %s", headers.toString()));
    logger.info(String.format("Response url: %s", returnUrlString));

    URL returnUrl = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      returnUrlString,
      null
    );
    logger.info(String.format("returnUrl: %s", returnUrl.toString()));

    URL devReturnUrl = new URL("http://localhost:9998" + returnUrlString);
    resultFileNameString = returnUrlString;

    logger.info(String.format("Hacking returnUrl for dev environment: %s", devReturnUrl.toString()));
    // jsonResult.put("url", returnUrl.toString());
    jsonResult.put("url", devReturnUrl.toString());
    Unirest.shutdown();
    return jsonResult;
  }
}
