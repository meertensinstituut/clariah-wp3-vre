package nl.knaw.meertens.deployment.lib.recipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.knaw.meertens.deployment.lib.DeploymentResponse;
import nl.knaw.meertens.deployment.lib.DeploymentStatus;
import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
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

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.deployment.lib.DeploymentLib.parseUserConfig;
import static nl.knaw.meertens.deployment.lib.DeploymentLib.workDirExists;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.deployment.lib.SystemConf.INPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class Folia implements RecipePlugin {
  private static Logger logger = LoggerFactory.getLogger(RecipePlugin.class);
  private DeploymentStatus status;
  private String workDir;

  private URL url;

  @Override
  public void init(String workDir, Service service) throws RecipePluginException {

    try {
      url = new URL(
          "https://gist.githubusercontent.com/vicding-mi/a2f1733240867433b4fdb24ca3fa4e29/raw" +
              "/6aeeb575bd55d0b048de5a6c70b946f76a4a28e8/folia2html.xsl");
    } catch (MalformedURLException e) {
      throw new RecipePluginException("Could not load xslt from url", e);
    }

    logger.info(format("init folia plugin in workDir [%s]", workDir));
    if (isEmpty(workDir)) {
      throw new RecipePluginException("work dir should not be empty");
    }
    this.workDir = workDir;
    this.status = DeploymentStatus.CREATED;
  }

  @Override
  public DeploymentResponse execute() throws RecipePluginException {
    logger.info(format("execute [%s]", workDir));

    // TODO: userConfig should be used
    ObjectNode userConfig;
    try {
      workDirExists(workDir);

      logger.info(format("run [%s]", workDir));
      this.runProject(workDir);

      // keep polling project
      boolean ready = false;
      int counter = 0;

      // TODO: create polling service
      while (!ready) {
        logger.info(format("poll [%s]", workDir));
        counter++;
        Thread.sleep(3000);
        // TODO: check if output file exists
        ready = true;
      }

      this.status = FINISHED;

    } catch (IOException | InterruptedException ex) {
      throw new RecipePluginException(ex.getMessage(), ex);
    }

    return status.toResponse();
  }

  @Override
  public DeploymentResponse getStatus() {
    return status.toResponse();
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

  private void runProject(String key) throws IOException, RecipePluginException {
    ObjectNode userConfig = parseUserConfig(key);
    if (userConfig.isEmpty(null)) {
      throw new IOException("No config file");
    }
    JsonNode params = userConfig.get("params");
    if (isNull(params)) {
      throw new IOException("No params");
    }
    ObjectNode inputOjbect = (ObjectNode) params.get(0);
    String inputFile = inputOjbect.get("value").asText();

    String inputPath = Paths
        .get(ROOT_WORK_DIR, workDir, INPUT_DIR, inputFile)
        .normalize().toString();

    String outputFile;
    if (params.size() > 1) {
      outputFile = params.get(1).get("value").asText();
    } else {
      outputFile = inputFile;
    }

    String fullOutputPath = Paths
        .get(ROOT_WORK_DIR, workDir, OUTPUT_DIR, outputFile)
        .normalize().toString();

    File outputPath = Paths
        .get(fullOutputPath).getParent()
        .normalize().toFile();

    if (!outputPath.exists()) {
      logger.info(format("creating folder [%s]", outputPath.toString()));
      outputPath.mkdirs();
    }

    Source xslt = new StreamSource(url.openStream());
    Source xml = new StreamSource(new File(inputPath));
    File file = new File(fullOutputPath);
    convertXmlToHtml(xml, xslt, file);
  }

}
