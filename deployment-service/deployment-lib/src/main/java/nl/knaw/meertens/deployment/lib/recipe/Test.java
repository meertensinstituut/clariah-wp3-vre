package nl.knaw.meertens.deployment.lib.recipe;

import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.Service;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static nl.knaw.meertens.deployment.lib.DeploymentLib.createDefaultStatus;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;

/**
 * @author vic
 */
public class Test implements RecipePlugin {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private Boolean isFinished = false;
  private String workDir;

  @Override
  public void init(String workDir, Service service) {
    logger.info(format("init [%s]", workDir));
    this.workDir = workDir;
  }

  @Override
  public JSONObject execute() {
    logger.info(format("execute [%s]", workDir));
    new Thread(this::finishDeployment).start();
    return createDefaultStatus(isFinished);
  }

  @Override
  public JSONObject getStatus() {
    return createDefaultStatus(isFinished);
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
      FileUtils.write(outputFile.toFile(), sentence, Charsets.UTF_8);
    } catch (IOException ex) {
      logger.error(format("could not generate output for [%s]", workDir), ex);
    }
    this.isFinished = true;
  }

}
