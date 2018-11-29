package nl.knaw.meertens.deployment.lib;


import org.apache.commons.configuration.ConfigurationException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import static java.util.Objects.isNull;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class Folia implements RecipePlugin {
  private static Logger logger = LoggerFactory.getLogger(RecipePlugin.class);
  private Boolean isFinished = false;
  private String workDir;

  private URL url;

  @Override
  public void init(String workDir, Service service) throws RecipePluginException {

    try {
      url = new URL("https://raw.githubusercontent.com/" +
        "proycon/folia/master/foliatools/folia2html.xsl");
    } catch (MalformedURLException e) {
      throw new RecipePluginException("Could not load xslt from url", e);
    }

    logger.info("init Folia plugin");
    if (isEmpty(workDir)) {
      throw new RecipePluginException("work dir should not be empty");
    }
    this.workDir = workDir;
  }

  @Override
  public JSONObject execute() throws RecipePluginException {
    logger.info("Start plugin execution");

    JSONObject json = new JSONObject();
    json.put("key", workDir);
    json.put("status", 202);
    JSONObject userConfig;
    try {
      DeploymentLib.workDirExists(workDir);

      userConfig = new DeploymentLib().parseUserConfig(workDir);
      logger.info("userConfig: ");
      logger.info(userConfig.toJSONString());

      logger.info("Running project");
      this.runProject(workDir);

      // keep polling project
      logger.info("Polling the service");
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

    } catch (ConfigurationException | IOException | InterruptedException ex) {
      throw new RecipePluginException(ex.getMessage(), ex);
    }

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

  private static void convertXmlToHtml(Source xml, Source xslt, File file) {
    StringWriter sw = new StringWriter();

    try {
      FileWriter fw = new FileWriter(file.getPath());
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer trasform = transformerFactory.newTransformer(xslt);
      trasform.transform(xml, new StreamResult(sw));
      fw.write(sw.toString());
      fw.close();
      logger.info("Generated html from xml successfully");
    } catch (IOException | TransformerFactoryConfigurationError | TransformerException e) {
      logger.error("Could not convert xml to html", e);
    }
  }

  private JSONObject runProject(String key) throws IOException, ConfigurationException {
    final String outputPathConst = "output";
    final String inputPathConst = "input";

    DeploymentLib dplib = new DeploymentLib();

    JSONObject userConfig = dplib.parseUserConfig(key);
    if (userConfig.isEmpty()) {
      throw new IOException("No config file");
    }
    logger.info("userConfig: " + userConfig.toJSONString());
    JSONArray params = (JSONArray) userConfig.get("params");
    if (isNull(params)) {
      throw new IOException("No params");
    }
    JSONObject inputOjbect = (JSONObject) params.get(0);
    String inputFile = (String) inputOjbect.get("value");

    String workDir = SystemConf.systemWorkDir;
    String fullInputPath = Paths.get(workDir, this.workDir, inputPathConst, inputFile).normalize().toString();
    String inputPath = Paths.get(workDir, this.workDir, inputPathConst).normalize().toString();
    logger.info(String.format("inputPath: %s", inputPath));
    logger.info(String.format("Full Input Path: %s", fullInputPath));

    JSONObject outputOjbect;
    String outputFile;
    if (params.size() > 1) {
      outputOjbect = (JSONObject) params.get(1);
      outputFile = (String) outputOjbect.get("value");
    } else {
      outputFile = inputFile;
    }

    String outputPath = Paths.get(workDir, this.workDir, outputPathConst).normalize().toString();
    String fullOutputPath = Paths.get(workDir, this.workDir, outputPathConst, outputFile).normalize().toString();
    logger.info(String.format("outputPath: %s", outputPath));
    logger.info(String.format("Full outputPath: %s", fullOutputPath));

    File outputPathAsFile = new File(Paths.get(fullOutputPath).getParent().normalize().toString());
    if (!outputPathAsFile.exists()) {
      logger.info(String.format("Creating folder: %s", outputPathAsFile.toString()));
      outputPathAsFile.mkdirs();
    }

    Source xslt = new StreamSource(url.openStream());
    Source xml = new StreamSource(new File(fullInputPath));
    File file = new File(fullOutputPath);
    convertXmlToHtml(xml, xslt, file);

    JSONObject json = new JSONObject();
    return json;

  }

}
