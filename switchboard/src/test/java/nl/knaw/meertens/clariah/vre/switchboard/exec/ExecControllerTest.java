package nl.knaw.meertens.clariah.vre.switchboard.exec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.ConfigDto;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDTO;
import org.junit.Test;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.CONFIG_FILE_NAME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamType.FILE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class ExecControllerTest extends AbstractControllerTest {

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
        ObjectsRecordDTO object = createTestFileWithRegistryObject();
        String uniqueTestFile = object.filepath;

        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto("" + object.id);
        String expectedService = "UCTO";

        Response deployed = deploy(expectedService, deploymentRequestDto);
        assertThat(deployed.getStatus()).isBetween(200, 203);
        String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

        Invocation.Builder request = target(String.format("exec/task/%s/", workDir)).request();

        startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}");
        String response = waitUntil(request, FINISHED);

        assertThat(Paths.get(DEPLOYMENT_VOLUME, workDir, INPUT_DIR, uniqueTestFile).toFile()).exists();
        createResultFile(workDir);
        assertThatJson(response).node("status").isEqualTo("FINISHED");

        // Atm links are kept:
        assertThat(Paths.get(DEPLOYMENT_VOLUME, workDir, INPUT_DIR, uniqueTestFile).toFile()).exists();
    }

    @Test
    public void postDeploymentRequest_shouldOutputFolderWithTestResult() throws InterruptedException, IOException {
        ObjectsRecordDTO object = createTestFileWithRegistryObject();
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto("" + object.id);
        String expectedService = "UCTO";
        Response deployed = deploy(expectedService, deploymentRequestDto);
        assertThat(deployed.getStatus()).isBetween(200, 203);
        String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

        Invocation.Builder request = target(String.format("exec/task/%s/", workDir)).request();
        startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{}");
        createResultFile(workDir);

        // Check status is finished:
        String finishedJson = waitUntil(request, FINISHED);

        // Check output file is moved:
        File outputFolder = findOutputFolder(finishedJson);
        assertThat(outputFolder).isNotNull();
        assertThat(outputFolder.toString()).startsWith("/usr/local/owncloud/admin/files/output-20");
        Path outputFile = Paths.get(outputFolder.getPath(), resultFilename);
        assertThat(outputFile.toFile()).exists();
        assertThat(Files.readAllLines(outputFile).get(0)).isEqualTo(resultSentence);
    }

    @Test
    public void postDeploymentRequest_shouldCreateConfigFile() throws IOException {
        ObjectsRecordDTO object = createTestFileWithRegistryObject();
        String uniqueTestFile = object.filepath;

        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto("" + object.id);
        String expectedService = "UCTO";

        Response deployed = deploy(expectedService, deploymentRequestDto);
        assertThat(deployed.getStatus()).isBetween(200, 203);
        String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

        Path configFile = Paths.get(DEPLOYMENT_VOLUME, workDir, CONFIG_FILE_NAME);
        assertThat(configFile.toFile()).exists();
        String configContent = new String(Files.readAllBytes(configFile));
        ConfigDto config = new ObjectMapper().readValue(configContent, ConfigDto.class);

        assertThat(config.params.get(0).value).contains(uniqueTestFile);
        assertThat(config.params.get(0).name).isEqualTo("untokinput");

        assertThat(config.params.get(0).type).isEqualTo(FILE);
        assertThatJson(configContent).node("params[0].type").isEqualTo("file");

        assertThat(config.params.get(0).params.get(0).get("language").asText()).isEqualTo("eng");
        assertThat(config.params.get(0).params.get(0).get("author").asText()).isEqualTo(longName);
    }

    @Test
    public void testFinishRequest_shouldIgnoreUnknownFields() throws InterruptedException, IOException {
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto("1");
        String expectedService = "UCTO";
        Response deployed = deploy(expectedService, deploymentRequestDto);
        assertThat(deployed.getStatus()).isBetween(200, 203);
        String workDir = JsonPath.parse(deployed.readEntity(String.class)).read("$.workDir");

        Invocation.Builder request = target(String.format("exec/task/%s/", workDir)).request();

        createResultFile(workDir);
        startOrUpdateStatusMockServer(FINISHED.getHttpStatus(), workDir, "{\"finished\":false,\"id\":\"" + workDir + "\",\"key\":\"" + workDir + "\", \"blarpiness\":\"100%\"}");

        // Check status is finished:
        String finishedResponse = waitUntil(request, FINISHED);
        assertThatJson(finishedResponse).node("status").isEqualTo("FINISHED");
    }

    private File findOutputFolder(String finishedJson) {
        String read = JsonPath.parse(finishedJson).read("$.outputDir");
        return Paths.get(OWNCLOUD_VOLUME, read).toFile();
    }

}