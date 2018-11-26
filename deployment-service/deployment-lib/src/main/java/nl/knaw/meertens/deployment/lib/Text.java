/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.knaw.meertens.deployment.lib;


import net.sf.saxon.s9api.SaxonApiException;
import org.apache.commons.configuration.ConfigurationException;
import org.jdom2.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Vic
 */
public class Text implements RecipePlugin {
  public URL serviceUrl;
  protected int counter = 0;
  protected Boolean isFinished = false;
  protected Boolean userConfigRemoteError = false;
  protected String projectName;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void init(String projectName, Service service) throws RecipePluginException {
    logger.info("init Text plugin");
    this.projectName = projectName;
    this.serviceUrl = null;
    logger.info("finish init Text plugin");
  }

  @Override
  public JSONObject execute() throws RecipePluginException {
    logger.info("## Start plugin execution ##");

    JSONObject json = new JSONObject();
    json.put("key", projectName);
    json.put("status", 202);
    JSONObject userConfig = new JSONObject();
    try {
      userConfig = new DeploymentLib().parseUserConfig(projectName);
      logger.info("## userConfig:  ##");
      logger.info(userConfig.toJSONString());

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
        ready = true;
      }

      this.isFinished = true;

    } catch (ConfigurationException | ParseException | IOException | InterruptedException ex) {
      logger.error(String.format("## Execution ERROR: {%s}", ex.getLocalizedMessage()), ex);
    }

    return json;
  }

  public JSONObject runProject(String key) throws IOException, ParseException, ConfigurationException {
    final String outputPathConst = "output";
    final String inputPathConst = "input";
    DeploymentLib dplib = new DeploymentLib();

    String workDir = dplib.getWd();
    // String userConfFile = dplib.getConfFile();
    JSONObject userConfig = dplib.parseUserConfig(key);
    JSONArray params = (JSONArray) userConfig.get("params");

    JSONObject inputOjbect = (JSONObject) params.get(0);
    String inputFile = (String) inputOjbect.get("value");
    String inputPath = Paths.get(workDir, projectName, inputPathConst).normalize().toString();
    String fullInputPath = Paths.get(workDir, projectName, inputPathConst, inputFile).normalize().toString();
    logger.info(String.format("### Full inputPath: %s ###", fullInputPath));
    logger.info(String.format("### inputPath: %s ###", inputPath));

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
    logger.info(String.format("### outputPath: %s ###", outputPath));
    logger.info(String.format("### Full outputPath: %s ###", fullOutputPath));

    File outputPathAsFile = new File(Paths.get(fullOutputPath).getParent().normalize().toString());
    if (!outputPathAsFile.exists()) {
      logger.info(String.format("### Creating folder: %s ###", outputPathAsFile.toString()));
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

}
