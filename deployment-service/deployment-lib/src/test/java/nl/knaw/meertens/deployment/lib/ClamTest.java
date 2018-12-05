package nl.knaw.meertens.deployment.lib;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.meertens.deployment.lib.FileUtil.createFile;
import static nl.knaw.meertens.deployment.lib.FileUtil.getTestFileContent;
import static nl.knaw.meertens.deployment.lib.SystemConf.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ClamTest extends AbstractDeploymentTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void init_shouldGetSemantics() throws RecipePluginException {
    Clam clam = new Clam();
    String workDir = "test-" + RandomStringUtils.randomAlphanumeric(8);

    final String serviceSemantics = FileUtil.getTestFileContent("ucto.xml");
    Service service = new Service("0", "UCTO", "CLAM", serviceSemantics, "<xml></xml>", true);

    clam.init(workDir, service);
  }

  @Test
  public void execute_shouldExecute() throws RecipePluginException, IOException {
    // create work dir:
    String workDir = "test-" + RandomStringUtils.randomAlphanumeric(8);
    FileUtil.createWorkDir(workDir);
    logger.info(String.format("create workdir [%s]", workDir));

    // create config file:
    Path configPath = Paths.get(WORK_DIR, workDir, USER_CONF_FILE);
    String testFileContent = FileUtil.getTestFileContent("configUcto.json");
    createFile(configPath.toString(), testFileContent);

    // create input file:
    String inputFilename = "ucto.txt";
    Path inputPath = Paths.get(WORK_DIR, workDir, INPUT_DIR, inputFilename);
    createFile(inputPath.toString(), FileUtil.getTestFileContent(inputFilename));

    // instantiate recipe:
    final String serviceSemantics = FileUtil.getTestFileContent("ucto.xml");
    Service service = new Service("0", "UCTO", "CLAM", serviceSemantics, "<xml></xml>", true);
    Clam clam = new Clam();
    clam.init(workDir, service);

    // mock service calls:
    createProjectMock(workDir, 1);
    getClamFilesAndAccessKeyMock(workDir);
    fileUploadMock(workDir, 1);
    runProjectMock(workDir, 1);
    pollProjectMock(workDir, 1);
    downloadFileMock(workDir, 1);
    downloadLogMock(workDir, 1);
    downloadErrorLogMock(workDir, 1);

    clam.execute();

    // assert output file exists:
    String outputFilename = "uctoOutput.xml";
    Path outputFile = Paths.get(WORK_DIR, workDir, OUTPUT_DIR, outputFilename);
    logger.info("output path expected: " + outputFile.toString());
    boolean outputExists = outputFile.toFile().exists();
    assertThat(outputExists).isTrue();
  }

  private void createProjectMock(String workDir, int times) {
    mockServer
        .when(
            request()
                .withMethod("PUT")
                .withPath("/ucto/" + workDir),
            Times.exactly(times)
        )
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                .withBody("Project " + workDir + " has been created for user anonymous")
        );
  }

  private void getClamFilesAndAccessKeyMock(String workDir) {
    String testFileContent = getTestFileContent("clamFileList.xml")
      .replace("testproject", workDir);

    mockServer
        .when(
            request()
                .withMethod("GET")
                .withPath("/ucto/" + workDir)
        )
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/xml; charset=utf-8"))
                .withBody(testFileContent)
        );
  }

  private void fileUploadMock(String workDir, int times) {
    mockServer
        .when(
            request()
                .withMethod("POST")
                .withPath("/ucto/" + workDir + "/input/ucto.txt"),
            Times.exactly(times)
        )
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/xml; charset=utf-8"))
                .withBody("")
        );
  }

  private void runProjectMock(String workDir, int times) {
    mockServer
        .when(
            request()
                .withMethod("POST")
                .withPath("/ucto/" + workDir + "/"),
            Times.exactly(times)
        )
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/xml; charset=utf-8"))
                .withBody("")
        );
  }

  private void pollProjectMock(String workDir, int times) {
    mockServer
        .when(
            request()
                .withMethod("GET")
                .withPath("/ucto/" + workDir + "/status/"),
            Times.exactly(times)
        )
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/xml; charset=utf-8"))
                .withBody(getTestFileContent("uctoPollProject.json"))
        );
  }

  private void downloadFileMock(String workDir, int times) {
    mockServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ucto/" + workDir + "/output/uctoOutput.xml"),
        Times.exactly(times)
      )
      .respond(
        response()
          .withStatusCode(200)
          .withHeaders(new Header("Content-Type", "application/xml; charset=utf-8"))
          .withBody(getTestFileContent("uctoResult.xml"))
      );
  }
  private void downloadLogMock(String workDir, int times) {
    mockServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ucto/" + workDir + "/output/log"),
        Times.exactly(times)
      )
      .respond(
        response()
          .withStatusCode(200)
          .withHeaders(new Header("Content-Type", "application/xml; charset=utf-8"))
          .withBody(getTestFileContent("log.txt"))
      );
  }
  private void downloadErrorLogMock(String workDir, int times) {
    mockServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ucto/" + workDir + "/output/error.log"),
        Times.exactly(times)
      )
      .respond(
        response()
          .withStatusCode(200)
          .withHeaders(new Header("Content-Type", "application/xml; charset=utf-8"))
          .withBody(getTestFileContent("error.log"))
      );
  }

}