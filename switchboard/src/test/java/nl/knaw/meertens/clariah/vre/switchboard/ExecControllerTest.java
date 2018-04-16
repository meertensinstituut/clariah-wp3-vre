package nl.knaw.meertens.clariah.vre.switchboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRecordDTO;
import org.apache.commons.io.FilenameUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.ParamType.FILE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.containsString;

public class ExecControllerTest extends AbstractSwitchboardTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @BeforeClass
    public static void afterExecControllerTests() throws IOException {
        Path path = Paths.get(OWNCLOUD_VOLUME + "/" + testFile);
        File file = path.toFile();
        file.getParentFile().mkdirs();
        Files.write(path, newArrayList(someText), Charset.forName("UTF-8"));
    }

    @Before
    public void beforeExecControllerTests() {
        ObjectsRecordDTO record = new ObjectsRecordDTO();
        record.id = 1L;
        record.filepath = testFile;
        startDeployMockServer(200);
    }

    @AfterClass
    public static void afterExecControllerTest () {
        deploymentFileService.unlock(testFile);
    }

    @Test
    public void getHelp() {
        Response response = target("exec")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(200);
        String json = response.readEntity(String.class);
        assertThatJson(json).node("msg").matches(containsString("readme"));
    }

    @Test
    public void postDeploymentRequest_shouldCreateSymbolicLinksToInputFiles() throws Exception {
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto();
        String expectedService = "UCTO";

        Response deployed = deploy(expectedService, deploymentRequestDto);
        assertThat(deployed.getStatus()).isBetween(200, 203);
        String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

        assertThat(Paths.get(DEPLOYMENT_VOLUME, workDir, INPUT_DIR, testFile).toFile()).exists();

        logger.info("postDeploymentRequest_shouldCreateAndRemoveSymbolicLinksToInputFiles has workDir: " + workDir);
        createResultFile(workDir);
        startStatusMockServer(FINISHED.getHttpStatus(), "{}");
        TimeUnit.SECONDS.sleep(5);

        Response pollStatusResponse = target(String.format("exec/task/%s/", workDir))
                .request()
                .get();

        assertThat(pollStatusResponse.getStatus()).isEqualTo(200);
        String jsonGetAfter6Secs = pollStatusResponse.readEntity(String.class);
        assertThatJson(jsonGetAfter6Secs).node("status").isEqualTo("FINISHED");

        // Atm links are kept:
        assertThat(Paths.get(DEPLOYMENT_VOLUME, workDir, INPUT_DIR, testFile).toFile()).exists();
    }

    @Test
    public void postDeploymentRequest_shouldOutputFolderWithTestResult() throws InterruptedException, IOException {
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto();
        String expectedService = "UCTO";
        Response deployed = deploy(expectedService, deploymentRequestDto);
        assertThat(deployed.getStatus()).isBetween(200, 203);
        String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

        createResultFile(workDir);
        startStatusMockServer(FINISHED.getHttpStatus(), "{}");
        TimeUnit.SECONDS.sleep(5);

        // Check status is finished:
        Response finishedResponse = target(String.format("exec/task/%s/", workDir))
                .request()
                .get();
        assertThat(finishedResponse.getStatus()).isEqualTo(200);
        String finishedJson = finishedResponse.readEntity(String.class);
        logger.info("finishedJson: " + finishedJson);
        assertThatJson(finishedJson).node("status").isEqualTo("FINISHED");

        // Check output file is moved:
        File outputFolder = findOutputFolder();
        logger.info("outputFolder: " + outputFolder);
        assertThat(outputFolder).isNotNull();
        assertThat(outputFolder.toString()).startsWith("/usr/local/owncloud/admin/files/output-20");
        Path outputFile = Paths.get(outputFolder.getPath(), RESULT_FILENAME);
        assertThat(outputFile.toFile()).exists();
        assertThat(Files.readAllLines(outputFile).get(0)).isEqualTo(resultSentence);
    }

    @Test
    public void postDeploymentRequest_shouldCreateConfigFile() throws IOException {
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto();
        String expectedService = "UCTO";

        Response deployed = deploy(expectedService, deploymentRequestDto);
        assertThat(deployed.getStatus()).isBetween(200, 203);
        String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

        Path configFile = Paths.get(DEPLOYMENT_VOLUME, workDir, CONFIG_FILE_NAME);
        assertThat(configFile.toFile()).exists();
        String configContent = new String(Files.readAllBytes(configFile));
        logger.info("config content: " + configContent);
        ConfigDto config = new ObjectMapper().readValue(configContent, ConfigDto.class);

        assertThat(config.params.get(0).value).contains(testFile);
        assertThat(config.params.get(0).name).isEqualTo("untokinput");

        assertThat(config.params.get(0).type).isEqualTo(FILE);
        assertThatJson(configContent).node("params[0].type").isEqualTo("file");

        assertThat(config.params.get(0).params.get(0).get("language").asText()).isEqualTo("eng");
        assertThat(config.params.get(0).params.get(0).get("author").asText()).isEqualTo(longName);
    }

    @Test
    public void testFinishRequest_shouldIgnoreUnknownFields() throws InterruptedException, IOException {
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto();
        String expectedService = "UCTO";
        Response deployed = deploy(expectedService, deploymentRequestDto);
        assertThat(deployed.getStatus()).isBetween(200, 203);
        String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

        createResultFile(workDir);
        startStatusMockServer(FINISHED.getHttpStatus(), "{\"finished\":false,\"id\":\"" + workDir + "\",\"key\":\"" + workDir + "\", \"blarpiness\":\"100%\"}");
        TimeUnit.SECONDS.sleep(1);

        // Check status is finished:
        Response finishedResponse = target(String.format("exec/task/%s/", workDir))
                .request()
                .get();
        assertThat(finishedResponse.getStatus()).isEqualTo(200);
        String finishedJson = finishedResponse.readEntity(String.class);
        logger.info("finishedJson: " + finishedJson);
        assertThatJson(finishedJson).node("status").isEqualTo("FINISHED");
    }

    private File findOutputFolder() {
        String pathWithoutFile = FilenameUtils.getPath(testFile);
        File resultParentFolder = Paths.get(OWNCLOUD_VOLUME, pathWithoutFile).toFile();
        File outputFolder = null;
        for (File file : resultParentFolder.listFiles()) {
            if (file.getName().contains(OUTPUT_DIR)) {
                outputFolder = file;
            }
        }
        return outputFolder;
    }

}