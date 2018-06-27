package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDTO;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamType.FILE;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public abstract class AbstractControllerTest {

    protected final static Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);

    protected static SwitchboardJerseyTest jerseyTest = new SwitchboardJerseyTest();
    private static boolean isSetUp = false;

    protected static ClientAndServer mockServer;

    protected static String longName = "Hubert Blaine Wolfeschlegelsteinhausenbergerdorff, Sr.";
    protected static String resultFilename = "result.txt";
    protected static String resultSentence = "Insanity: doing the same thing over and over again and expecting different results.";
    protected static String testFile = "admin/files/testfile-switchboard.txt";

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            logger.info(String.format("Starting test [%s]", description.getMethodName()));
        }
    };

    @BeforeClass
    public static void beforeAbstractTests() throws Exception {
        if (!isSetUp) {
            jerseyTest.setUp();
            isSetUp = true;
        }
        createTestFileWithRegistryObject();
        mockServer = ClientAndServer.startClientAndServer(1080);
        startDeployMockServer(200);
    }

    protected static ObjectsRecordDTO createTestFileWithRegistryObject() throws IOException {
        String fileName = String.format("admin/files/testfile-switchboard-%s.txt", UUID.randomUUID());
        testFile = fileName;
        createFile(fileName);
        Long maxId = SwitchboardJerseyTest.getObjectsRegistryServiceStub().getMaxTestObject();
        Long newId = maxId + 1;
        ObjectsRecordDTO newObject = createObject(fileName, newId);
        SwitchboardJerseyTest.getObjectsRegistryServiceStub().addTestObject(newObject);
        return newObject;
    }

    private static void createFile(String fileName) throws IOException {
        Path path = Paths.get(OWNCLOUD_VOLUME + "/" + fileName);
        File file = path.toFile();
        file.getParentFile().mkdirs();
        String someText = "De vermeende terugkeer van tante Rosie naar Reetveerdegem werd als " +
                "een aangename schok ervaren in de levens van onze volstrekt nutteloze mannen, waarvan ik er op dat " +
                "ogenblik een in wording was.";
        Files.write(path, newArrayList(someText), Charset.forName("UTF-8"));
    }

    @Before
    public void setUp() {
        // To prevent that JerseyTests setUp() runs
    }

    @After
    public void tearDown() {
        SwitchboardJerseyTest.getOwncloudFileService().unlock(testFile);
        SwitchboardJerseyTest.getRequestRepository().clearAll();
    }

    @AfterClass
    public static void afterAbstractTests() {
        mockServer.stop();
        SwitchboardJerseyTest.getOwncloudFileService().unlock(testFile);
    }

    protected DeploymentRequestDto getDeploymentRequestDto(String id) throws IOException {
        DeploymentRequestDto deploymentRequestDto = new DeploymentRequestDto();
        ParamDto paramDto = new ParamDto();
        paramDto.name = "untokinput";
        paramDto.type = FILE;
        paramDto.value = id;
        paramDto.params = getMapper().readTree("[{\"language\": \"eng\", \"author\": \"" + longName + "\"}]");
        deploymentRequestDto.params.add(paramDto);
        return deploymentRequestDto;
    }

    protected void startStatusMockServer(int status, String body) {
        mockServer
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
        assert (path.toFile().getParentFile().mkdirs());
        logger.info("result file path: " + path.toString());
        path.toFile().getParentFile().mkdirs();
        try {
            Files.write(path, newArrayList(resultSentence), UTF_8);
        } catch (IOException e) {
            handleException(e, "DeploymentServiceStub could not create result file");
        }
    }

    protected WebTarget target(String url) {
        return jerseyTest.target(url);
    }

    protected Response deploy(String expectedService, DeploymentRequestDto deploymentRequestDto) {
        return jerseyTest.deploy(expectedService, deploymentRequestDto);
    }

    protected void restartMockServer() {
        mockServer.stop();
        mockServer = ClientAndServer.startClientAndServer(1080);
    }

    protected static void startDeployMockServer(Integer status) {
        DeploymentStatus deploymentStatus = DeploymentStatus.getDeployStatus(status);
        mockServer
                .when(
                        request()
                                .withMethod("PUT")
                                .withPath("/deployment-service/a/exec/UCTO/.*"))
                .respond(
                        response()
                                .withStatusCode(status)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{\"message\":\"running\",\"status\":" + deploymentStatus.toString() + "}")
                );
    }

    private static ObjectsRecordDTO createObject(String filePath, long id) {
        ObjectsRecordDTO testFileRecord = new ObjectsRecordDTO();
        testFileRecord.id = id;
        testFileRecord.filepath = filePath;
        testFileRecord.mimetype = "text/plain";
        return testFileRecord;
    }
}
