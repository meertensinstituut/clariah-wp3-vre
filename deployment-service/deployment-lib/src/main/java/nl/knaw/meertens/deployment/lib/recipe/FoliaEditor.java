package nl.knaw.meertens.deployment.lib.recipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DeploymentResponse;
import nl.knaw.meertens.deployment.lib.DeploymentStatus;
import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import nl.knaw.meertens.deployment.lib.SystemConf;
import nl.mpi.tla.util.Saxon;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom2.JDOMException;
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

import static java.lang.String.format;
import static java.nio.charset.Charset.forName;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.FINISHED;


public class FoliaEditor implements RecipePlugin {
  private URL serviceUrl;
  protected DeploymentStatus status;
  protected String workDir;
  private static Logger logger = LoggerFactory.getLogger(RecipePlugin.class);
  private static JsonNodeFactory jsonFactory = new JsonNodeFactory(false);

  /**
   * Initiate the recipe
   */
  @Override
  public void init(String workDir, Service service) throws RecipePluginException {
    logger.info(format("init [%s]", workDir));
    ObjectNode json = DeploymentLib.parseSemantics(service.getServiceSemantics());
    this.workDir = workDir;
    try {
      this.serviceUrl = new URL(json.get("serviceLocation").toString());
    } catch (MalformedURLException e) {
      throw new RecipePluginException("Url is not correct: " + serviceUrl);
    }
    this.status = DeploymentStatus.CREATED;
  }

  @Override
  public DeploymentResponse execute() throws RecipePluginException {
    logger.info("Start plugin execution");

    try {
      logger.info("Running project");
      this.runProject(workDir);

      // keep polling project
      logger.info("Polling the service");
      boolean ready = false;
      int counter = 0;
      while (!ready) {
        logger.info(format("polling {%s}", counter));
        counter++;
        Thread.sleep(3000);
        // TODO: where does the polling happen?
        ready = true;
      }

      this.status = FINISHED;

    } catch (IOException | InterruptedException | UnirestException ex) {
      throw new RecipePluginException("Could not execute recipe", ex);
    }

    return status.toResponse();
  }

  @Override
  public DeploymentResponse getStatus() {
    return status.toResponse();
  }

  public ObjectNode runProject(String key) throws IOException, UnirestException, RecipePluginException {
    final String outputPathConst = "output";
    final String inputPathConst = "input";

    ObjectNode userConfig = null;
    userConfig = DeploymentLib.parseUserConfig(key);
    JsonNode params = userConfig.get("params");

    ObjectNode inputOjbect = (ObjectNode) params.get(0);
    String inputFile = inputOjbect.get("value").asText();
    String fullInputPath =
      Paths.get(SystemConf.ROOT_WORK_DIR, workDir, inputPathConst, inputFile).normalize().toString();
    String inputPath = Paths.get(SystemConf.ROOT_WORK_DIR, workDir, inputPathConst).normalize().toString();
    logger.info(format("inputPath: %s", inputPath));
    logger.info(format("Full Input Path: %s", fullInputPath));


    ObjectNode outputOjbect;
    String outputFile;
    if (params.size() > 1) {
      outputOjbect = (ObjectNode) params.get(1);
      outputFile = outputOjbect.get("value").asText();
    } else {
      outputFile = inputFile;
    }

    String fullOutputPath = Paths.get(
      SystemConf.ROOT_WORK_DIR, workDir, outputPathConst, outputFile
    ).normalize().toString();
    logger.info(format("Full outputPath: [%s]", fullOutputPath));

    File outputPathAsFile = new File(Paths.get(fullOutputPath).getParent().normalize().toString());
    if (!outputPathAsFile.exists()) {
      logger.info(format("Creating folder: %s", outputPathAsFile.toString()));
      outputPathAsFile.mkdirs();
    }

    ObjectNode urlJson = uploadFile(key, fullInputPath);
    ObjectNode json = jsonFactory.objectNode();
    File file = new File(fullOutputPath);
    logger.info(format("Generating output file: %s", fullOutputPath));
    writeToHtml(format(
      "<iframe src=\"%s\" width=\"100%%\" height=\"800px\">Text to display when iframe is not supported</iframe>",
      urlJson.get("url")), file);
    json.set("url", urlJson.get("url"));
    return json;

  }

  /**
   * Get output file.
   *
   * @param workDir project name also knows as project ID, working directory and key
   * @throws MalformedURLException Wrongly formed URL
   * @throws IOException           Disk read/write Exception
   * @throws JDOMException         Invalide JDOM
   * @throws SaxonApiException     Saxon Exception
   */
  public ObjectNode getOutputFiles(String workDir) throws MalformedURLException, IOException, SaxonApiException {
    ObjectNode json = jsonFactory.objectNode();

    URL url = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      this.serviceUrl.getFile() + "/" + workDir,
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

  public ObjectNode uploadFile(String workDir, String filename, String language, String inputTemplate,
                               String author) throws IOException, ConfigurationException, UnirestException {
    return uploadFile(workDir, filename);
  }

  public ObjectNode uploadFile(String projectName, String filename)
      throws IOException, UnirestException {
    ObjectNode jsonResult = jsonFactory.objectNode();

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

    HttpResponse<String> jsonResponse = Unirest
      .post(url.toString())
      .header("accept", "application/json")
      .header("file", file.toString())
      .field("file", file)
      .asString();

    logger.info(format("Response code: %s", jsonResponse.getCode()));
    Headers headers = jsonResponse.getHeaders();
    logger.info(format("Response url: %s", headers.get("location").get(0)));

    URL returnUrl = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      headers.get("location").get(0),
      null
    );
    logger.info(format("returnUrl: %s", returnUrl.toString()));

    URL devReturnUrl = new URL("http://localhost:9998" + headers.get("location").get(0));

    logger.info(format("Hacking returnUrl for dev environment: %s", devReturnUrl.toString()));
    jsonResult.put("url", devReturnUrl.toString());
    Unirest.shutdown();
    return jsonResult;
  }

  private static void writeToHtml(String content, File file) throws IOException {
    FileUtils.writeStringToFile(new File(file.toString()), content, forName("UTF-8"));
    logger.info("Generated successfully");
  }
}