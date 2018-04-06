package nl.knaw.meertens.clariah.vre.switchboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.ParamDto;
import nl.knaw.meertens.clariah.vre.switchboard.file.DeploymentFileService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.ObjectsRegistryServiceStub;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.switchboard.App.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.App.INPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.App.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.App.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.ExceptionHandler.*;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.ParamType.FILE;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public abstract class AbstractSwitchboardTest extends JerseyTest {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    static String testFile = "admin/files/testfile-switchboard.txt";

    static final String STUBRESULT_FILENAME = "stubresult.txt";

    private DeploymentFileService deploymentFileService = new DeploymentFileService(
            OWNCLOUD_VOLUME, DEPLOYMENT_VOLUME, OUTPUT_DIR, INPUT_DIR);
    private ObjectMapper mapper = new ObjectMapper();

    String longName = "Hubert Blaine Wolfeschlegelsteinhausenbergerdorff, Sr.";

    private static String someText = "De vermeende terugkeer van tante Rosie naar Reetveerdegem werd als " +
            "een aangename schok ervaren in de levens van onze volstrekt nutteloze mannen, waarvan ik er op dat " +
            "ogenblik een in wording was.";

    private static ClientAndServer mockServer;

    @BeforeClass
    public static void beforeExecTests() throws IOException {
        Path path = Paths.get(OWNCLOUD_VOLUME + "/" + testFile);
        File file = path.toFile();
        file.getParentFile().mkdirs();
        Files.write(path, newArrayList(someText), Charset.forName("UTF-8"));
        mockServer = startClientAndServer(1080);
    }

    @After
    public void afterAbstractTests() {
        deploymentFileService.unlock(testFile);
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
                                .withMethod("GET") // TODO: atm, should be PUT in the future
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
                                .withPath("/deployment-service/a/exec/task/.*/"),
                        exactly(1))
                .respond(
                        response()
                                .withStatusCode(status)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody(body)
                );
    }

    Path createResultFile(String workDir) {
        Path path = Paths.get(DEPLOYMENT_VOLUME, workDir, OUTPUT_DIR, STUBRESULT_FILENAME);
        path.toFile().getParentFile().mkdirs();
        logger.info("result file path: " + path.toString());
        try {
            Files.write(path, newArrayList("Geen resultaat is ook een resultaat."), Charset.forName("UTF-8"));
        } catch (IOException e) {
            handleException(e, "DeploymentServiceStub could not create result file");
        }
        return path;
    }



}
