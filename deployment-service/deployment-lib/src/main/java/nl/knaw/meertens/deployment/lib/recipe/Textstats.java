package nl.knaw.meertens.deployment.lib.recipe;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class Textstats extends RecipePluginImpl {
  private static Logger logger = LoggerFactory.getLogger(Textstats.class);

  private String workDir;
  private Service service;
  private DeploymentStatus status;
  private String serviceLocation;
  private JsonNode params;

  @Override
  public void init(String workDir, Service service, String serviceLocation, Stack<HandlerPlugin> handlers)
    throws RecipePluginException {
    super.init(workDir, service, serviceLocation, handlers);
    logger.info(format("init [%s]", workDir));
    this.workDir = workDir;
    this.service = service;
    this.status = DeploymentStatus.CREATED;

    this.serviceLocation = isBlank(serviceLocation) ? "http://textstats:5000" : serviceLocation;
    this.params = DeploymentLib.parseUserConfig(workDir).get("params");
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

  private void runProject(String workDir) throws RecipePluginException {
    String body;
    try {
      var inputContent = retrieveInputContent(workDir);
      var layer = getParamValueByName(params, "layer");

      var response = Unirest
        .post(serviceLocation)
        .header("Accept", "text/plain")
        .field("layer", layer)
        .field("file", inputContent.toFile())
        .asString();

      if (response.getStatus() != 200) {
        throw new IllegalStateException("response status wasn't 200 but " + response.getStatus());
      }

      body = response.getBody();
    } catch (IllegalStateException | UnirestException ex) {
      throw new RecipePluginException(format("could not request service [%s]", serviceLocation), ex);
    }

    var outputFile = buildOutputFilePath(this.workDir, "result.json");
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
    var inputFilename = getParamValueByName(params, "input");
    var inputPath = buildInputPath(projectName, inputFilename);
    return Paths.get(inputPath);
  }

  private String getParamValueByName(JsonNode params, String name) throws RecipePluginException {
    for (var i = 0; i < params.size(); i++) {
      if (name.equals(params.get(i).get("name").asText())) {
        return params.get(i).get("value").asText();
      }
    }
    throw new RecipePluginException(format("Could not find param value by name [%s] in [%s]", name, params));
  }

}
