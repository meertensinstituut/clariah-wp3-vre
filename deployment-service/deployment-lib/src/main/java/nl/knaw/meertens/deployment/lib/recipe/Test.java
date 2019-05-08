package nl.knaw.meertens.deployment.lib.recipe;

import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DeploymentResponse;
import nl.knaw.meertens.deployment.lib.DeploymentStatus;
import nl.knaw.meertens.deployment.lib.HandlerPlugin;
import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.RecipePluginImpl;
import nl.knaw.meertens.deployment.lib.Service;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.RUNNING;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;

public class Test extends RecipePluginImpl {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private DeploymentStatus status;
  private String workDir;

  @Override
  public void init(String workDir, Service service, String serviceLocation, Stack<HandlerPlugin> handlers)
      throws RecipePluginException {
    super.init(workDir, service, serviceLocation, handlers);
    logger.info(format("init [%s]", workDir));
    this.workDir = workDir;
    this.status = DeploymentStatus.CREATED;
  }

  @Override
  public DeploymentResponse execute() {
    logger.info(format("execute [%s]", workDir));
    this.status = RUNNING;
    new Thread(this::finishDeployment).start();
    return status.toResponse();
  }

  @Override
  public DeploymentResponse getStatus() {
    return status.toResponse();
  }

  private void finishDeployment() {
    try {
      logger.info("wait 15 seconds");
      TimeUnit.SECONDS.sleep(15);
    } catch (InterruptedException ex) {
      logger.error("test deployment was interrupted.", ex);
    }

    Path outputFile = Paths.get(
      ROOT_WORK_DIR,
      workDir,
      "/output/result.txt"
    );

    logger.info(format("create outputFile [%s]", outputFile.toString()));
    outputFile.toFile().getParentFile().mkdirs();
    try {
      String sentence = "Insanity: doing the same thing over and over again and expecting different results.";
      FileUtils.write(outputFile.toFile(), sentence, UTF_8);
    } catch (IOException ex) {
      logger.error(format("could not generate output for [%s]", workDir), ex);
    }
    this.status = FINISHED;
  }

}
