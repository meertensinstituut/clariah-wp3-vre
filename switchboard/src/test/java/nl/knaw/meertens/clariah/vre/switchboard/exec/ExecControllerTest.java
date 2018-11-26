package nl.knaw.meertens.clariah.vre.switchboard.exec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import nl.knaw.meertens.clariah.vre.switchboard.util.DeployUtil;
import nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.NEXTCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamType.FILE;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.createResultFile;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.createTestFileWithRegistryObject;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.never;

public class ExecControllerTest extends AbstractControllerTest {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private String dummyViewerService = "{\n" +
    "      \"id\": \"1\",\n" +
    "      \"name\": \"VIEWER\",\n" +
    "      \"kind\": \"viewer\",\n" +
    "      \"recipe\": \"nl.knaw.meertens.deployment.lib.Test\",\n" +
    "      \"semantics\": \"<cmd:CMD xmlns:cmd=\\\"http://www.clarin.eu/cmd/1\\\" xmlns:cmdp=\\\"http://www.clarin" +
    ".eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795\\\" xmlns:xs=\\\"http://www.w3.org/2001/XMLSchema\\\" " +
    "xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:schemaLocation=\\\"\\n  http://www.clarin" +
    ".eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd\\n  http://www.clarin.eu/cmd/1/profiles/clarin" +
    ".eu:cr1:p_1505397653795 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin" +
    ".eu:cr1:p_1505397653795/xsd\\\" CMDVersion=\\\"1.2\\\">\\n  <cmd:Header>\\n    " +
    "<cmd:MdCreationDate>2018-05-28</cmd:MdCreationDate>\\n    <cmd:MdProfile>clarin" +
    ".eu:cr1:p_1505397653795</cmd:MdProfile><!-- profile is fixed -->\\n  </cmd:Header>\\n  <cmd:Resources>\\n    " +
    "<cmd:ResourceProxyList/>\\n    <cmd:JournalFileProxyList/>\\n    <cmd:ResourceRelationList/>\\n  " +
    "</cmd:Resources>\\n  <cmd:Components>\\n    <cmdp:CLARINWebService>\\n      <cmdp:Service CoreVersion=\\\"1" +
    ".0\\\">\\n        <cmdp:Name>Test</cmdp:Name>\\n        <cmdp:Description>Service to test deployment mechanism " +
    "of VRE</cmdp:Description>\\n        <cmdp:ServiceDescriptionLocation/> <!-- test doesn't really run remote " +
    "-->\\n        <cmdp:Operations>\\n          <cmdp:Operation>\\n            <cmdp:Name>main</cmdp:Name><!-- main " +
    "is our default endpoint -->\\n            <cmdp:Input>\\n              <cmdp:Parameter><!-- use Parameter " +
    "instead of ParameterGroup, if there are no nested parameters -->\\n                " +
    "<cmdp:Name>input</cmdp:Name>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              " +
    "</cmdp:Parameter>\\n            </cmdp:Input>\\n            <cmdp:Output>\\n              <cmdp:Parameter>\\n   " +
    "             <cmdp:Name>output</cmdp:Name>\\n                <cmdp:Description>Surprise</cmdp:Description>\\n   " +
    "             <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            " +
    "</cmdp:Output>\\n          </cmdp:Operation>\\n        </cmdp:Operations>\\n      </cmdp:Service>\\n    " +
    "</cmdp:CLARINWebService>\\n  </cmd:Components>\\n</cmd:CMD>\",\n" +
    "      \"tech\": null,\n" +
    "      \"time_created\": \"2018-05-28 12:34:48.863548+00\",\n" +
    "      \"time_changed\": null,\n" +
    "      \"mimetype\": \"text/plain\"\n" +
    "    }";

  @Test
  public void getHelp() {
    var response = target("exec")
      .request()
      .get();

    assertThat(response.getStatus()).isEqualTo(200);
    var json = response.readEntity(String.class);
    assertThatJson(json).node("msg").matches(containsString("readme"));
  }

