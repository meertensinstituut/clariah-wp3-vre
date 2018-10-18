package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;

import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil.getMockServer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ServicesRegistryServiceImplTest extends AbstractControllerTest {

    private String testResource =
            "    {\n" +
                    "      \"id\": \"1\",\n" +
                    "      \"name\": \"TEST\",\n" +
                    "      \"recipe\": \"nl.knaw.meertens.deployment.lib.Test\",\n" +
                    "      \"semantics\": \"<cmd:CMD xmlns:cmd=\\\"http://www.clarin.eu/cmd/1\\\" xmlns:cmdp=\\\"http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795\\\" xmlns:xs=\\\"http://www.w3.org/2001/XMLSchema\\\" xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:schemaLocation=\\\"\\n  http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd\\n  http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1505397653795/xsd\\\" CMDVersion=\\\"1.2\\\">\\n  <cmd:Header>\\n    <cmd:MdCreationDate>2018-05-28</cmd:MdCreationDate>\\n    <cmd:MdProfile>clarin.eu:cr1:p_1505397653795</cmd:MdProfile><!-- profile is fixed -->\\n  </cmd:Header>\\n  <cmd:Resources>\\n    <cmd:ResourceProxyList/>\\n    <cmd:JournalFileProxyList/>\\n    <cmd:ResourceRelationList/>\\n  </cmd:Resources>\\n  <cmd:Components>\\n    <cmdp:CLARINWebService>\\n      <cmdp:Service CoreVersion=\\\"1.0\\\">\\n        <cmdp:Name>Test</cmdp:Name>\\n        <cmdp:Description>Service to test deployment mechanism of VRE</cmdp:Description>\\n        <cmdp:ServiceDescriptionLocation/> <!-- test doesn't really run remote -->\\n        <cmdp:Operations>\\n          <cmdp:Operation>\\n            <cmdp:Name>main</cmdp:Name><!-- main is our default endpoint -->\\n            <cmdp:Input>\\n              <cmdp:Parameter><!-- use Parameter instead of ParameterGroup, if there are no nested parameters -->\\n                <cmdp:Name>input</cmdp:Name>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Input>\\n            <cmdp:Output>\\n              <cmdp:Parameter>\\n                <cmdp:Name>output</cmdp:Name>\\n                <cmdp:Description>Surprise</cmdp:Description>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Output>\\n          </cmdp:Operation>\\n        </cmdp:Operations>\\n      </cmdp:Service>\\n    </cmdp:CLARINWebService>\\n  </cmd:Components>\\n</cmd:CMD>\",\n" +
                    "      \"tech\": null,\n" +
                    "      \"time_created\": \"2018-05-28 12:34:48.863548+00\",\n" +
                    "      \"time_changed\": null,\n" +
                    "      \"mimetype\": \"text/plain\"\n" +
                    "    }";

    @Test
    public void getServicesByMimetype() {
        startGetServicesByMimetypeRegistryMock();
        var servicesRegistry = new ServicesRegistryServiceImpl("http://localhost:1080/api/v2/services", "abc", getMapper());
        var result = servicesRegistry.getServicesByMimetype("text/plain");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1);
        assertThat(result.get(0).getName()).isEqualTo("TEST");
        assertThat(result.get(1).getId()).isEqualTo(13);
        assertThat(result.get(1).getName()).isEqualTo("NANDOE");
    }

    @Test
    public void getService() {
        startGetServiceByIdRegistryMock();
        var servicesRegistry = new ServicesRegistryServiceImpl("http://localhost:1080/api/v2/services", "abc", getMapper());
        var result = servicesRegistry.getService(1L);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("TEST");
        assertThat(result.getSemantics()).startsWith("<cmd:CMD xmlns:cmd=\"http://www.clarin.eu/cmd/1\"");
    }

    private void startGetServicesByMimetypeRegistryMock() {
        getMockServer()
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/v2/services/_table/service_with_mimetype")
                                .withQueryStringParameter(new Parameter("filter", "mimetype = text/plain")),
                        Times.exactly(1)
                ).respond(
                response()
                        .withStatusCode(200)
                        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                        .withBody(
                                "{\n" +
                                        "  \"resource\": [\n" +
                                        testResource + "," +
                                        "    {\n" +
                                        "      \"id\": \"13\",\n" +
                                        "      \"name\": \"NANDOE\",\n" +
                                        "      \"recipe\": \"nl.knaw.meertens.deployment.lib.Nandoe\",\n" +
                                        "      \"semantics\": \"<cmd:CMD xmlns:cmd=\\\"http://www.clarin.eu/cmd/1\\\" xmlns:cmdp=\\\"http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795\\\" xmlns:xs=\\\"http://www.w3.org/2001/XMLSchema\\\" xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:schemaLocation=\\\"\\n  http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd\\n  http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1505397653795/xsd\\\" CMDVersion=\\\"1.2\\\">\\n  <cmd:Header>\\n    <cmd:MdCreationDate>2018-05-28</cmd:MdCreationDate>\\n    <cmd:MdProfile>clarin.eu:cr1:p_1505397653795</cmd:MdProfile><!-- profile is fixed -->\\n  </cmd:Header>\\n  <cmd:Resources>\\n    <cmd:ResourceProxyList/>\\n    <cmd:JournalFileProxyList/>\\n    <cmd:ResourceRelationList/>\\n  </cmd:Resources>\\n  <cmd:Components>\\n    <cmdp:CLARINWebService>\\n      <cmdp:Service CoreVersion=\\\"1.0\\\">\\n        <cmdp:Name>Test</cmdp:Name>\\n        <cmdp:Description>Service to test deployment mechanism of VRE</cmdp:Description>\\n        <cmdp:ServiceDescriptionLocation/> <!-- test doesn't really run remote -->\\n        <cmdp:Operations>\\n          <cmdp:Operation>\\n            <cmdp:Name>main</cmdp:Name><!-- main is our default endpoint -->\\n            <cmdp:Input>\\n              <cmdp:Parameter><!-- use Parameter instead of ParameterGroup, if there are no nested parameters -->\\n                <cmdp:Name>input</cmdp:Name>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Input>\\n            <cmdp:Output>\\n              <cmdp:Parameter>\\n                <cmdp:Name>output</cmdp:Name>\\n                <cmdp:Description>Surprise</cmdp:Description>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Output>\\n          </cmdp:Operation>\\n        </cmdp:Operations>\\n      </cmdp:Service>\\n    </cmdp:CLARINWebService>\\n  </cmd:Components>\\n</cmd:CMD>\",\n" +
                                        "      \"tech\": null,\n" +
                                        "      \"time_created\": \"2018-05-28 12:34:48.863548+00\",\n" +
                                        "      \"time_changed\": null,\n" +
                                        "      \"mimetype\": \"text/plain\"\n" +
                                        "    }\n" +
                                        "  ]\n" +
                                        "}")
        );
    }

    private void startGetServiceByIdRegistryMock() {
        getMockServer()
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/api/v2/services/_table/service/1")
                ).respond(
                response()
                        .withStatusCode(200)
                        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                        .withBody(testResource)
        );
    }
}
