package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import nl.knaw.meertens.clariah.vre.switchboard.util.DeployUtil;
import nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil;
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
    public void postDeploymentRequest_shouldRequestsDeployment() throws IOException {
        String expectedService = "UCTO";
        DeploymentRequestDto deploymentRequestDto = DeployUtil.getDeploymentRequestDto("1", longName);

        Response deployResponse = deploy(expectedService, deploymentRequestDto);

        String json = deployResponse.readEntity(String.class);
        assertThat(deployResponse.getStatus()).isEqualTo(201);
        assertThatJson(json).node("status").isEqualTo("DEPLOYED");
    }

    @Test
    public void postDeploymentRequest_shouldNotRequestsDeployment_whenAlreadyRunning() throws IOException {
        String expectedService = "UCTO";
        DeploymentRequestDto deploymentRequestDto = DeployUtil.getDeploymentRequestDto("1", longName);

        deploy(expectedService, deploymentRequestDto);

        MockServerUtil.getMockServer().clear(
                request()
                        .withMethod("PUT")
                        .withPath("/deployment-service/a/exec/UCTO/.*")
        );
        MockServerUtil.startDeployMockServerWithUcto(403);

        Response secondTimeResponse = deploy(expectedService, deploymentRequestDto);

        String json = secondTimeResponse.readEntity(String.class);
        assertThat(secondTimeResponse.getStatus()).isEqualTo(403);
        assertThatJson(json).node("status").isEqualTo("ALREADY_RUNNING");

        setDeployBackTo200();
    }

    @Test
    public void getDeploymentStatus_whenRunning() throws InterruptedException {
        Response deployResponse = deploy("UCTO", DeployUtil.getDeploymentRequestDto("1", longName));
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        MockServerUtil.startOrUpdateStatusMockServer(RUNNING.getHttpStatus(), workDir, "{}", "UCTO");

        Invocation.Builder request = target(String.format("exec/task/%s", workDir)).request();
        String json = DeployUtil.waitUntil(request, RUNNING);
        assertThatJson(json).node("status").isEqualTo("RUNNING");
    }

    @Test
    public void getDeploymentStatus_whenNotFound() throws Exception {
        Response deployResponse = deploy("UCTO", DeployUtil.getDeploymentRequestDto("1", longName));
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        MockServerUtil.startOrUpdateStatusMockServer(NOT_FOUND.getHttpStatus(), workDir, "{}", "UCTO");

        Invocation.Builder request = target(String.format("exec/task/%s", workDir)).request();
        String json = DeployUtil.waitUntil(request, NOT_FOUND);
        assertThatJson(json).node("status").isEqualTo("NOT_FOUND");
    }


    private void setDeployBackTo200() {
        MockServerUtil.getMockServer().clear(
                request()
                        .withMethod("PUT")
                        .withPath("/deployment-service/a/exec/UCTO/.*")
        );
        MockServerUtil.startDeployMockServerWithUcto(200);
    }
}
