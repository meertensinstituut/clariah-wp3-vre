package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractSwitchboardTest;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.NOT_FOUND;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DeploymentServiceImplTest extends AbstractSwitchboardTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Before
    public void beforeDeploymentServiceImplTest() {
        startDeployMockServer(200);
    }

    @Test
    public void testStart_requestsDeploymentUrl() throws IOException {
        String expectedService = "UCTO";
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto();

        Response deployResponse = deploy(expectedService, deploymentRequestDto);

        String json = deployResponse.readEntity(String.class);
        logger.info(json);
        assertThat(deployResponse.getStatus()).isEqualTo(200);
        assertThatJson(json).node("status").isEqualTo("DEPLOYED");
    }

    @Test
    public void testStart_requestsDeploymentUrl_whenAlreadyRunning() throws IOException {
        String expectedService = "UCTO";
        DeploymentRequestDto deploymentRequestDto = getDeploymentRequestDto();

        Response firstTime = deploy(expectedService, deploymentRequestDto);

        new MockServerClient("localhost", 1080).reset();
        startDeployMockServer(403);

        Response secondTimeResponse = deploy(expectedService, deploymentRequestDto);

        String json = secondTimeResponse.readEntity(String.class);
        logger.info(json);
        assertThat(secondTimeResponse.getStatus()).isEqualTo(403);
        assertThatJson(json).node("status").isEqualTo("ALREADY_RUNNING");
    }

    @Test
    public void testGetStatus_whenRunning() throws IOException, InterruptedException {
        Response deployResponse = deploy("UCTO", getDeploymentRequestDto());
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        startStatusMockServer(RUNNING.getHttpStatus(), "{}");
        TimeUnit.SECONDS.sleep(1);

        Response statusResponse = target(String.format("exec/task/%s", workDir))
                .request()
                .get();

        String json = statusResponse.readEntity(String.class);
        assertThat(statusResponse.getStatus()).isEqualTo(202);
        assertThatJson(json).node("status").isEqualTo("RUNNING");
    }

    @Test
    public void testGetStatus_whenNotFound() throws Exception {
        logger.info("start_testGetStatus_whenNotFound");
        Response deployResponse = deploy("UCTO", getDeploymentRequestDto());
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        startStatusMockServer(NOT_FOUND.getHttpStatus(), "{}");
        TimeUnit.SECONDS.sleep(1);

        Response statusResponse = target(String.format("exec/task/%s", workDir))
                .request()
                .get();

        String json = statusResponse.readEntity(String.class);
        assertThat(statusResponse.getStatus()).isEqualTo(404);
        assertThatJson(json).node("status").isEqualTo("NOT_FOUND");
    }

}
