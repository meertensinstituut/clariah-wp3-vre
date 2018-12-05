package nl.knaw.meertens.deployment.lib;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.deployment.lib.FileUtil.createFile;
import static nl.knaw.meertens.deployment.lib.SystemConf.INPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.WORK_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.USER_CONF_FILE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class FoliaTest extends AbstractDeploymentTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void init_shouldThrowExceptionWhenEmptyWorkDir() throws RecipePluginException {
    expectedEx.expect(RecipePluginException.class);
    expectedEx.expectMessage("work dir should not be empty");

    Folia folia = new Folia();
    Service service = new Service();
    String workDir = "";
    folia.init(workDir, service);
  }

  @Test
  public void execute_shouldThrowNoWorkDir() throws RecipePluginException {
    expectedEx.expect(RecipePluginException.class);
    expectedEx.expectMessage("work dir does not exist");

    Folia folia = new Folia();
    Service service = new Service();
    String workDir = "test-" + RandomStringUtils.randomAlphanumeric(8);
    folia.init(workDir, service);
    folia.execute();
  }

  @Test
  public void execute_shouldCheckConfigFile() throws RecipePluginException, IOException {
    String workDir = "test-" + RandomStringUtils.randomAlphanumeric(8);
    expectedEx.expect(RecipePluginException.class);
    expectedEx.expectMessage("could not read config file [/tmp/wd/" + workDir + "/config.json]");

    FileUtil.createWorkDir(workDir);

    Folia folia = new Folia();
    Service service = new Service();
    folia.init(workDir, service);
    folia.execute();
  }

  @Test
  public void execute_shouldCreateOutput() throws RecipePluginException, IOException {

    String outputFileName = "example.html";
    String workDir = "test-" + RandomStringUtils.randomAlphanumeric(8);
    FileUtil.createWorkDir(workDir);
    Path configPath = Paths.get(WORK_DIR, workDir, USER_CONF_FILE);

    logger.info("Created config: " + configPath.toString());
    createFile(configPath.toString(), FileUtil.getTestFileContent("config.json"));

    String inputFilename = "example.xml";
    Path inputPath = Paths.get(WORK_DIR, workDir, INPUT_DIR, inputFilename);
    createFile(inputPath.toString(), FileUtil.getTestFileContent(inputFilename));

    Folia folia = new Folia();
    Service service = new Service();
    folia.init(workDir, service);
    folia.execute();
    File outputFile = Paths.get(WORK_DIR, workDir, OUTPUT_DIR, outputFileName).toFile();

    assertThat(outputFile.exists()).isTrue();
    String outputContent = FileUtils.readFileToString(outputFile);
    String expectedOutput = "<span class=\"t\">test</span>";
    assertThat(outputContent).contains(expectedOutput);
  }

}
