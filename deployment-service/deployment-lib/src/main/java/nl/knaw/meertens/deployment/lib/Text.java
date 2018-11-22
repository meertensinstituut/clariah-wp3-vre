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
import org.jdom2.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

//import java.util.Iterator;

/**
 * @author Vic
 */
public class Text implements RecipePlugin {
  public URL serviceUrl;
  protected int counter = 0;
  protected Boolean isFinished = false;
  protected Boolean userConfigRemoteError = false;
  protected String projectName;

  /**
   * @param projectName
   *
   * @param service
   *
   * @throws JDOMException
   *
   * @throws IOException
   *
   * @throws SaxonApiException
   *
   */
  @Override
  public void init(String projectName, Service service) throws JDOMException, IOException, SaxonApiException {
    System.out.print("init Text plugin");
    JSONObject json = this.parseSymantics(service.getServiceSymantics());
    this.projectName = projectName;
    this.serviceUrl = null;
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
      int counter = 0;
      while (!ready) {
        logger.info(String.format("polling {%s}", counter));
        counter++;
        Thread.sleep(3000);

        // TODO: check if output file exists, if so, ready = true, else false
        ready = 1 == 1;
      }

      this.isFinished = true;

    } catch (IOException | InterruptedException ex) {
      logger.info(String.format("## Execution ERROR: {%s}", ex.getLocalizedMessage()));
      Logger.getLogger(Text.class.getName()).log(Level.SEVERE, null, ex);
    } catch (ParseException ex) {
      Logger.getLogger(Text.class.getName()).log(Level.SEVERE, null, ex);
    } catch (ConfigurationException ex) {
      Logger.getLogger(Text.class.getName()).log(Level.SEVERE, null, ex);
    }

    return json.toString();
  }

  /**
   * @param key
   *
   * @throws FileNotFoundException
   *
   * @throws IOException
   *
   * @throws org.json.simple.parser.ParseException
   *
   * @throws org.apache.commons.configuration.ConfigurationException
   *
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

  public JSONObject runProject(String key) throws IOException, ParseException, ConfigurationException {
    final String outputPathConst = "output";
    final String inputPathConst = "input";
    DeploymentLib dplib = new DeploymentLib();

    String workDir = dplib.getWd();
    // String userConfFile = dplib.getConfFile();
    JSONObject userConfig = this.parseUserConfig(key);
    JSONArray params = (JSONArray) userConfig.get("params");

    JSONObject inputOjbect = (JSONObject) params.get(0);
    String inputFile = (String) inputOjbect.get("value");
    String inputPath = Paths.get(workDir, projectName, inputPathConst).normalize().toString();
    String fullInputPath = Paths.get(workDir, projectName, inputPathConst, inputFile).normalize().toString();
    System.out.println(String.format("### Full inputPath: %s ###", fullInputPath));
    System.out.println(String.format("### inputPath: %s ###", inputPath));

    String content = new String(Files.readAllBytes(Paths.get(fullInputPath)));

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

    File file = new File(fullOutputPath);

    try (FileWriter fileWriter = new FileWriter(file)) {
      fileWriter.write("<pre>");
      fileWriter.write(content);
      fileWriter.write("</pre>");
      fileWriter.flush();
    }


    JSONObject json = new JSONObject();
    return json;

  }

  /**
   * @param pid
   *
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
      service, "//cmdp:Operation[cmdp:Name='main']/cmdp:Input/cmdp:ParameterGroup/cmdp:Label", null, nameSpace);
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
    json.put("outputType", outputType);
    json.put("outputCardinalityMin", outputCardinalityMin);
    json.put("outputCardinalityMax", outputCardinalityMax);

    return json;

  }

}
