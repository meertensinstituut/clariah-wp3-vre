/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.knaw.meertens.deployment.lib;

import net.sf.saxon.s9api.SaxonApiException;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.jdom2.JDOMException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

//import java.nio.charset.Charset;

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
    } catch (InterruptedException e) {
      logger.info("Test service was interrupted.");
      logger.info(e.getLocalizedMessage());
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
  public void init(String wd, Service serviceObj) throws RecipePluginException {
    logger.info("init plugin");
    this.wd = wd;
  }

}
