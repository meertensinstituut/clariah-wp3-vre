package nl.knaw.meertens.clariah.vre.switchboard.param;

import net.minidev.json.JSONObject;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil.getMockServer;
import static org.apache.commons.codec.Charsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ParamControllerTest extends AbstractControllerTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void getParamsFor_whenService() throws IOException {
        startGetServiceByIdRegistryMock("service", "test.cmdi", "TEST");

        Response response = jerseyTest.target("services/1/params")
                .request()
                .get();

        String json = response.readEntity(String.class);
        assertThat(response.getStatus()).isEqualTo(200);

        // main fields:
        assertThatJson(json).node("id").isEqualTo("1");
        assertThatJson(json).node("name").isEqualTo("TEST");
        assertThatJson(json).node("kind").isEqualTo("service");

        // integer param with label:
        assertThatJson(json).node("params[0].name").isEqualTo("input");
        assertThatJson(json).node("params[0].label").isEqualTo("Input text");
        assertThatJson(json).node("params[0].type").isEqualTo("integer");
        assertThatJson(json).node("params[0].minimumCardinality").isEqualTo("\"1\"");
        assertThatJson(json).node("params[0].maximumCardinality").isEqualTo("*");

        // enumeration param with two values:
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

        // file param:
        assertThatJson(json).node("params[2].name").isEqualTo("inputfile");
        assertThatJson(json).node("params[2].label").isEqualTo("Input file");
        assertThatJson(json).node("params[2].type").isEqualTo("file");

        // param with two params:
        assertThatJson(json).node("params[3].params").isArray().ofLength(2);
        assertThatJson(json).node("params[3].params[0].name").isEqualTo("language");
        assertThatJson(json).node("params[3].params[0].values[0].label").isEqualTo("Dutch");
        assertThatJson(json).node("params[3].params[1].name").isEqualTo("author");

        // param with one param:
        assertThatJson(json).node("params[4].name").isEqualTo("second-param-group");
        assertThatJson(json).node("params[4].params").isArray().ofLength(1);
        assertThatJson(json).node("params[4].params[0].name").isEqualTo("second-param-group-param");
    }

    @Test
    public void getParamsFor_whenViewer() throws IOException {
        startGetServiceByIdRegistryMock("viewer", "viewer.cmdi", "VIEWER");

        Response response = jerseyTest.target("services/1/params")
                .request()
                .get();

        String json = response.readEntity(String.class);

        assertThat(response.getStatus()).isEqualTo(200);

        // main fields:
        assertThatJson(json).node("id").isEqualTo("1");
        assertThatJson(json).node("name").isEqualTo("VIEWER");
        assertThatJson(json).node("kind").isEqualTo("viewer");

        // only input param exists, output param is removed:
        assertThatJson(json).node("params").isArray().ofLength(1);
        assertThatJson(json).node("params[0].name").isEqualTo("input");
        assertThatJson(json).node("params[0].type").isEqualTo("file");

    }

    private void startGetServiceByIdRegistryMock(String kind, String cmdiFileName, String name) throws IOException {
        File cmdiFile = new File(getClass().getClassLoader().getResource(cmdiFileName).getFile());
        String cmdi = FileUtils.readFileToString(cmdiFile, UTF_8);

        JSONObject obj = new JSONObject();
        obj.put("id", "1");
        obj.put("name", name);
        obj.put("kind", kind);
        obj.put("semantics", cmdi);
        final String json = obj.toString();

        getMockServer()
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
