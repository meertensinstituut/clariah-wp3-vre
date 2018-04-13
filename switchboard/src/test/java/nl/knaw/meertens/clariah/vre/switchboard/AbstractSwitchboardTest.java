package nl.knaw.meertens.clariah.vre.switchboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.ParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.exec.ExecController;
import nl.knaw.meertens.clariah.vre.switchboard.file.DeploymentFileService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryServiceStub;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getRequestRepositoryService;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.ParamType.FILE;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public abstract class AbstractSwitchboardTest extends JerseyTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    ObjectMapper mapper = new ObjectMapper();

    static DeploymentFileService deploymentFileService = new DeploymentFileService(
            OWNCLOUD_VOLUME, DEPLOYMENT_VOLUME, OUTPUT_DIR, INPUT_DIR);

    String longName = "Hubert Blaine Wolfeschlegelsteinhausenbergerdorff, Sr.";

    static final String RESULT_FILENAME = "result.txt";
    String resultSentence = "Insanity: doing the same thing over and over again and expecting different results.";

    static ClientAndServer mockServer;

    private static PollServiceImpl pollService = new PollServiceImpl(
            getRequestRepositoryService(),
            getMapper(),
            "http://localhost:1080"
    );

    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(ExecController.class);

        SwitchboardDIBinder diBinder = new SwitchboardDIBinder(
                createObjectsRegistryServiceStub(),
                new DeploymentServiceImpl(
                        "http://localhost:1080",
                        getRequestRepositoryService(),
                        pollService
                )
        );
        resourceConfig.register(diBinder);
        return resourceConfig;
    }

    @BeforeClass
    public static void beforeAbstractTests() {
        mockServer = ClientAndServer.startClientAndServer(1080);
    }

    @Before
    public void beforeAbstractTest() {
        new MockServerClient("localhost", 1080).reset();
        pollService.startPolling();
    }

    @After
    public void afterAbstractTest() {
        pollService.stopPolling();
    }

    @AfterClass
    public static void afterDeploymentTests() {
        mockServer.stop();
    }

    ObjectsRegistryServiceStub createObjectsRegistryServiceStub() {
        ObjectsRegistryServiceStub result = new ObjectsRegistryServiceStub();
        ObjectsRecordDTO testFileRecord = new ObjectsRecordDTO();
        testFileRecord.id = 1L;
        testFileRecord.filepath = "admin/files/testfile-switchboard.txt";
        result.setTestFileRecord(testFileRecord);
        return result;
    }

    DeploymentRequestDto getDeploymentRequestDto() throws IOException {
        DeploymentRequestDto deploymentRequestDto = new DeploymentRequestDto();
        ParamDto paramDto = new ParamDto();
        paramDto.name = "untokinput";
        paramDto.type = FILE;
        paramDto.value = "1";
        paramDto.params = mapper.readTree("[{\"language\": \"eng\", \"author\": \"" + longName + "\"}]");
        deploymentRequestDto.params.add(paramDto);
        return deploymentRequestDto;
    }

    Response deploy(String expectedService, DeploymentRequestDto deploymentRequestDto) {
        return target(String.format("exec/%s", expectedService))
                .request()
                .post(Entity.json(deploymentRequestDto));
    }

    void startDeployMockServer(Integer status) {
        DeploymentStatus deploymentStatus = DeploymentStatus.getDeployStatus(status);
        new MockServerClient("localhost", 1080)
                .when(
                        request()
                                // TODO: GET request atm, should be PUT in the future:
                                .withMethod("GET")
                                .withPath("/deployment-service/a/exec/UCTO/.*/"),
                        exactly(1))
                .respond(
                        response()
                                .withStatusCode(status)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{\"id\":\"wd1234\",\"message\":\"running\",\"status\":" + deploymentStatus.toString() + "}")
                );
    }

    void startStatusMockServer(int status, String body) {
        new MockServerClient("localhost", 1080)
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/deployment-service/a/exec/task/.*/"))
                .respond(
                        response()
                                .withStatusCode(status)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody(body)
                );
    }

    void createResultFile(String workDir) {
        Path path = Paths.get(DEPLOYMENT_VOLUME, workDir, OUTPUT_DIR, RESULT_FILENAME);
        logger.info("result file path: " + path.toString());
        path.toFile().getParentFile().mkdirs();
        try {
            Files.write(path, newArrayList(resultSentence), UTF_8);
        } catch (IOException e) {
            handleException(e, "DeploymentServiceStub could not create result file");
        }
    }

}
