package nl.knaw.meertens.clariah.vre.switchboard;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.param.Param;
import nl.knaw.meertens.clariah.vre.switchboard.param.ParamGroup;
import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRecordDTO;
import org.assertj.core.api.exception.RuntimeIOException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.param.ParamType.FILE;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public abstract class AbstractControllerTest extends AbstractTest {

    private static Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);

    protected static ClientAndServer mockServer;
    protected static SwitchboardJerseyTest jerseyTest = new SwitchboardJerseyTest();
    private static boolean isSetUp = false;

    private static String testFile;
    protected static String longName = "Hubert Blaine Wolfeschlegelsteinhausenbergerdorff, Sr.";
    protected static String resultFilename = "result.txt";
    protected static String resultSentence = "Insanity: doing the same thing over and over again and expecting different results.";

    protected static String dummyUctoService = "{\n" +
            "      \"id\": \"1\",\n" +
            "      \"name\": \"UCTO\",\n" +
            "      \"kind\": \"service\",\n" +
            "      \"recipe\": \"nl.knaw.meertens.deployment.lib.Test\",\n" +
            "      \"semantics\": \"<cmd:CMD xmlns:cmd=\\\"http://www.clarin.eu/cmd/1\\\" xmlns:cmdp=\\\"http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795\\\" xmlns:xs=\\\"http://www.w3.org/2001/XMLSchema\\\" xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:schemaLocation=\\\"\\n  http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd\\n  http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1505397653795/xsd\\\" CMDVersion=\\\"1.2\\\">\\n  <cmd:Header>\\n    <cmd:MdCreationDate>2018-05-28</cmd:MdCreationDate>\\n    <cmd:MdProfile>clarin.eu:cr1:p_1505397653795</cmd:MdProfile><!-- profile is fixed -->\\n  </cmd:Header>\\n  <cmd:Resources>\\n    <cmd:ResourceProxyList/>\\n    <cmd:JournalFileProxyList/>\\n    <cmd:ResourceRelationList/>\\n  </cmd:Resources>\\n  <cmd:Components>\\n    <cmdp:CLARINWebService>\\n      <cmdp:Service CoreVersion=\\\"1.0\\\">\\n        <cmdp:Name>Test</cmdp:Name>\\n        <cmdp:Description>Service to test deployment mechanism of VRE</cmdp:Description>\\n        <cmdp:ServiceDescriptionLocation/> <!-- test doesn't really run remote -->\\n        <cmdp:Operations>\\n          <cmdp:Operation>\\n            <cmdp:Name>main</cmdp:Name><!-- main is our default endpoint -->\\n            <cmdp:Input>\\n              <cmdp:Parameter><!-- use Parameter instead of ParameterGroup, if there are no nested parameters -->\\n                <cmdp:Name>input</cmdp:Name>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Input>\\n            <cmdp:Output>\\n              <cmdp:Parameter>\\n                <cmdp:Name>output</cmdp:Name>\\n                <cmdp:Description>Surprise</cmdp:Description>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Output>\\n          </cmdp:Operation>\\n        </cmdp:Operations>\\n      </cmdp:Service>\\n    </cmdp:CLARINWebService>\\n  </cmd:Components>\\n</cmd:CMD>\",\n" +
            "      \"tech\": null,\n" +
            "      \"time_created\": \"2018-05-28 12:34:48.863548+00\",\n" +
            "      \"time_changed\": null,\n" +
            "      \"mimetype\": \"text/plain\"\n" +
            "    }";

    @BeforeClass
    public static void beforeAbstractTests() throws Exception {
        if (isSetUp) {
            return;
        }
        jerseyTest.setUp();
        createTestFileWithRegistryObject();
        mockServer = ClientAndServer.startClientAndServer(1080);
        startDeployMockServerWithUcto(200);
        startServicesRegistryMockServer(dummyUctoService);
        isSetUp = true;
    }

    /* Method signature prevents JerseyTest.setUp() from running. */
    @Before
    public void setUp() {}

    /* Method signature prevents JerseyTest.tearDown() from running. */
    @After
    public void tearDown() {
        logger.info("reset abstract controller test setup");
        SwitchboardJerseyTest.getRequestRepository().clearAll();
        SwitchboardJerseyTest.getOwncloudFileService().unlock(testFile);
        mockServer.reset();
        startDeployMockServerWithUcto(200);
        startServicesRegistryMockServer(dummyUctoService);
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

    protected DeploymentRequestDto getDeploymentRequestDto(String id) throws IOException {
        DeploymentRequestDto deploymentRequest = new DeploymentRequestDto();
        ParamGroup paramGroup = new ParamGroup();
        paramGroup.name = "untokinput";
        paramGroup.type = FILE;
        paramGroup.value = id;
        Param param = new Param();
        param.name = "language";
        param.value = "eng";
        Param param2 = new Param();
        param2.name = "author";
        param2.value = longName;
        paramGroup.params = newArrayList(param, param2);
        deploymentRequest.params.add(paramGroup);
        return deploymentRequest;
    }

    protected DeploymentRequestDto getViewerDeploymentRequestDto(String id) throws IOException {
        DeploymentRequestDto deploymentRequestDto = new DeploymentRequestDto();
        ParamGroup paramDto = new ParamGroup();
        paramDto.name = "input";
        paramDto.type = FILE;
        paramDto.value = id;
        deploymentRequestDto.params.add(paramDto);
        return deploymentRequestDto;
    }

    protected void startOrUpdateStatusMockServer(int status, String workDir, String body, String serviceName) {
        HttpRequest getStatusOfWorkDirRequest = request()
                .withMethod("GET")
                .withPath("/deployment-service/a/exec/" + serviceName + "/" + workDir);

        mockServer.clear(getStatusOfWorkDirRequest);

        mockServer
                .when(getStatusOfWorkDirRequest)
                .respond(
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
            throw new RuntimeIOException("DeploymentServiceStub could not create result file", e);
        }
    }

    protected WebTarget target(String url) {
        return jerseyTest.target(url);
    }

    protected Response deploy(String expectedService, DeploymentRequestDto deploymentRequestDto) {
        return jerseyTest.deploy(expectedService, deploymentRequestDto);
    }

    protected static void startDeployMockServerWithUcto(Integer status) {
        String serviceName = "UCTO";
        startDeployMockServer(serviceName, status);
    }

    protected static void startDeployMockServer(String serviceName, Integer status) {
        logger.info(String.format("start deploy mock server of service [%s] with status [%d]", serviceName, status));
        DeploymentStatus deploymentStatus = DeploymentStatus.getDeployStatus(status);
        mockServer
                .when(
                        request()
                                .withMethod("PUT")
                                .withPath("/deployment-service/a/exec/" + serviceName + "/.*"))
                .respond(
                        response()
                                .withStatusCode(status)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{\"message\":\"running\",\"status\":\"" + deploymentStatus.toString() + "\"}")
                );
    }

    protected static void startServicesRegistryMockServer(String serviceJson) {
        mockServer
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_table/service/"))
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{ \"resource\": [" + serviceJson + "]}")
                );
    }

    protected String waitUntil(Invocation.Builder request, DeploymentStatus deploymentStatus) throws InterruptedException {
        logger.info(String.format("Wait until status [%s]", deploymentStatus));
        int httpStatus = 0;
        String json = "";
        for (int i = 0; i < 20; i++) {
            Response response = request.get();
            httpStatus = response.getStatus();
            json = response.readEntity(String.class);
            String status = JsonPath.parse(json).read("$.status");
            if (status.equals(deploymentStatus.toString())) {
                logger.info(String.format("Status is [%s]", deploymentStatus));
                return json;
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
        throw new AssertionError(String.format("Deployment status [%s] not found in response [%d][%s]", deploymentStatus, httpStatus, json));
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

    private static ObjectsRecordDTO createObject(String filePath, long id) {
        ObjectsRecordDTO testFileRecord = new ObjectsRecordDTO();
        testFileRecord.id = id;
        testFileRecord.filepath = filePath;
        testFileRecord.mimetype = "text/plain";
        return testFileRecord;
    }

}
