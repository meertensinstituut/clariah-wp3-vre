package nl.knaw.meertens.deployment.lib.recipe;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DeploymentResponse;
import nl.knaw.meertens.deployment.lib.DeploymentStatus;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.RecipePluginImpl;
import nl.knaw.meertens.deployment.lib.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.deployment.lib.DeploymentLib.buildInputPath;
import static nl.knaw.meertens.deployment.lib.DeploymentLib.buildOutputFilePath;
import static nl.knaw.meertens.deployment.lib.DeploymentLib.createOutputFolder;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.RUNNING;
import static nl.knaw.meertens.deployment.lib.recipe.Demo.OUTPUT_FILENAME;
import static org.apache.commons.io.FileUtils.writeStringToFile;

public class Tika extends RecipePluginImpl {
  private static Logger logger = LoggerFactory.getLogger(Tika.class);

  private static final String SERVICE_URL = "http://tika:9998/tika/main";

  private String workDir;
  private Service service;
  private DeploymentStatus status;

  @Override
  public void init(String workDir, Service service, String serviceLocation, Stack<HandlerPlugin> handlers)
      throws RecipePluginException {
    super.init(workDir, service, serviceLocation, handlers);
    logger.info(format("init [%s]", workDir));
    this.workDir = workDir;
    this.service = service;
    this.status = DeploymentStatus.CREATED;
  }

  @Override
  public DeploymentResponse execute() throws RecipePluginException {
    logger.info(format("execute [%s][%s]", service.getName(), workDir));
    status = RUNNING;
    runProject(workDir);
    return status.toResponse();
  }

  @Override
  public DeploymentResponse getStatus() {
    return status.toResponse();
  }

  private void runProject(String projectName) throws RecipePluginException {
    String body;
    try {
      var inputContent = retrieveInputContent(projectName);
      var response = Unirest
        .put(SERVICE_URL)
        .header("Accept", "text/plain")
        .header("Content-Type","text/html")
        .field("file", inputContent.toFile())
        .asString();

      if (response.getStatus() != 200) {
        throw new IllegalStateException("response status wasn't 200 but " + response.getStatus());
      }

      body = response.getBody();
    } catch (IllegalStateException | UnirestException ex) {
      throw new RecipePluginException(format("request to [%s] failed", SERVICE_URL), ex);
    }
    var outputFile = buildOutputFilePath(workDir, OUTPUT_FILENAME);
    createOutputFolder(outputFile);

    try {
      writeStringToFile(outputFile.toFile(), body, UTF_8);
      logger.info(format("saved result to [%s]", outputFile.toString()));
    } catch (IOException ex) {
      throw new RecipePluginException(format("could not write output to file [%s]", outputFile), ex);
    }
    status = FINISHED;
  }

  private Path retrieveInputContent(String projectName) throws RecipePluginException {
    var userConfig = DeploymentLib.parseUserConfig(projectName);
    var params = userConfig.get("params");
    var inputFilename = params.get(0).get("value").asText();
    var inputPath = buildInputPath(projectName, inputFilename);
    return Paths.get(inputPath);
  }

}
