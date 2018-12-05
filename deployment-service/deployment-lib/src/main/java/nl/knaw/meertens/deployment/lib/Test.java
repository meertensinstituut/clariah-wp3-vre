package nl.knaw.meertens.deployment.lib;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author vic
 */
public class Test implements RecipePlugin {
  protected int counter = 0;
  protected Boolean isFinished = false;

  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private String wd;

  @Override
  public JSONObject execute() throws RecipePluginException {
    logger.info(String.format("Create TEST deployment with workDir [%s]", wd));
    try {
      logger.info("Start 15 second run...");
      Thread.sleep(15000);
    } catch (InterruptedException ex) {
      logger.info("Test service was interrupted.", ex);
    }
    Path outputFile = Paths.get("/tmp/wd/" + wd + "/output/result.txt");
    outputFile.toFile().getParentFile().mkdirs();
    logger.info(String.format("Creating outputFile [%s]", outputFile.toString()));
    try {
      String sentence = "Insanity: doing the same thing over and over again and expecting different results.";
      FileUtils.write(outputFile.toFile(), sentence, Charsets.UTF_8);
      logger.info(String.format("Created outputFile [%s]", outputFile.toString()));
    } catch (IOException e) {
      logger.info(String.format("Could not generate output for [%s]", wd));
      logger.info(e.getLocalizedMessage());
    }
    this.isFinished = true;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("status", "OK");
    return jsonObject;
  }

  @Override
  public JSONObject getStatus() throws RecipePluginException {
    JSONObject json = new JSONObject();
    json.put("key", wd);
    json.put("finished", isFinished);
    json.put("success", isFinished);
    if (isFinished) {
      json.put("completion", 100L);
    } else {
      json.put("completion", 80L);
    }
    return json;
  }

  @Override
  public void init(String workDir, Service service) throws RecipePluginException {
    logger.info("init plugin");
    this.wd = workDir;
  }

}
