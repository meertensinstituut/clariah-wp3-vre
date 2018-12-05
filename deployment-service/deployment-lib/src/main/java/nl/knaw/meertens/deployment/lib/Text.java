package nl.knaw.meertens.deployment.lib;


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

import static nl.knaw.meertens.deployment.lib.SystemConf.INPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.WORK_DIR;

/**
 * @author Vic
 */
public class Text implements RecipePlugin {
  public URL serviceUrl;
  protected Boolean isFinished = false;
  protected String projectName;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void init(String workDir, Service service) {
    logger.info("init Text plugin");
    this.projectName = workDir;
    this.serviceUrl = null;
    logger.info("finish init Text plugin");
  }

  @Override
  public JSONObject execute() throws RecipePluginException {
    logger.info("Start plugin execution");

    JSONObject json = new JSONObject();
    json.put("key", projectName);
    json.put("status", 202);
    JSONObject userConfig;
    try {
      userConfig = DeploymentLib.parseUserConfig(projectName);
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
        ready = true;
      }

      this.isFinished = true;

    } catch (IOException | InterruptedException ex) {
      logger.error(String.format("Execution ERROR: {%s}", ex.getLocalizedMessage()), ex);
    }

    return json;
  }

  private void runProject(String key) throws IOException, RecipePluginException {
    JSONObject userConfig = DeploymentLib.parseUserConfig(key);
    JSONArray params = (JSONArray) userConfig.get("params");

    JSONObject inputOjbect = (JSONObject) params.get(0);
    String inputFile = (String) inputOjbect.get("value");
    String inputPath = Paths.get(WORK_DIR, projectName, INPUT_DIR).normalize().toString();
    String fullInputPath = Paths.get(WORK_DIR, projectName, INPUT_DIR, inputFile).normalize().toString();
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

    String outputPath = Paths.get(WORK_DIR, projectName, OUTPUT_DIR, outputFile).normalize().toString();

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

  @Override
  public JSONObject getStatus() {
    JSONObject result = new JSONObject();
    if (this.isFinished) {
      result.put("status", 200);
      result.put("message", "Task finished");
      result.put("finished", true);
    } else {
      result.put("status", 202);
      result.put("message", "Task running");
      result.put("finished", false);
    }
    return result;
  }

}