  @Test
  public void postDeploymentRequest_shouldCreateSymbolicLinksToInputFiles() throws Exception {
    var object = createTestFileWithRegistryObject(resultSentence);
    var uniqueTestFile = object.filepath;

    var deploymentRequestDto = DeployUtil.getDeploymentRequestDto("" + object.id, longName);
    var expectedService = "UCTO";

    var deployed = deploy(expectedService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    var request = target(String.format("exec/task/%s/", workDir)).request();

    MockServerUtil.startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", "UCTO");
    var response = DeployUtil.waitUntil(request, FINISHED);

    assertThat(Paths.get(DEPLOYMENT_VOLUME, workDir, INPUT_DIR, uniqueTestFile).toFile()).exists();
    createResultFile(workDir, resultFilename, resultSentence);
    assertThatJson(response).node("status").isEqualTo("FINISHED");

    // Atm links are kept:
    assertThat(Paths.get(DEPLOYMENT_VOLUME, workDir, INPUT_DIR, uniqueTestFile).toFile()).exists();
  }

  @Test
  public void postDeploymentRequest_shouldOutputFolderWithTestResult() throws InterruptedException, IOException {
    MockServerUtil.startServicesRegistryMockServer(dummyUctoService);

    var object = createTestFileWithRegistryObject(resultSentence);
    var deploymentRequestDto = DeployUtil.getDeploymentRequestDto("" + object.id, longName);
    var expectedService = "UCTO";
    var deployed = deploy(expectedService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    var request = target(String.format("exec/task/%s/", workDir)).request();

    MockServerUtil.startOrUpdateStatusMockServer(RUNNING.getHttpStatus(), workDir, "{}", "UCTO");
    DeployUtil.waitUntil(request, RUNNING);

    MockServerUtil.startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", "UCTO");
    MockServerUtil.startServicesRegistryMockServer(dummyUctoService);
    createResultFile(workDir, resultFilename, resultSentence);
    var finishedJson = DeployUtil.waitUntil(request, FINISHED);

    // Check output file is moved:
    var outputFolder = findOutputFolder(finishedJson);
    assertThat(outputFolder).isNotNull();
    assertThat(outputFolder.toString()).startsWith("/usr/local/nextcloud/admin/files/output-20");
    var outputFile = Paths.get(outputFolder.getPath(), resultFilename);
    assertThat(outputFile.toFile()).exists();
    assertThat(Files.readAllLines(outputFile).get(0)).isEqualTo(resultSentence);
  }

  @Test
  public void postDeploymentRequest_shouldCreateConfigFile() throws IOException {
    var object = createTestFileWithRegistryObject(resultSentence);
    var uniqueTestFile = object.filepath;

    var deploymentRequestDto = DeployUtil.getDeploymentRequestDto("" + object.id, longName);
    var expectedService = "UCTO";

    var deployed = deploy(expectedService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    var configFile = Paths.get(DEPLOYMENT_VOLUME, workDir, CONFIG_FILE_NAME);
    assertThat(configFile.toFile()).exists();
    var configJson = new String(Files.readAllBytes(configFile));
    var config = new ObjectMapper().readValue(configJson, ConfigDto.class);

    assertThat(config.params.get(0).value).contains(uniqueTestFile);
    assertThat(config.params.get(0).name).isEqualTo("untokinput");

    assertThat(config.params.get(0).type).isEqualTo(FILE);
    assertThatJson(configJson).node("params[0].type").isEqualTo("file");

    // sub params:
    assertThat(config.params.get(0).params.size()).isEqualTo(2);
    config.params.get(0).params.forEach(p -> {
      assertThat(p.name).isIn("language", "author");
      assertThat(p.value).isIn("eng", longName);
    });
  }

  @Test
  public void testFinishRequest_shouldIgnoreUnknownFields() throws InterruptedException, IOException {
    var deploymentRequestDto = DeployUtil.getDeploymentRequestDto("1", longName);
    var expectedService = "UCTO";
    var deployed = deploy(expectedService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    var request = target(String.format("exec/task/%s/", workDir)).request();
    createResultFile(workDir, resultFilename, resultSentence);
    MockServerUtil.startOrUpdateStatusMockServer(
      FINISHED.getHttpStatus(),
      workDir,
      "{\"finished\":false,\"id\":\"" + workDir + "\",\"key\":\"" + workDir + "\", \"blarpiness\":\"100%\"}",
      "UCTO"
    );

    // Check status is finished:
    String finishedResponse = DeployUtil.waitUntil(request, FINISHED);
    assertThatJson(finishedResponse).node("status").isEqualTo("FINISHED");
  }

  @Test
  public void postDeploymentRequest_shouldMoveViewerOutputFileToViewerFolder()
    throws IOException, InterruptedException {
    MockServerUtil.getMockServer().reset();
    MockServerUtil.startServicesRegistryMockServer(dummyViewerService);

    // create file and dummy registry object:
    var viewerService = "VIEWER";
    MockServerUtil.startDeployMockServer(viewerService, 200);
    var object = createTestFileWithRegistryObject(resultSentence);
    var inputPath = Paths.get(object.filepath);
    var expectedOutputPath = "admin/files/.vre/VIEWER/" + inputPath.subpath(2, inputPath.getNameCount());

    // request deployment:
    var deploymentRequestDto = DeployUtil.getViewerDeploymentRequestDto("" + object.id);
    var deployed = deploy(viewerService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    // check output param in config:
    Path configFile = Paths.get(DEPLOYMENT_VOLUME, workDir, CONFIG_FILE_NAME);
    assertThat(configFile.toFile()).exists();
    var configJson = new String(Files.readAllBytes(configFile));
    var config = new ObjectMapper().readValue(configJson, ConfigDto.class);
    assertThat(config.params.get(1).name).isEqualTo("output");
    assertThat(config.params.get(1).value).contains(inputPath.toString());

    // finish deployment:
    var request = target(String.format("exec/task/%s/", workDir)).request();
    MockServerUtil.startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", viewerService);
    createResultFile(workDir, object.filepath, "<pre>" + resultSentence + "</pre>");
    var finishedJson = DeployUtil.waitUntil(request, FINISHED);

    // check output path:
    String viewerFile = JsonPath.parse(finishedJson).read("$.viewerFile");
    assertThat(viewerFile).isEqualTo(expectedOutputPath);

    // viewer file content:
    var viewerFilePath = Paths.get(NEXTCLOUD_VOLUME, viewerFile);
    assertThat(viewerFilePath.toFile()).exists();
    var viewerFileContent = FileUtils.readFileToString(viewerFilePath.toFile(), UTF_8);
    assertThat(viewerFileContent).contains("<pre>");
    assertThat(viewerFileContent).contains("Insanity");
    assertThat(viewerFileContent).contains("</pre>");
    String viewerFileContentInJson = JsonPath.parse(finishedJson).read("$.viewerFileContent");
    assertThat(viewerFileContentInJson).isEqualTo(viewerFileContent);
  }

  @Test
  public void postDeploymentRequest_shouldNotCreateKafkaMsg_whenViewerService()
    throws IOException, InterruptedException {
    MockServerUtil.getMockServer().reset();
    MockServerUtil.startServicesRegistryMockServer(dummyViewerService);

    // create file and dummy registry object:
    var viewerService = "VIEWER";
    MockServerUtil.startDeployMockServer(viewerService, 200);
    var object = createTestFileWithRegistryObject(resultSentence);
    var inputPath = Paths.get(object.filepath);
    var expectedOutputPath = "admin/files/.vre/VIEWER/" + inputPath.subpath(2, inputPath.getNameCount());

    // request deployment:
    var deploymentRequestDto = DeployUtil.getViewerDeploymentRequestDto("" + object.id);
    var deployed = deploy(viewerService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    // finish deployment:
    var request = target(String.format("exec/task/%s/", workDir)).request();
    MockServerUtil.startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", viewerService);
    createResultFile(workDir, object.filepath, "<pre>" + resultSentence + "</pre>");
    var finishedJson = DeployUtil.waitUntil(request, FINISHED);

    // verify:
    var kafkaNextcloudServiceMock = jerseyTest.getKafkaNextcloudServiceMock();
    Mockito.verify(
      kafkaNextcloudServiceMock,
      never())
           .send(Mockito.any());
  }

  private File findOutputFolder(String finishedJson) {
    String read = JsonPath.parse(finishedJson).read("$.outputDir");
    return Paths.get(NEXTCLOUD_VOLUME, read).toFile();
  }

}