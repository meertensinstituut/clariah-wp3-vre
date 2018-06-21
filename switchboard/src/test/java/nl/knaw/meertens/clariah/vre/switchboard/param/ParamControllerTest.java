package nl.knaw.meertens.clariah.vre.switchboard.param;

import net.minidev.json.JSONObject;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractSwitchboardTest;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.model.Header;

import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.apache.commons.codec.Charsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ParamControllerTest extends AbstractSwitchboardTest {

    @Before
    public void beforeExecControllerTests() {
        startDeployMockServer(200);
    }

    @Test
    public void getCmdiForTest() throws IOException {
        startGetServiceByIdRegistryMock();

        Response response = target("param/1")
                .request()
                .get();

        String json = response.readEntity(String.class);
        System.out.println("response: " + json);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThatJson(json).node("params[0].name").isEqualTo("input");
        assertThatJson(json).node("params[0].label").isEqualTo("Input text");
        assertThatJson(json).node("params[0].type").isEqualTo("integer");
        assertThatJson(json).node("params[0].minimumCardinality").isEqualTo("\"1\"");
        assertThatJson(json).node("params[0].maximumCardinality").isEqualTo("*");
        assertThatJson(json).node("params[0].name").isEqualTo("input");
    }

    private void startGetServiceByIdRegistryMock() throws IOException {
        File file = new File(getClass().getClassLoader().getResource("test.cmdi").getFile());
        String cmdi = FileUtils.readFileToString(file, UTF_8);

        JSONObject obj = new JSONObject();
        obj.put("id", "1");
        obj.put("type", "TEST");
        obj.put("semantics", cmdi);
        final String json = obj.toString();

        new MockServerClient("localhost", 1080)
                .when(
                        request()
                                .withMethod("GET")
                                .withPath("/_table/service/1")
                ).respond(
                response()
                        .withStatusCode(200)
                        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                        .withBody(json)
        );
    }

}
