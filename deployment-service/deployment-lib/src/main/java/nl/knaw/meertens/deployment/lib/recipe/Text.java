package nl.knaw.meertens.deployment.lib.recipe;


import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DeploymentStatus;
import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.String.format;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.RUNNING;
import static nl.knaw.meertens.deployment.lib.SystemConf.INPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;

public class Text implements RecipePlugin {
  public URL serviceUrl;
  protected DeploymentStatus status;
  protected String projectName;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void init(String workDir, Service service) {
    logger.info(format("init [%s]", workDir));
    this.projectName = workDir;
    this.serviceUrl = null;
    logger.info("finish init Text plugin");
    this.status = DeploymentStatus.CREATED;
  }

  @Override
  public JSONObject execute() throws RecipePluginException {
    logger.info("Start plugin execution");
    JSONObject userConfig;
    try {
      userConfig = DeploymentLib.parseUserConfig(projectName);
      logger.info("userConfig: ");
      logger.info(userConfig.toJSONString());

      logger.info("Running project");
      this.runProject(projectName);

      this.status = DeploymentStatus.RUNNING;

      // keep polling project
      logger.info("Polling the service");
      boolean ready = false;
      int counter = 0;
      while (!ready) {
        logger.info(String.format("polling {%s}", counter));
        counter++;
        Thread.sleep(3000);
        ready = true;
      }

      this.status = FINISHED;

    } catch (IOException | InterruptedException ex) {
      logger.error(String.format("Execution ERROR: {%s}", ex.getLocalizedMessage()), ex);
    }

    return status.getJsonStatus();
  }

  @Override
  public JSONObject getStatus() {
    return status.getJsonStatus();
  }

  private void runProject(String key) throws IOException, RecipePluginException {
    JSONObject userConfig = DeploymentLib.parseUserConfig(key);
    JSONArray params = (JSONArray) userConfig.get("params");

    JSONObject inputOjbect = (JSONObject) params.get(0);
    String inputFile = (String) inputOjbect.get("value");
    String inputPath = Paths.get(ROOT_WORK_DIR, projectName, INPUT_DIR).normalize().toString();
    String fullInputPath = Paths.get(ROOT_WORK_DIR, projectName, INPUT_DIR, inputFile).normalize().toString();
    logger.info(String.format("Full inputPath: %s", fullInputPath));
    logger.info(String.format("inputPath: %s", inputPath));

    String content = new String(Files.readAllBytes(Paths.get(fullInputPath)));

    JSONObject outputOjbect;
    String outputFile;
    if (params.size() > 1) {
      outputOjbect = (JSONObject) params.get(1);
      outputFile = (String) outputOjbect.get("value");
    } else {
      outputFile = inputFile;
    }

    String outputPath = Paths.get(ROOT_WORK_DIR, projectName, OUTPUT_DIR, outputFile).normalize().toString();

    File outputPathAsFile = Paths
      .get(outputPath).getParent()
      .normalize().toFile();
    if (!outputPathAsFile.exists()) {
      logger.info(String.format("Creating folder: %s", outputPathAsFile.toString()));
      outputPathAsFile.mkdirs();
    }

    File file = new File(outputPath);

    try (FileWriter fileWriter = new FileWriter(file)) {
      fileWriter.write("<pre>");
      fileWriter.write(content);
      fileWriter.write("</pre>");
      fileWriter.flush();
    }

  }

}
