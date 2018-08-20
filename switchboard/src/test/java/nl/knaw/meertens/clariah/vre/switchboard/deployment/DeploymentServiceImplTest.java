package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.NOT_FOUND;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.HttpRequest.request;

public class DeploymentServiceImplTest extends AbstractControllerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testStart_requestsDeploymentUrl() throws IOException {
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

        mockServer.clear(
                request()
                        .withMethod("PUT")
                        .withPath("/deployment-service/a/exec/UCTO/.*")
        );
        startDeployMockServerWithUcto(403);

        Response secondTimeResponse = deploy(expectedService, deploymentRequestDto);

        String json = secondTimeResponse.readEntity(String.class);
        logger.info(json);
        assertThat(secondTimeResponse.getStatus()).isEqualTo(403);
        assertThatJson(json).node("status").isEqualTo("ALREADY_RUNNING");

        // Reset:
        setDeployBackTo200();
    }

    private void setDeployBackTo200() {
        mockServer.clear(
                request()
                        .withMethod("PUT")
                        .withPath("/deployment-service/a/exec/UCTO/.*")
        );
        startDeployMockServerWithUcto(200);
    }

    @Test
    public void testGetStatus_whenRunning() throws IOException, InterruptedException {
        Response deployResponse = deploy("UCTO", getDeploymentRequestDto("1"));
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        startOrUpdateStatusMockServer(RUNNING.getHttpStatus(), workDir, "{}", "UCTO");

        Invocation.Builder request = target(String.format("exec/task/%s", workDir)).request();
        String json = waitUntil(request, RUNNING);
        assertThatJson(json).node("status").isEqualTo("RUNNING");
    }

    @Test
    public void testGetStatus_whenNotFound() throws Exception {
        Response deployResponse = deploy("UCTO", getDeploymentRequestDto("1"));
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        startOrUpdateStatusMockServer(NOT_FOUND.getHttpStatus(), workDir, "{}", "UCTO");

        Invocation.Builder request = target(String.format("exec/task/%s", workDir)).request();
        String json = waitUntil(request, NOT_FOUND);
        assertThatJson(json).node("status").isEqualTo("NOT_FOUND");
    }

}
