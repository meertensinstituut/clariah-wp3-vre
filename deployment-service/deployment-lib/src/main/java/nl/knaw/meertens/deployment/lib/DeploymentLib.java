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
import org.apache.commons.configuration.XMLConfiguration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author Vic
 */
public class DeploymentLib {

  private final String defaultConfiPath = "/conf/conf.xml";
  String fullPath;
  File config;
  String userConfFile;
  String inputDirectory;
  String outputDirectory;
  String queueLength;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public DeploymentLib() throws ConfigurationException {

    this.config = new File(this.defaultConfiPath);
    this.parseConfig(this.config);
  }

  public File getConfigFile() {
    return this.config;
  }

  public Service getServiceByName(String serviceName) throws IOException, ConfigurationException {
    // valid service, fetch data from db and return
    JSONObject json = new JSONObject();
    JSONParser parser = new JSONParser();
    String dbSessionToken = System.getenv("SERVICES_TOKEN");
    String dbApiKey = System.getenv("APP_KEY_SERVICES");
    DeploymentLib dplib = new DeploymentLib();

    String urlString = "http://dreamfactory:80/api/v2/services/_table/service/?filter=name%3D" + serviceName;
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
      if (resource.size() > 0) {
        JSONObject record = (JSONObject) resource.get(0);
        String serviceRecipe = (String) record.get("recipe");
        String serviceSemantics = (String) record.get("semantics");
        String serviceId = (String) record.get("id");

        return new Service(serviceId, serviceName, serviceRecipe, serviceSemantics, "", true);
      }
    }

    return null;

  }

  public Boolean serviceExists(String serviceName) throws ConfigurationException, IOException {
    Service service = this.getServiceByName(serviceName);
    return service instanceof Service;
  }

  public void parseConfig(File config) throws ConfigurationException {
    XMLConfiguration xml = new XMLConfiguration(config);
    this.fullPath = xml.getString("workingFolder");
    this.userConfFile = xml.getString("userConfFile");
    this.inputDirectory = xml.getString("inputDirectory");
    this.outputDirectory = xml.getString("outputDirectory");
    this.queueLength = xml.getString("configLength");
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

    while ((currentLine = in.readLine()) != null) {
      response.append(currentLine);
    }

    in.close();

    return response.toString();
  }

  String getOutputDir() {
    return this.outputDirectory;
  }

  String getInputDir() {
    return this.inputDirectory;
  }

  String getQueueLength() {
    return this.queueLength;
  }

  public JSONObject parseUserConfig(String key) {
    DeploymentLib dplib = null;
    try {
      dplib = new DeploymentLib();
    } catch (ConfigurationException e) {
      throw new RuntimeException("Configuration file is invalid");
    }

    String workDir = dplib.getWd();
    String userConfFile = dplib.getConfFile();
    JSONParser parser = new JSONParser();

    try {
      String path = Paths.get(workDir, key, userConfFile).normalize().toString();
      JSONObject userConfig = (JSONObject) parser.parse(new FileReader(path));

      return userConfig;
    } catch (Exception ex) {
      logger.info(ex.getLocalizedMessage());
    }
    JSONObject userConfig = new JSONObject();
    userConfig.put("parse user config", "failed");
    return userConfig;
  }


  public static JSONObject parseSymantics(String symantics) throws RecipePluginException {
    try {
      JSONObject parametersJson = new JSONObject();

      Map<String, String> nameSpace = new LinkedHashMap<>();
      nameSpace.put("cmd", "http://www.clarin.eu/cmd/1");
      nameSpace.put("cmdp", "http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1527668176011");

      StringReader reader = new StringReader(symantics);
      XdmNode service = Saxon.buildDocument(new StreamSource(reader));


      int counter = 0;
      for (XdmItem param : Saxon
        .xpath(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:Parameter", null, nameSpace)) {

        JSONObject parameterJson = new JSONObject();
        String parameterName = Saxon.xpath2string(param, "cmdp:Name", null, nameSpace);
        String parameterDescription = Saxon.xpath2string(param, "cmdp:Description", null, nameSpace);
        String parameterType = Saxon.xpath2string(param, "cmdp:MIMEType", null, nameSpace);

        parameterJson.put("parameterName", parameterName);
        parameterJson.put("parameterDescription", parameterDescription);
        parameterJson.put("parameterType", parameterType);

        int valueCounter = 0;
        for (XdmItem paramValue : Saxon
          .xpath(service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:Parameter/cmdp:ParameterValue",
            null, nameSpace)) {

          JSONObject parameterValueJson = new JSONObject();

          String parameterValueValue = Saxon.xpath2string(paramValue, "cmdp:Value", null, nameSpace);
          String parameterValueDescription = Saxon.xpath2string(paramValue, "cmdp:Description", null, nameSpace);

          parameterValueJson.put("parameterValueValue", parameterValueValue);
          parameterValueJson.put("parameterValueDescription", parameterValueDescription);

          parameterJson.put("value" + Integer.toString(valueCounter), parameterValueJson);

          valueCounter++;
        }

        parametersJson.put("parameter" + Integer.toString(counter), parameterJson);

        counter++;
      }

      JSONObject json = new JSONObject();
      String serviceName = Saxon.xpath2string(service, "cmdp:Name", null, nameSpace);
      String serviceDescription = Saxon.xpath2string(service, "//cmdp:Service/cmdp:Description", null, nameSpace);
      String serviceLocation = Saxon.xpath2string(
        service, "//cmdp:ServiceDescriptionLocation/cmdp:Location", null, nameSpace);

      json.put("serviceName", serviceName);
      json.put("serviceDescription", serviceDescription);
      json.put("serviceLocation", serviceLocation);
      json.put("counter", counter);
      json.put("parameters", parametersJson);
      return json;
    } catch (SaxonApiException ex) {
      throw new RecipePluginException("Invalid semantics xml");
    }
  }

}
