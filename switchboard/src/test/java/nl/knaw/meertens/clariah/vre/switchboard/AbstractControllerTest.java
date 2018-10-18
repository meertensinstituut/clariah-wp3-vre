package nl.knaw.meertens.clariah.vre.switchboard;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentRequestDto;
import nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil;
import nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockserver.integration.ClientAndServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public abstract class AbstractControllerTest extends AbstractTest {

    private static Logger logger = LoggerFactory.getLogger(AbstractControllerTest.class);

    protected static SwitchboardJerseyTest jerseyTest = new SwitchboardJerseyTest();
    private static boolean isSetUp = false;

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
        FileUtil.createTestFileWithRegistryObject(resultSentence);
        MockServerUtil.setMockServer(ClientAndServer.startClientAndServer(1080));
        MockServerUtil.startDeployMockServerWithUcto(200);
        MockServerUtil.startServicesRegistryMockServer(dummyUctoService);
        isSetUp = true;
    }

    /* Method signature prevents JerseyTest.setUp() from running. */
    @Before
    public void setUp() {
    }

    /* Method signature prevents JerseyTest.tearDown() from running. */
    @After
    public void tearDown() {
        logger.info("reset abstract controller test setup");
        SwitchboardJerseyTest.getRequestRepository().clearAll();
        MockServerUtil.getMockServer().reset();
        MockServerUtil.startDeployMockServerWithUcto(200);
        MockServerUtil.startServicesRegistryMockServer(dummyUctoService);
    }

    protected WebTarget target(String url) {
        return jerseyTest.target(url);
    }

    protected Response deploy(String expectedService, DeploymentRequestDto deploymentRequestDto) {
        return jerseyTest.deploy(expectedService, deploymentRequestDto);
    }

}
