package nl.knaw.meertens.clariah.vre.switchboard;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.exec.ExecController;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.NOT_FOUND;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.RUNNING;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DeploymentServiceImplTest extends AbstractSwitchboardTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(ExecController.class);
        SwitchboardDIBinder diBinder = new SwitchboardDIBinder(
                createObjectsRegistryServiceStub(),
                new DeploymentServiceImpl(getMapper(), "http://localhost:1080")
        );
        resourceConfig.register(diBinder);
        return resourceConfig;
    }

    @Before
    public void beforeDeployTests() {
        new MockServerClient("localhost", 1080).reset();
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
    public void testGetStatus_whenRunning() throws IOException {
        startStatusMockServer(RUNNING.getHttpStatus(), "{}");
        Response deployResponse = deploy("UCTO", getDeploymentRequestDto());
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        Response statusResponse = target(String.format("exec/task/%s", workDir))
                .request()
                .get();

        String json = statusResponse.readEntity(String.class);
        logger.info("statusResponse: " + json);
        assertThat(statusResponse.getStatus()).isEqualTo(202);
        assertThatJson(json).node("status").isEqualTo("RUNNING");
    }

    @Test
    public void testGetStatus_whenNotFound() throws IOException {
        startStatusMockServer(NOT_FOUND.getHttpStatus(), "{}");
        Response deployResponse = deploy("UCTO", getDeploymentRequestDto());
        String workDir = JsonPath.parse(deployResponse.readEntity(String.class)).read("$.workDir");

        Response statusResponse = target(String.format("exec/task/%s", workDir))
                .request()
                .get();

        String json = statusResponse.readEntity(String.class);
        logger.info("statusResponse: " + json);
        assertThat(statusResponse.getStatus()).isEqualTo(404);
        assertThatJson(json).node("status").isEqualTo("NOT_FOUND");
    }

}
