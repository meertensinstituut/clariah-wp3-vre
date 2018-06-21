package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.RequestRepository;
import nl.knaw.meertens.clariah.vre.switchboard.file.OwncloudFileService;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollServiceImpl;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryServiceStub;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryServiceImpl;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
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
import static nl.knaw.meertens.clariah.vre.switchboard.Config.USER_TO_LOCK_WITH;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getRequestRepository;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamType.FILE;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public abstract class AbstractSwitchboardTest extends JerseyTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static String testFile = "admin/files/testfile-switchboard.txt";
    protected static String longName = "Hubert Blaine Wolfeschlegelsteinhausenbergerdorff, Sr.";
    protected static String resultFilename = "result.txt";
    protected static String resultSentence = "Insanity: doing the same thing over and over again and expecting different results.";

    private static ClientAndServer mockServer;
    private static RequestRepository requestRepository = getRequestRepository();
    private static PollServiceImpl pollService = new PollServiceImpl(
            requestRepository,
            getMapper(),
            "http://localhost:1080"
    );
    private static OwncloudFileService owncloudFileService = new OwncloudFileService(
            OWNCLOUD_VOLUME,
            DEPLOYMENT_VOLUME,
            OUTPUT_DIR,
            INPUT_DIR,
            USER_TO_LOCK_WITH
    );
    private static ServicesRegistryServiceImpl servicesRegistryService = new ServicesRegistryServiceImpl(
            "http://localhost:1080",
            "abc",
            getMapper()
    );


    @Override
    protected Application configure() {
        ResourceConfig resourceConfig = new ResourceConfig(
                SwitchboardDIBinder.getControllerClasses()
        );

        SwitchboardDIBinder diBinder = new SwitchboardDIBinder(
                createObjectsRegistryServiceStub(),
                servicesRegistryService,
                new DeploymentServiceImpl(
                        "http://localhost:1080",
                        requestRepository,
                        pollService
                )
        );
        resourceConfig.register(diBinder);
        return resourceConfig;
    }

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            logger.info(String.format("Starting test [%s]", description.getMethodName()));
        }
    };

    @BeforeClass
    public static void beforeAbstractTests() throws IOException {
        Path path = Paths.get(OWNCLOUD_VOLUME + "/" + testFile);
        File file = path.toFile();
        file.getParentFile().mkdirs();
        String someText = "De vermeende terugkeer van tante Rosie naar Reetveerdegem werd als " +
                "een aangename schok ervaren in de levens van onze volstrekt nutteloze mannen, waarvan ik er op dat " +
                "ogenblik een in wording was.";
        Files.write(path, newArrayList(someText), Charset.forName("UTF-8"));
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
        owncloudFileService.unlock(testFile);
        requestRepository.clearAll();
    }

    @AfterClass
    public static void afterAbstractTests() {
        mockServer.stop();
        owncloudFileService.unlock(testFile);
    }

    private ObjectsRegistryServiceStub createObjectsRegistryServiceStub() {
        ObjectsRegistryServiceStub result = new ObjectsRegistryServiceStub();
        ObjectsRecordDTO testFileRecord = new ObjectsRecordDTO();
        testFileRecord.id = 1L;
        testFileRecord.filepath = testFile;
        testFileRecord.mimetype = "text/plain";
        result.setTestFileRecord(testFileRecord);
        return result;
    }

    protected DeploymentRequestDto getDeploymentRequestDto() throws IOException {
        DeploymentRequestDto deploymentRequestDto = new DeploymentRequestDto();
        ParamDto paramDto = new ParamDto();
        paramDto.name = "untokinput";
        paramDto.type = FILE;
        paramDto.value = "1";
        paramDto.params = getMapper().readTree("[{\"language\": \"eng\", \"author\": \"" + longName + "\"}]");
        deploymentRequestDto.params.add(paramDto);
        return deploymentRequestDto;
    }

    protected Response deploy(String expectedService, DeploymentRequestDto deploymentRequestDto) {
        return target(String.format("exec/%s", expectedService))
                .request()
                .post(Entity.json(deploymentRequestDto));
    }

    protected void startDeployMockServer(Integer status) {
        DeploymentStatus deploymentStatus = DeploymentStatus.getDeployStatus(status);
        new MockServerClient("localhost", 1080)
                .when(
                        request()
                                .withMethod("PUT")
                                .withPath("/deployment-service/a/exec/UCTO/.*"))
                .respond(
                        response()
                                .withStatusCode(status)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{\"id\":\"wd1234\",\"message\":\"running\",\"status\":" + deploymentStatus.toString() + "}")
                );
    }

    protected void startStatusMockServer(int status, String body) {
        new MockServerClient("localhost", 1080)
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/deployment-service/a/exec/UCTO/.*")
                ).respond(
                        response()
                                .withStatusCode(status)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody(body)
                );
    }

    protected void createResultFile(String workDir) {
        Path path = Paths.get(DEPLOYMENT_VOLUME, workDir, OUTPUT_DIR, resultFilename);
        assert(path.toFile().getParentFile().mkdirs());
        logger.info("result file path: " + path.toString());
        path.toFile().getParentFile().mkdirs();
        try {
            Files.write(path, newArrayList(resultSentence), UTF_8);
        } catch (IOException e) {
            handleException(e, "DeploymentServiceStub could not create result file");
        }
    }

}
