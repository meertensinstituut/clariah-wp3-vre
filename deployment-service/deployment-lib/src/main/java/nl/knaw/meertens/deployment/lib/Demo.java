package nl.knaw.meertens.deployment.lib;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static nl.knaw.meertens.deployment.lib.SystemConf.INPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.SYSTEM_DIR;

public class Demo implements RecipePlugin {
  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private String projectName;
  private String serviceUrl;
  private boolean isFinished;

  @Override
  public void init(String workDir, Service service) throws RecipePluginException {
    logger.info("init Demo plugin");
    this.projectName = workDir;
    this.serviceUrl = "https://tools.digitalmethods.net/beta/deduplicate/";
    logger.info("finish init Demo plugin");
  }

  @Override
  public JSONObject execute() throws RecipePluginException {
    logger.info("Start plugin execution");

    JSONObject json = new JSONObject();
    json.put("key", projectName);
    json.put("status", 202);
    JSONObject userConfig = new JSONObject();
    try {
      userConfig = DeploymentLib.parseUserConfig(projectName);
      logger.info("userConfig: ");
      logger.info(userConfig.toJSONString());

      logger.info("Running project");
      this.runProject(projectName);

      this.isFinished = true;

    } catch (IOException ex) {
      logger.error(String.format("Execution ERROR: {%s}", ex.getLocalizedMessage()), ex);
    } catch (UnirestException e) {
      throw new RecipePluginException("Request to url failed: " + serviceUrl);
    }

    return json;
  }

  private void runProject(String projectName) throws IOException, UnirestException {
    JSONObject userConfig = DeploymentLib.parseUserConfig(projectName);
    JSONArray params = (JSONArray) userConfig.get("params");

    JSONObject inputOjbect = (JSONObject) params.get(0);
    String inputFile = (String) inputOjbect.get("value");
    String inputPath = Paths.get(SYSTEM_DIR, projectName, INPUT_DIR).normalize().toString();
    String fullInputPath = Paths.get(SYSTEM_DIR, projectName, INPUT_DIR, inputFile).normalize().toString();
    logger.info(String.format("Full inputPath: %s", fullInputPath));
    logger.info(String.format("inputPath: %s", inputPath));

    String content = new String(Files.readAllBytes(Paths.get(fullInputPath)));
    HttpResponse<String> responseString = Unirest
        .post(serviceUrl.toString())
        .header("accept", "application/json")
        .header("MIME Type", "application/x-www-form-urlencoded")
        .body(content)
        .asString();


    JSONObject outputOjbect;
    String outputFile;
    if (params.size() > 1) {
      outputOjbect = (JSONObject) params.get(1);
      outputFile = (String) outputOjbect.get("value");
    } else {
      outputFile = inputFile;
    }

    String outputPath = Paths.get(SYSTEM_DIR, projectName, OUTPUT_DIR, outputFile).normalize().toString();

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
      fileWriter.write(responseString.getBody());
      fileWriter.write("</pre>");
      fileWriter.flush();
    }

  }

  @Override
  public JSONObject getStatus() throws RecipePluginException {
    return null;
  }
}
