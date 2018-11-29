package nl.knaw.meertens.deployment.lib;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.meertens.deployment.lib.FileUtil.createFile;
import static nl.knaw.meertens.deployment.lib.FileUtil.getTestFileContent;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ClamTest {
  @Rule
  public ExpectedException expectedEx = ExpectedException.none();
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String mockHostName = "http://localhost:1080";
  private static ClientAndServer mockServer;

  @BeforeClass
  public static void setUp() {
    mockServer = ClientAndServer.startClientAndServer(1080);
  }

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
    String workDir = "test-" + RandomStringUtils.randomAlphanumeric(8);

    Clam clam = new Clam();
    FileUtil.createWorkDir(workDir);
    Path configPath = Paths.get(SystemConf.systemWorkDir, workDir, SystemConf.userConfFile);

    logger.info("Created config: " + configPath.toString());
    createFile(configPath.toString(), FileUtil.getTestFileContent("configUcto.json"));

    String inputFilename = "ucto.txt";
    Path inputPath = Paths.get(SystemConf.systemWorkDir, workDir, SystemConf.inputDirectory, inputFilename);
    createFile(inputPath.toString(), FileUtil.getTestFileContent(inputFilename));

    final String serviceSemantics = FileUtil.getTestFileContent("ucto.xml");
    Service service = new Service("0", "UCTO", "CLAM", serviceSemantics, "<xml></xml>", true);

    clam.init(workDir, service);

    createProjectMock(workDir, 1);
    createAccessKeyMock(workDir);
    fileUploadMock(workDir, 1);
    runProjectMock(workDir, 1);
    pollProjectMock(workDir, 1);
    fileDownloadMock(workDir, 1);

    clam.execute();
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

  private void createAccessKeyMock(String workDir) {
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
                .withBody(getTestFileContent("uctoAccessToken.xml"))
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

  private void fileDownloadMock(String workDir, int times) {
    mockServer
        .when(
            request()
                .withMethod("GET")
                .withPath("/ucto/" + workDir),
            Times.exactly(times)
        )
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(new Header("Content-Type", "application/xml; charset=utf-8"))
                .withBody(getTestFileContent("uctoResult.xml"))
        );
  }

}