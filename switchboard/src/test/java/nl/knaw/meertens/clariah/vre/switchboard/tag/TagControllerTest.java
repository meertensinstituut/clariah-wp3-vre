package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.knaw.meertens.clariah.vre.switchboard.AbstractControllerTest;
import org.junit.Test;
import org.mockserver.model.Header;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.TEST_USER;
import static nl.knaw.meertens.clariah.vre.switchboard.SwitchboardDIBinder.getMapper;
import static nl.knaw.meertens.clariah.vre.switchboard.util.FileUtil.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.switchboard.util.MockServerUtil.getMockServer;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class TagControllerTest extends AbstractControllerTest {

    @Test
    public void createTag() {
        TagDto tag = new TagDto();
        tag.name = "2018";
        tag.type = "date";

        startPostTagMock();

        Entity<TagDto> tagEntity = Entity.entity(tag, MediaType.APPLICATION_JSON_TYPE);
        Response response = jerseyTest.target("tags")
                .request()
                .post(tagEntity);

        String json = response.readEntity(String.class);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThatJson(json).node("id").isEqualTo(5);
    }

    @Test
    public void tagObject_succeeds() throws JsonProcessingException {
        ObjectTagDto objectTag = new ObjectTagDto();
        objectTag.object = 2L;

        startTagObjectMock(200, "{\"id\": 6}");

        String entity = getMapper().writeValueAsString(objectTag);
        Entity<String> tagEntity = Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE);

        Response response = jerseyTest.target("tags/1/objects")
                .request()
                .post(tagEntity);

        String json = response.readEntity(String.class);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThatJson(json).node("id").isEqualTo(6);
    }

    @Test
    public void tagObject_fails_whenObjectDoesNotExist() throws JsonProcessingException {
        ObjectTagDto objectTag = new ObjectTagDto();
        objectTag.object = 2L;

        startTagObjectMock(
                500,
                getTestFileContent("new-object-tag-response-when-object-does-not-exist.json")
        );

        String entity = getMapper().writeValueAsString(objectTag);
        Entity<String> tagEntity = Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE);

        Response response = jerseyTest.target("tags/1/objects")
                .request()
                .post(tagEntity);

        String json = response.readEntity(String.class);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThatJson(json).node("msg").isEqualTo("Could not create object tag. Tag or object does not exist.");
    }

    @Test
    public void tagObject_fails_whenObjectTagAlreadyExists() throws JsonProcessingException {
        ObjectTagDto objectTag = new ObjectTagDto();
        objectTag.object = 2L;

        startTagObjectMock(
                500,
                getTestFileContent("new-object-tag-response-when-object-tag-already-exists.json")
        );

        String entity = getMapper().writeValueAsString(objectTag);
        Entity<String> tagEntity = Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE);

        Response response = jerseyTest.target("tags/1/objects")
                .request()
                .post(tagEntity);

        String json = response.readEntity(String.class);
        assertThat(response.getStatus()).isEqualTo(500);
        assertThatJson(json).node("msg").isEqualTo("Could not create object tag. Object tag already exists.");
    }

    @Test
    public void untagObject_succeeds() throws JsonProcessingException {
        startUntagObjectMock();

        Response response = jerseyTest.target("tags/1/objects/2")
                .request()
                .delete();

        String json = response.readEntity(String.class);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThatJson(json).node("id").isEqualTo(3);
    }

    private void startPostTagMock() {
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

    private void startTagObjectMock(int statusCode, String body) {
        getMockServer()
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/_func/insert_object_tag")
                                .withQueryStringParameter("_tag", "1")
                                .withQueryStringParameter("_object", "2")
                                .withQueryStringParameter("_owner", "test")
                ).respond(
                response()
                        .withStatusCode(statusCode)
                        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                        .withBody(body)
        );
    }

    private void startUntagObjectMock() {
        getMockServer()
                .when(
                        request()
                                .withMethod("DELETE")
                                .withPath("/_table/object_tag")
                                .withQueryStringParameter("filter", "(tag = 1) AND (object = 2)")
                ).respond(
                response()
                        .withStatusCode(200)
                        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
                        .withBody("{\"resource\":[{\"id\":\"3\"}]}")
        );
    }

}
