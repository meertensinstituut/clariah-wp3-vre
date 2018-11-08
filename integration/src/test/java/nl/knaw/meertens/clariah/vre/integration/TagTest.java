package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.maven.surefire.shade.org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.pollAndAssert;

public class TagTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void tagAndUntagObject() throws UnirestException {
        // Create tag:
        String randomTagName = "test-tag-" + RandomStringUtils.random(8);
        HttpResponse<String> responseCreatingTag = Unirest
                .post(Config.SWITCHBOARD_ENDPOINT + "/tags")
                .header("Content-Type", "application/json; charset=UTF-8")
                .body("{\"name\": \"" + randomTagName + "\", \"type\": \"test\"}")
                .asString();
        logger.info("Response creating tag: " + responseCreatingTag.getBody());
        assertThatJson(responseCreatingTag.getBody()).node("id").isPresent();
        Integer tagId = JsonPath.parse(responseCreatingTag.getBody()).read("$.id", Integer.class);

        // Create object:
        final String expectedFilename = uploadTestFile();
        Long objectId = pollAndAssert(() -> getObjectIdFromRegistry(expectedFilename));

        // Tag object:
        HttpResponse<String> responseTagging = Unirest
                .post(Config.SWITCHBOARD_ENDPOINT + "/tags/" + tagId + "/objects/")
                .header("Content-Type", "application/json; charset=UTF-8")
                .body("{\"object\": " + objectId + "}")
                .asString();
        logger.info("Response tagging: " + responseTagging.getBody());
        assertThatJson(responseTagging.getBody()).node("id").isPresent();
        Integer tagObjectId = JsonPath.parse(responseTagging.getBody()).read("$.id", Integer.class);

        // Untag object:
        HttpResponse<String> responseUntagging = Unirest
                .delete(Config.SWITCHBOARD_ENDPOINT + "/tags/" + tagId + "/objects/" + objectId)
                .header("Content-Type", "application/json; charset=UTF-8")
                .asString();
        logger.info("Response untagging: " + responseUntagging.getBody());
        assertThatJson(responseUntagging .getBody()).node("id").isPresent();

        // Delete tag:
        HttpResponse<String> responseDeletingTag = Unirest
                .delete(Config.SWITCHBOARD_ENDPOINT + "/tags/" + tagId)
                .header("Content-Type", "application/json; charset=UTF-8")
                .asString();
        logger.info("Response deleting tag: " + responseDeletingTag.getBody());
        assertThatJson(responseDeletingTag .getBody()).node("id").isPresent();

    }

}
