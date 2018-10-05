package nl.knaw.meertens.clariah.vre.switchboard.object;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import org.junit.Test;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;

import javax.ws.rs.core.Response;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil.getMockServer;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ObjectControllerTest extends AbstractControllerTest {

    private String testService = "    {\n" +
            "      \"id\": \"1\",\n" +
            "      \"name\": \"TEST\",\n" +
            "      \"kind\": \"service\",\n" +
            "      \"recipe\": \"nl.knaw.meertens.deployment.lib.Test\",\n" +
            "      \"semantics\": \"<cmd:CMD xmlns:cmd=\\\"http://www.clarin.eu/cmd/1\\\" xmlns:cmdp=\\\"http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795\\\" xmlns:xs=\\\"http://www.w3.org/2001/XMLSchema\\\" xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:schemaLocation=\\\"\\n  http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd\\n  http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1505397653795/xsd\\\" CMDVersion=\\\"1.2\\\">\\n  <cmd:Header>\\n    <cmd:MdCreationDate>2018-05-28</cmd:MdCreationDate>\\n    <cmd:MdProfile>clarin.eu:cr1:p_1505397653795</cmd:MdProfile><!-- profile is fixed -->\\n  </cmd:Header>\\n  <cmd:Resources>\\n    <cmd:ResourceProxyList/>\\n    <cmd:JournalFileProxyList/>\\n    <cmd:ResourceRelationList/>\\n  </cmd:Resources>\\n  <cmd:Components>\\n    <cmdp:CLARINWebService>\\n      <cmdp:Service CoreVersion=\\\"1.0\\\">\\n        <cmdp:Name>Test</cmdp:Name>\\n        <cmdp:Description>Service to test deployment mechanism of VRE</cmdp:Description>\\n        <cmdp:ServiceDescriptionLocation/> <!-- test doesn't really run remote -->\\n        <cmdp:Operations>\\n          <cmdp:Operation>\\n            <cmdp:Name>main</cmdp:Name><!-- main is our default endpoint -->\\n            <cmdp:Input>\\n              <cmdp:Parameter><!-- use Parameter instead of ParameterGroup, if there are no nested parameters -->\\n                <cmdp:Name>input</cmdp:Name>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Input>\\n            <cmdp:Output>\\n              <cmdp:Parameter>\\n                <cmdp:Name>output</cmdp:Name>\\n                <cmdp:Description>Surprise</cmdp:Description>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Output>\\n          </cmdp:Operation>\\n        </cmdp:Operations>\\n      </cmdp:Service>\\n    </cmdp:CLARINWebService>\\n  </cmd:Components>\\n</cmd:CMD>\",\n" +
            "      \"tech\": null,\n" +
            "      \"time_created\": \"2018-05-28 12:34:48.863548+00\",\n" +
            "      \"time_changed\": null,\n" +
            "      \"mimetype\": \"text/plain\"\n" +
            "    }";

    private String nandoeService = "    {\n" +
            "      \"id\": \"13\",\n" +
            "      \"name\": \"NANDOE\",\n" +
            "      \"kind\": \"service\",\n" +
            "      \"recipe\": \"nl.knaw.meertens.deployment.lib.Nandoe\",\n" +
            "      \"semantics\": \"<cmd:CMD xmlns:cmd=\\\"http://www.clarin.eu/cmd/1\\\" xmlns:cmdp=\\\"http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795\\\" xmlns:xs=\\\"http://www.w3.org/2001/XMLSchema\\\" xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:schemaLocation=\\\"\\n  http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd\\n  http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1505397653795 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1505397653795/xsd\\\" CMDVersion=\\\"1.2\\\">\\n  <cmd:Header>\\n    <cmd:MdCreationDate>2018-05-28</cmd:MdCreationDate>\\n    <cmd:MdProfile>clarin.eu:cr1:p_1505397653795</cmd:MdProfile><!-- profile is fixed -->\\n  </cmd:Header>\\n  <cmd:Resources>\\n    <cmd:ResourceProxyList/>\\n    <cmd:JournalFileProxyList/>\\n    <cmd:ResourceRelationList/>\\n  </cmd:Resources>\\n  <cmd:Components>\\n    <cmdp:CLARINWebService>\\n      <cmdp:Service CoreVersion=\\\"1.0\\\">\\n        <cmdp:Name>Test</cmdp:Name>\\n        <cmdp:Description>Service to test deployment mechanism of VRE</cmdp:Description>\\n        <cmdp:ServiceDescriptionLocation/> <!-- test doesn't really run remote -->\\n        <cmdp:Operations>\\n          <cmdp:Operation>\\n            <cmdp:Name>main</cmdp:Name><!-- main is our default endpoint -->\\n            <cmdp:Input>\\n              <cmdp:Parameter><!-- use Parameter instead of ParameterGroup, if there are no nested parameters -->\\n                <cmdp:Name>input</cmdp:Name>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Input>\\n            <cmdp:Output>\\n              <cmdp:Parameter>\\n                <cmdp:Name>output</cmdp:Name>\\n                <cmdp:Description>Surprise</cmdp:Description>\\n                <cmdp:MIMEType>text/plain</cmdp:MIMEType>\\n              </cmdp:Parameter>\\n            </cmdp:Output>\\n          </cmdp:Operation>\\n        </cmdp:Operations>\\n      </cmdp:Service>\\n    </cmdp:CLARINWebService>\\n  </cmd:Components>\\n</cmd:CMD>\",\n" +
            "      \"tech\": null,\n" +
            "      \"time_created\": \"2018-05-28 12:34:48.863548+00\",\n" +
            "      \"time_changed\": null,\n" +
            "      \"mimetype\": \"text/plain\"\n" +
            "    }";

    // VIEWER is of type viewer:
    private String viewer = "    {\n" +
            "      \"id\": \"14\",\n" +
            "      \"name\": \"VIEWER\",\n" +
            "      \"kind\": \"viewer\",\n" +
            "      \"recipe\": \"nl.knaw.meertens.deployment.lib.Viewer\",\n" +
            "      \"semantics\": \"\"," +
            "      \"tech\": null,\n" +
            "      \"time_created\": \"2018-05-28 12:34:48.863548+00\",\n" +
            "      \"time_changed\": null,\n" +
            "      \"mimetype\": \"text/plain\"\n" +
            "    }";


    @Test
    public void getServicesFor_shouldGetServicesOfKindService_whenCorrectMimetype() {
        startServicesRegistryMock(
                newArrayList(testService, nandoeService),
                "(mimetype = text/plain) and (kind like service)"
        );

        Response response = target("object/1/services")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(200);
        String json = response.readEntity(String.class);
        assertThat(JsonPath.parse(json).read("$.length()", Integer.class)).isEqualTo(2);
        assertThat(JsonPath.parse(json).read("$[0].id", Integer.class)).isEqualTo(1);
        assertThat(JsonPath.parse(json).read("$[0].name", String.class)).isEqualTo("TEST");
        assertThat(JsonPath.parse(json).read("$[1].id", Integer.class)).isEqualTo(13);
        assertThat(JsonPath.parse(json).read("$[1].name", String.class)).isEqualTo("NANDOE");
    }

    @Test
    public void getViewersFor_shouldGetServicesOfKindViewer_whenCorrectMimetype() {
        startServicesRegistryMock(
                newArrayList(viewer),
                "(mimetype = text/plain) and (kind like viewer)"
        );

        Response response = target("object/1/viewers")
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(200);
        String json = response.readEntity(String.class);
        assertThat(JsonPath.parse(json).read("$.length()", Integer.class)).isEqualTo(1);
        assertThat(JsonPath.parse(json).read("$[0].id", Integer.class)).isEqualTo(14);
        assertThat(JsonPath.parse(json).read("$[0].name", String.class)).isEqualTo("VIEWER");
    }

    private void startServicesRegistryMock(List<String> servicesList, String filter) {
        String services = servicesList.stream().collect(Collectors.joining(", "));

        getMockServer()
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_table/service_with_mimetype")
                                .withQueryStringParameter(new Parameter("filter", filter))
                ).respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                                .withBody("{\n" +
                                        "  \"resource\": [\n"
                                        + services
                                        + "  ]\n"
                                        + "}")
                );
    }

}
