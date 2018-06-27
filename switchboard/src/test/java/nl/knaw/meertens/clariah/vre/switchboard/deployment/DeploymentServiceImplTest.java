package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.NOT_FOUND;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DeploymentServiceImplTest extends AbstractControllerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testStart_requestsDeploymentUrl() throws IOException {
        mockServer.reset();
        startDeployMockServer(200);

        String expectedService = "UCTO";
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto("1");

        Response deployResponse = deploy(expectedService, deploymentRequestDto);

        String json = deployResponse.readEntity(String.class);
        assertThat(deployResponse.getStatus()).isEqualTo(200);
        assertThatJson(json).node("status").isEqualTo("DEPLOYED");
    }

    @Test
    public void testStart_requestsDeploymentUrl_whenAlreadyRunning() throws IOException {
        String expectedService = "UCTO";
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto("1");

        deploy(expectedService, deploymentRequestDto);

        mockServer.reset();
        startDeployMockServer(403);

        Response secondTimeResponse = deploy(expectedService, deploymentRequestDto);

        String json = secondTimeResponse.readEntity(String.class);
        logger.info(json);
        assertThat(secondTimeResponse.getStatus()).isEqualTo(403);
        assertThatJson(json).node("status").isEqualTo("ALREADY_RUNNING");
    }

    @Test
    public void testGetStatus_whenRunning() throws IOException, InterruptedException {
        Response deployResponse = deploy("UCTO", getDeploymentRequestDto("1"));
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        startStatusMockServer(RUNNING.getHttpStatus(), "{}");
        TimeUnit.SECONDS.sleep(2);

        Response statusResponse = target(String.format("exec/task/%s", workDir))
                .request()
                .get();

        String json = statusResponse.readEntity(String.class);
        assertThat(statusResponse.getStatus()).isEqualTo(202);
        assertThatJson(json).node("status").isEqualTo("RUNNING");
    }

    @Test
    public void testGetStatus_whenNotFound() throws Exception {
        mockServer.reset();
        startDeployMockServer(200);

        startStatusMockServer(FINISHED.getHttpStatus(), "{}");

        Response deployResponse = deploy("UCTO", getDeploymentRequestDto("1"));
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        jerseyTest.getPollService().stopPolling();
        mockServer.reset();
        startDeployMockServer(200);
        startStatusMockServer(NOT_FOUND.getHttpStatus(), "{}");
        jerseyTest.getPollService().startPolling();
        TimeUnit.SECONDS.sleep(3);

        Response statusResponse = target(String.format("exec/task/%s", workDir))
                .request()
                .get();

        String json = statusResponse.readEntity(String.class);
        assertThat(statusResponse.getStatus()).isEqualTo(404);
        assertThatJson(json).node("status").isEqualTo("NOT_FOUND");
    }

}
