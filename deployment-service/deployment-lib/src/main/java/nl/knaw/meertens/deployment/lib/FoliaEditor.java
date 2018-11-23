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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  public static void writeToHtml(String content, File file) throws IOException {
    FileUtils.writeStringToFile(new File(file.toString()), content, forName("UTF-8"));
    System.out.println("### Generated successfully! ###");
  }

  // public static String readStringFromURL(URL requestURL) throws IOException {
  //   try (Scanner scanner = new Scanner(requestURL.openStream(),
  //     StandardCharsets.UTF_8.toString())) {
  //     scanner.useDelimiter("\\A");
  //     return scanner.hasNext() ? scanner.next() : "";
  //   }
  // }

  /**
   * Initiate the recipe.
   *
   * @param projectName
   * Project also known as the folder name
   * @param service
   * The record in the service registry
   * @throws JDOMException
   * JDOM Exception
   * @throws IOException
   * IO Exception
   * @throws SaxonApiException
   * Exception
   */
  @Override
  public void init(String projectName, Service service) throws JDOMException, IOException, SaxonApiException {
    System.out.print("init Folia Editor plugin");
    JSONObject json = this.parseSymantics(service.getServiceSymantics());
    this.projectName = projectName;
    this.serviceUrl = new URL((String) json.get("serviceLocation"));
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
      int counter = 0;
      while (!ready) {
        logger.info(String.format("polling {%s}", counter));
        counter++;
        Thread.sleep(3000);

        // TODO: check if output file exists, if so, ready = true, else false
        ready = 1 == 1;
      }

      this.isFinished = true;

    } catch (IOException | InterruptedException | UnirestException ex) {
      logger.info(String.format("## Execution ERROR: {%s}", ex.getLocalizedMessage()));
      Logger.getLogger(FoliaEditor.class.getName()).log(Level.SEVERE, null, ex);
    } catch (ConfigurationException ex) {
      Logger.getLogger(FoliaEditor.class.getName()).log(Level.SEVERE, null, ex);
    }

    return json.toString();
  }

  /**
   * @param key
   * Project name also knows as working directory
   * @throws FileNotFoundException
   * File not found
   * @throws IOException
   * IOException
   * @throws org.json.simple.parser.ParseException
   * Invalid json
   * @throws org.apache.commons.configuration.ConfigurationException
   * Invalid configuration
   */
  @Override
  public JSONObject parseUserConfig(String key) throws ConfigurationException {
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

  public JSONObject runProject(String key) throws IOException, ConfigurationException, UnirestException {
    final String outputPathConst = "output";
    final String inputPathConst = "input";

    DeploymentLib dplib = new DeploymentLib();

    String workDir = dplib.getWd();
    JSONObject userConfig = this.parseUserConfig(key);
    JSONArray params = (JSONArray) userConfig.get("params");

    JSONObject inputOjbect = (JSONObject) params.get(0);
    String inputFile = (String) inputOjbect.get("value");
    String fullInputPath = Paths.get(workDir, projectName, inputPathConst, inputFile).normalize().toString();
    String inputPath = Paths.get(workDir, projectName, inputPathConst).normalize().toString();
    System.out.println(String.format("### inputPath: %s ###", inputPath));
    System.out.println(String.format("### Full Input Path: %s ###", fullInputPath));


    JSONObject outputOjbect;
    String outputFile;
    if (params.size() > 1) {
      outputOjbect = (JSONObject) params.get(1);
      outputFile = (String) outputOjbect.get("value");
    } else {
      outputFile = inputFile;
    }

    String outputPath = Paths.get(workDir, projectName, outputPathConst).normalize().toString();
    String fullOutputPath = Paths.get(workDir, projectName, outputPathConst, outputFile).normalize().toString();
    System.out.println(String.format("### outputPath: %s ###", outputPath));
    System.out.println(String.format("### Full outputPath: %s ###", fullOutputPath));

    File outputPathAsFile = new File(Paths.get(fullOutputPath).getParent().normalize().toString());
    if (!outputPathAsFile.exists()) {
      System.out.println(String.format("### Creating folder: %s ###", outputPathAsFile.toString()));
      outputPathAsFile.mkdirs();
    }

    JSONObject urlJson = uploadFile(key, fullInputPath);
    JSONObject json = new JSONObject();
    File file = new File(fullOutputPath);
    System.out.println(String.format("### Generating output file: %s ###", fullOutputPath));
    writeToHtml(String.format(
      "<iframe src=\"%s\" width=\"100%%\" height=\"800px\">Text to display when iframe is not supported</iframe>",
      (String) urlJson.get("url")), file);
    json.put("url", urlJson.get("url"));
    return json;

  }

  /**
   * @param pid
   * Project ID also known as key, working directory
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
   * Get output file.
   *
   * @param projectName
   * project name also knows as project ID, working directory and key
   * @throws MalformedURLException
   * Wrongly formed URL
   * @throws IOException
   * Disk read/write Exception
   * @throws JDOMException
   * Invalide JDOM
   * @throws SaxonApiException
   * Saxon Exception
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

  @Override
  public JSONObject parseSymantics(String symantics) throws JDOMException, SaxonApiException {
    System.out.println(String.format("### symantics in parseSymantics before parsing: %s ###", symantics));
    JSONObject json = new JSONObject();
    JSONObject parametersJson = new JSONObject();

    Map<String, String> nameSpace = new LinkedHashMap<>();
    nameSpace.put("cmd", "http://www.clarin.eu/cmd/1");
    nameSpace.put("cmdp", "http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1527668176011");

    StringReader reader = new StringReader(symantics);
    XdmNode service = Saxon.buildDocument(new StreamSource(reader));

    String serviceName = Saxon.xpath2string(service, "//cmdp:Service/cmdp:Name", null, nameSpace);
    String serviceDescription = Saxon.xpath2string(service, "//cmdp:Service/cmdp:Description", null, nameSpace);
    String serviceLocation = Saxon.xpath2string(
      service, "//cmdp:ServiceDescriptionLocation/cmdp:Location", null, nameSpace);

    String inputName = Saxon.xpath2string(
      service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:Name", null, nameSpace);
    String inputLabel = Saxon.xpath2string(
      service,
      "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:Label",
      null,
      nameSpace);
    String inputType = Saxon
      .xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:MIMEType", null,
        nameSpace);
    String inputCardinalityMin = Saxon.xpath2string(service,
      "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:MinimumCardinality", null, nameSpace);
    String inputCardinalityMax = Saxon.xpath2string(service,
      "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:MaximumCardinality", null, nameSpace);

    String outputName = Saxon.xpath2string(
      service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Output/cmdp:Parameter/cmdp:Name", null, nameSpace);
    String outputType = Saxon.xpath2string(
      service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Output/cmdp:Parameter/cmdp:MIMEType", null, nameSpace);
    String outputCardinalityMin = Saxon
      .xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Output/cmdp:Parameter/cmdp:MinimumCardinality",
        null, nameSpace);
    String outputCardinalityMax = Saxon
      .xpath2string(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Output/cmdp:Parameter/cmdp:MaximumCardinality",
        null, nameSpace);

    json.put("serviceName", serviceName);
    json.put("serviceDescription", serviceDescription);
    json.put("serviceLocation", serviceLocation);

    json.put("inputName", inputName);
    json.put("inputLabel", inputLabel);
    json.put("inputType", inputType);
    json.put("inputCardinalityMin", inputCardinalityMin);
    json.put("inputCardinalityMax", inputCardinalityMax);

    json.put("outputName", outputName);
    // json.put("outputLabel", outputLabel);
    json.put("outputType", outputType);
    json.put("outputCardinalityMin", outputCardinalityMin);
    json.put("outputCardinalityMax", outputCardinalityMax);

    return json;

  }

  public static String[] explodeString(String stringToExplode, String separator) {
    return  StringUtils.splitPreserveAllTokens(stringToExplode, separator);
  }

  public void downloadResultFile(String url) throws IOException {
    downloadResultFile(new URL(url));
  }

  public void downloadResultFile(URL url) throws IOException {
    System.out.println("### Download URL:" + url + " ###");
    FileUtils.copyURLToFile(
        url,
        new File("resultFile.xml"),
        60,
        60);
    System.out.println("### Download successful ###");
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
    System.out.println("### File path to be uploaded:" + path + " ###");

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
    System.out.println("### Upload URL:" + url.toString() + " ###");

    com.mashape.unirest.http.HttpResponse<JsonNode> jsonResponse = Unirest
      .post(url.toString())
      .header("accept", "application/json")
      .header("file", file.toString())
      .field("file", file)
      .asJson();

    System.out.println(String.format("### Response code: %s ###", jsonResponse.getCode()));
    Headers headers = jsonResponse.getHeaders();
    String returnUrlString = headers.get("location").get(0);
    System.out.println(String.format("### Response full headers: %s ###", headers.toString()));
    System.out.println(String.format("### Response url: %s ###", returnUrlString));

    URL returnUrl = new URL(
      this.serviceUrl.getProtocol(),
      this.serviceUrl.getHost(),
      this.serviceUrl.getPort(),
      returnUrlString,
      null
    );
    System.out.println(String.format("### returnUrl: %s ###", returnUrl.toString()));

    URL devReturnUrl = new URL("http://localhost:9998" + returnUrlString);
    resultFileNameString = returnUrlString;

    System.out.println(String.format("### Hacking returnUrl for dev environment: %s ###", devReturnUrl.toString()));
    // jsonResult.put("url", returnUrl.toString());
    jsonResult.put("url", devReturnUrl.toString());
    Unirest.shutdown();
    return jsonResult;
  }
}
