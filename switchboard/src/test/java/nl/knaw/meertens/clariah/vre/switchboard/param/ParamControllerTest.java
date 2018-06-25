package nl.knaw.meertens.clariah.vre.switchboard.param;

import net.minidev.json.JSONObject;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import org.apache.commons.io.FileUtils;
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

public class ParamControllerTest extends AbstractControllerTest {

    @Test
    public void getCmdiForTest() throws IOException {
        startGetServiceByIdRegistryMock();

        Response response = jerseyTest.target("param/1")
                .request()
                .get();

        String json = response.readEntity(String.class);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThatJson(json).node("params[0].name").isEqualTo("input");
        assertThatJson(json).node("params[0].label").isEqualTo("Input text");
        assertThatJson(json).node("params[0].type").isEqualTo("integer");
        assertThatJson(json).node("params[0].minimumCardinality").isEqualTo("\"1\"");
        assertThatJson(json).node("params[0].maximumCardinality").isEqualTo("*");

        assertThatJson(json).node("params[1].name").isEqualTo("red-pill-and-blue-pill");
        assertThatJson(json).node("params[1].label").isEqualTo("Red pill and blue pill");
        assertThatJson(json).node("params[1].type").isEqualTo("enumeration");
        assertThatJson(json).node("params[1].valuesType").isEqualTo("string");
        assertThatJson(json).node("params[1].description").matches(containsString("This is your last chance."));
        assertThatJson(json).node("params[1].minimumCardinality").isEqualTo("\"1\"");
        assertThatJson(json).node("params[1].maximumCardinality").isEqualTo("\"1\"");

        assertThatJson(json).node("params[1].values").isArray().ofLength(2);
        assertThatJson(json).node("params[1].values[0].value").isEqualTo("red");
        assertThatJson(json).node("params[1].values[0].label").isEqualTo("Red");
        assertThatJson(json).node("params[1].values[0].description").matches(containsString("truth"));
        assertThatJson(json).node("params[1].values[1].value").isEqualTo("blue");
        assertThatJson(json).node("params[1].values[1].label").isEqualTo("Blue");
        assertThatJson(json).node("params[1].values[1].description").matches(containsString("happiness"));

        assertThatJson(json).node("paramGroups").isArray().ofLength(1);
        assertThatJson(json).node("paramGroups[0].params").isArray().ofLength(2);
        assertThatJson(json).node("paramGroups[0].params[0].name").isEqualTo("language");
        assertThatJson(json).node("paramGroups[0].params[0].values[0].label").isEqualTo("Dutch");
        assertThatJson(json).node("paramGroups[0].params[1].name").isEqualTo("author");

    }

    private void startGetServiceByIdRegistryMock() throws IOException {
        File file = new File(getClass().getClassLoader().getResource("test.cmdi").getFile());
        String cmdi = FileUtils.readFileToString(file, UTF_8);

        JSONObject obj = new JSONObject();
        obj.put("id", "1");
        obj.put("type", "TEST");
        obj.put("semantics", cmdi);
        final String json = obj.toString();

        mockServer
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
