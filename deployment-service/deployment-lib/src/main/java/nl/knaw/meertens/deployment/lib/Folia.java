package nl.knaw.meertens.deployment.lib;


import net.sf.saxon.s9api.SaxonApiException;
import org.apache.commons.configuration.ConfigurationException;
import org.jdom2.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Paths;

//import java.util.Iterator;

/**
 * @author Vic
 */
public class Folia implements RecipePlugin {
  private static Logger logger = LoggerFactory.getLogger(RecipePlugin.class);
  public URL serviceUrl;
  protected int counter = 0;
  protected Boolean isFinished = false;
  protected Boolean userConfigRemoteError = false;
  protected String projectName;

  public static void convertXmlToHtml(Source xml, Source xslt, File file) {
    StringWriter sw = new StringWriter();

    try {

      FileWriter fw = new FileWriter(file.getPath());
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer trasform = transformerFactory.newTransformer(xslt);
      trasform.transform(xml, new StreamResult(sw));
      fw.write(sw.toString());
      fw.close();

      logger.info("### Generated successfully! ###");

    } catch (IOException | TransformerConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerFactoryConfigurationError e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }
  }

  /**
   * @param projectName Project name also known as key, project id and working directory
   * @param service     Service record in the service registry
   * @throws JDOMException     Invalid DOM object
   * @throws IOException       Disk I/O Exception
   * @throws SaxonApiException Saxon API Exception
   */
  @Override
  public void init(String projectName, Service service) throws RecipePluginException {
    logger.info("init Text plugin");
    this.projectName = projectName;
    this.serviceUrl = null;
    logger.info("finish init Text plugin");

  }

  @Override
  public JSONObject execute() throws RecipePluginException {
    logger.info("## Start plugin execution ##");

    JSONObject json = new JSONObject();
    json.put("key", projectName);
    json.put("status", 202);
    JSONObject userConfig = new JSONObject();
    try {
      userConfig = new DeploymentLib().parseUserConfig(projectName);
      logger.info("## userConfig:  ##");
      logger.info(userConfig.toJSONString());

      logger.info("## Running project ##");
      this.runProject(projectName);

      // keep polling project
      logger.info("## Polling the service ##");
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
      throw new RecipePluginException(String.format("## Execution ERROR: {%s}", ex.getLocalizedMessage()));
    }

    return json;
  }

  public JSONObject runProject(String key) throws IOException, ConfigurationException {
    final String outputPathConst = "output";
    final String inputPathConst = "input";

    DeploymentLib dplib = new DeploymentLib();

    String workDir = dplib.getWd();
    JSONObject userConfig = dplib.parseUserConfig(key);
    JSONArray params = (JSONArray) userConfig.get("params");

    JSONObject inputOjbect = (JSONObject) params.get(0);
    String inputFile = (String) inputOjbect.get("value");
    String fullInputPath = Paths.get(workDir, projectName, inputPathConst, inputFile).normalize().toString();
    String inputPath = Paths.get(workDir, projectName, inputPathConst).normalize().toString();
    logger.info(String.format("### inputPath: %s ###", inputPath));
    logger.info(String.format("### Full Input Path: %s ###", fullInputPath));

    JSONObject outputOjbect;
    String outputFile;
    if (params.size() > 1) {
      outputOjbect = (JSONObject) params.get(1);
      outputFile = (String) outputOjbect.get("value");
    } else {
      outputFile = inputFile;
    }

    String outputPath = Paths.get(workDir, projectName, outputPathConst).normalize().toString();
    String fullOutputPath = Paths.get(workDir, projectName, outputPathConst, outputFile).normalize().toString();
    logger.info(String.format("### outputPath: %s ###", outputPath));
    logger.info(String.format("### Full outputPath: %s ###", fullOutputPath));

    File outputPathAsFile = new File(Paths.get(fullOutputPath).getParent().normalize().toString());
    if (!outputPathAsFile.exists()) {
      logger.info(String.format("### Creating folder: %s ###", outputPathAsFile.toString()));
      outputPathAsFile.mkdirs();
    }

    URL url = new URL("https://raw.githubusercontent.com/proycon/folia/master/foliatools/folia2html.xsl");
    Source xslt = new StreamSource(url.openStream());
    Source xml = new StreamSource(new File(fullInputPath));
    File file = new File(fullOutputPath);
    convertXmlToHtml(xml, xslt, file);

    JSONObject json = new JSONObject();
    return json;

  }

  /**
   * project id also known as project name, key and working directory
   */
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

}
