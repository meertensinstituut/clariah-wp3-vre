package nl.knaw.meertens.clariah.vre.switchboard.exec;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.ObjectPath;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static java.nio.file.Path.of;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.EDITOR_OUTPUT;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.EDITOR_TMP;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.NEXTCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.USER_TO_LOCK_WITH;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.USER_TO_UNLOCK_WITH;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamType.FILE;
import static nl.knaw.meertens.clariah.vre.switchboard.util.DeployUtil.getDeploymentRequestDto;
import static nl.knaw.meertens.clariah.vre.switchboard.util.DeployUtil.getViewerDeploymentRequest;
import static nl.knaw.meertens.clariah.vre.switchboard.util.DeployUtil.waitUntil;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.createResultFile;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.createTestFileWithRegistryObject;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.getNextcloudFileContent;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil.getMockServer;
import static nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil.startDeployMockServer;
import static nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil.startOrUpdateStatusMockServer;
import static nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil.startServicesRegistryMockServer;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.getPath;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.never;

public class ExecControllerTest extends AbstractControllerTest {

  private static String dummyViewerService = "{\n" +
    "      \"id\": \"1\",\n" +
    "      \"name\": \"VIEWER\",\n" +
    "      \"kind\": \"viewer\",\n" +
    "      \"recipe\": \"nl.knaw.meertens.deployment.lib.recipe.Test\",\n" +
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

  private static String dummyEditorService = "{\n" +
    "      \"id\": \"1\",\n" +
    "      \"name\": \"EDITOR\",\n" +
    "      \"kind\": \"editor\",\n" +
    "      \"recipe\": \"nl.knaw.meertens.deployment.lib.recipe.Test\",\n" +
    "      \"semantics\": \"" + new String(new JsonStringEncoder().quoteAsString(getTestFileContent("editor.cmdi"))) +
    "\",\n" +
    "      \"tech\": null,\n" +
    "      \"time_created\": \"2018-05-28 12:34:48.863548+00\",\n" +
    "      \"time_changed\": null,\n" +
    "      \"mimetype\": \"text/plain\"\n" +
    "    }";

