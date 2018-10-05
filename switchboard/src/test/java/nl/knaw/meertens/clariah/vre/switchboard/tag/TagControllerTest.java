package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil.getMockServer;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class TagControllerTest extends AbstractControllerTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void createTag () throws JsonProcessingException {
        TagDto tag = new TagDto();
        tag.name = "2018";
        tag.type = "date";
        tag.owner = "test";

        startPostTagMock(tag);

        Entity<TagDto> tagEntity = Entity.entity(tag, MediaType.APPLICATION_JSON_TYPE);
        Response response = jerseyTest.target("tags")
                .request()
                .post(tagEntity);

        String json = response.readEntity(String.class);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThatJson(json).node("id").isEqualTo(5);
    }

    private void startPostTagMock(TagDto tag) throws JsonProcessingException {
        getMockServer()
                .when(
                        request()
                                .withMethod("POST")
                                .withBody(getTestFileContent("new-tag.json"))
                                .withPath("/_table/tag")
                ).respond(
                response()
                        .withStatusCode(200)
                        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                        .withBody("{\"resource\": [{\"id\": 5}]}")
        );
    }

}