  @Test
  public void postDeploymentRequest_shouldCreateSymbolicLinksToInputFiles() throws Exception {
    var object = createTestFileWithRegistryObject(resultSentence);
    var uniqueTestFile = object.filepath;

    var deploymentRequestDto = getDeploymentRequestDto("" + object.id, longName);
    var expectedService = "UCTO";

    var deployed = deploy(expectedService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    var request = target(format("exec/task/%s/", workDir)).request();

    startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", "UCTO");
    var response = waitUntil(request, FINISHED);

    assertThat(Paths.get(DEPLOYMENT_VOLUME, workDir, INPUT_DIR, uniqueTestFile).toFile()).exists();
    createResultFile(workDir, resultFilename, resultSentence);
    assertThatJson(response).node("status").isEqualTo("FINISHED");

    // Atm links are kept:
    assertThat(Paths.get(DEPLOYMENT_VOLUME, workDir, INPUT_DIR, uniqueTestFile).toFile()).exists();
  }

  @Test
  public void postDeploymentRequest_shouldOutputFolderWithTestResult() throws InterruptedException, IOException {
    System.out.println("USER_TO_LOCK_WITH:" + USER_TO_LOCK_WITH);
    System.out.println("USER_TO_UNLOCK_WITH:" + USER_TO_UNLOCK_WITH);

    startServicesRegistryMockServer(dummyUctoService);

    var object = createTestFileWithRegistryObject(resultSentence);
    var deploymentRequestDto = getDeploymentRequestDto("" + object.id, longName);
    var expectedService = "UCTO";
    var deployed = deploy(expectedService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    var request = target(format("exec/task/%s/", workDir)).request();

    startOrUpdateStatusMockServer(RUNNING.getHttpStatus(), workDir, "{}", "UCTO");
    waitUntil(request, RUNNING);

    startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", "UCTO");
    startServicesRegistryMockServer(dummyUctoService);
    createResultFile(workDir, resultFilename, resultSentence);
    var finishedJson = waitUntil(request, FINISHED);

    // Check output file is moved:
    var outputFolder = findOutputFolder(finishedJson);
    assertThat(outputFolder).isNotNull();
    assertThat(outputFolder.toString()).contains("admin/files/output-20");
    var outputFile = Paths.get(outputFolder.getPath(), resultFilename);
    assertThat(outputFile.toFile()).exists();
    assertThat(Files.readAllLines(outputFile).get(0)).isEqualTo(resultSentence);
  }

  @Test
  public void postDeploymentRequest_shouldCreateConfigFile() throws IOException {
    var object = createTestFileWithRegistryObject(resultSentence);
    var uniqueTestFile = object.filepath;

    var deploymentRequestDto = getDeploymentRequestDto("" + object.id, longName);
    var expectedService = "UCTO";

    var deployed = deploy(expectedService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    var configFile = Paths.get(DEPLOYMENT_VOLUME, workDir, CONFIG_FILE_NAME);
    assertThat(configFile.toFile()).exists();
    var configJson = new String(Files.readAllBytes(configFile));
    var config = new ObjectMapper().readValue(configJson, ConfigDto.class);

    assertThat(config.params.get(0).value).contains(uniqueTestFile.toString());
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
  public void requestStatus_shouldReturnObjectPathsAsStrings() throws IOException {
    var object = createTestFileWithRegistryObject(resultSentence);
    var uniqueTestFile = object.filepath;

    var deploymentRequestDto = getDeploymentRequestDto("" + object.id, longName);
    var expectedService = "UCTO";

    var deployed = deploy(expectedService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    var deployResponse = deployed.readEntity(String.class);
    String workDir = JsonPath.parse(deployResponse).read("$.workDir");

    var request = target(format("exec/task/%s/", workDir)).request();
    var status = request
      .get()
      .readEntity(String.class);
    String file = JsonPath.parse(status).read("$.files[0]");
    assertThat(uniqueTestFile).isEqualTo(file);
  }

  @Test
  public void testFinishRequest_shouldIgnoreUnknownFields() throws InterruptedException {
    var deploymentRequestDto = getDeploymentRequestDto("1", longName);
    var expectedService = "UCTO";
    var deployed = deploy(expectedService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    var request = target(format("exec/task/%s/", workDir)).request();
    createResultFile(workDir, resultFilename, resultSentence);
    startOrUpdateStatusMockServer(
      FINISHED.getHttpStatus(),
      workDir,
      "{\"finished\":false,\"id\":\"" + workDir + "\",\"key\":\"" + workDir + "\", \"blarpiness\":\"100%\"}",
      "UCTO"
    );

    // Check status is finished:
    var finishedResponse = waitUntil(request, FINISHED);
    assertThatJson(finishedResponse).node("status").isEqualTo("FINISHED");
  }

  @Test
  public void postDeploymentRequest_shouldMoveViewerOutputFileToViewerFolder()
    throws IOException, InterruptedException {
    getMockServer().reset();
    startServicesRegistryMockServer(dummyViewerService);

    // create file and dummy registry object:
    var viewerService = "VIEWER";
    startDeployMockServer(viewerService, 200);
    var object = createTestFileWithRegistryObject(resultSentence);
    var inputPath = Paths.get(object.filepath);
    var expectedOutputPath = "admin/files/.vre/VIEWER/" + inputPath.subpath(2, inputPath.getNameCount());

    // request deployment:
    var deploymentRequestDto = getViewerDeploymentRequest("" + object.id);
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
    var request = target(format("exec/task/%s/", workDir)).request();
    startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", viewerService);
    createResultFile(workDir, object.filepath, "<pre>" + resultSentence + "</pre>");
    var finishedJson = waitUntil(request, FINISHED);

    // check output path:
    String viewerFile = JsonPath.parse(finishedJson).read("$.viewerFile");
    assertThat(viewerFile).isEqualTo(expectedOutputPath);

    // viewer file content:
    var viewerFileContent = getNextcloudFileContent(viewerFile);
    assertThat(viewerFileContent).contains("<pre>");
    assertThat(viewerFileContent).contains("Insanity");
    assertThat(viewerFileContent).contains("</pre>");
    String viewerFileContentInJson = JsonPath.parse(finishedJson).read("$.viewerFileContent");
    assertThat(viewerFileContentInJson).isEqualTo(viewerFileContent);
  }

  @Test
  public void postDeploymentRequest_shouldNotCreateKafkaMsg_whenViewerService()
    throws IOException, InterruptedException {
    getMockServer().reset();
    startServicesRegistryMockServer(dummyViewerService);

    // create file and dummy registry object:
    var viewerService = "VIEWER";
    startDeployMockServer(viewerService, 200);
    var object = createTestFileWithRegistryObject(resultSentence);
    var inputPath = ObjectPath.of(object.filepath).toPath();
    var expectedOutputPath = "admin/files/.vre/VIEWER/" + inputPath.subpath(2, inputPath.getNameCount());

    // request deployment:
    var deploymentRequestDto = getViewerDeploymentRequest("" + object.id);
    var deployed = deploy(viewerService, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

    // finish deployment:
    var request = target(format("exec/task/%s/", workDir)).request();
    startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", viewerService);
    createResultFile(workDir, object.filepath, "<pre>" + resultSentence + "</pre>");
    var finishedJson = waitUntil(request, FINISHED);



    // verify:
    var kafkaNextcloudServiceMock = jerseyTest.getKafkaNextcloudServiceMock();
    Mockito.verify(
      kafkaNextcloudServiceMock,
      never())
           .send(Mockito.any());
  }

  @Test
  public void deleteDeploymentRequest_shouldUpdateEditedFile() throws IOException, InterruptedException {

    getMockServer().reset();
    startServicesRegistryMockServer(dummyEditorService);

    // create file and dummy registry object:
    var service = "EDITOR";
    startDeployMockServer(service, 200);
    var object = createTestFileWithRegistryObject(getTestFileContent("editor-inputfile.xml"));
    var inputPath = object.filepath;

    // request deployment:
    var deploymentRequestDto = getViewerDeploymentRequest("" + object.id);
    var deployed = deploy(service, deploymentRequestDto);
    assertThat(deployed.getStatus()).isBetween(200, 203);
    var response = deployed.readEntity(String.class);
    String workDir = JsonPath.parse(response).read("$.workDir");

    // get config:
    var configFile = Paths.get(DEPLOYMENT_VOLUME, workDir, CONFIG_FILE_NAME);
    assertThat(configFile.toFile()).exists();
    var configJson = new String(Files.readAllBytes(configFile));
    var config = new ObjectMapper().readValue(configJson, ConfigDto.class);

    // check input file is defined:
    assertThat(config.params.get(0).name).isEqualTo("input");
    assertThat(config.params.get(0).value).contains(inputPath);

    // check editor iframe is defined:
    assertThat(config.params.get(1).name).isEqualTo("tmp");
    assertThat(config.params.get(1).value).isEqualTo(EDITOR_TMP);

    // check editor save file is defined:
    assertThat(config.params.get(2).name).isEqualTo("output");
    assertThat(
      getExtension(config.params.get(2).value)
    ).isEqualTo(
      getExtension(config.params.get(0).value)
    );

    // finish deployment:
    var request = target(format("exec/task/%s/", workDir)).request();
    startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}", service);

    // get editor iframe:
    var editor = getTestFileContent("editor-iframe.html");
    createResultFile(workDir, "editor.html", editor);
    var finishedJson = waitUntil(request, FINISHED);
    String viewerFileContentInJson = JsonPath.parse(finishedJson).read("$.viewerFileContent");
    assertThat(viewerFileContentInJson).isEqualToIgnoringWhitespace(editor);

    // create result file:
    var resultContent = getTestFileContent("editor-savefile.xml");
    var resultFilename = of(getPath(object.filepath), EDITOR_OUTPUT + ".txt").toString();
    createResultFile(workDir, resultFilename, resultContent);

    // stop deployment:
    var result = target(format("exec/task/%s", workDir))
      .request()
      .delete();
    assertThat(result.getStatus()).isEqualTo(200);

    // input file should be updated:
    assertThat(getNextcloudFileContent(inputPath))
      .isEqualToIgnoringWhitespace(getTestFileContent("editor-savefile.xml"));

  }

  private File findOutputFolder(String finishedJson) {
    String read = JsonPath.parse(finishedJson).read("$.outputDir");
    return Paths.get(NEXTCLOUD_VOLUME, read).toFile();
  }

}