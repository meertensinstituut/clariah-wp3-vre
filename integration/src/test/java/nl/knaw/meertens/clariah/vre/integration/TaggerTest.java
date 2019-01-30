package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
import nl.knaw.meertens.clariah.vre.integration.util.Poller;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.surefire.shade.org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getRandomFilenameWithTime;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.apache.http.auth.AuthScope.ANY_HOST;
import static org.apache.http.auth.AuthScope.ANY_PORT;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggerTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private KafkaConsumerService taggerTopic;

    private ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
            Config.DB_OBJECTS_DATABASE, Config.DB_OBJECTS_USER, Config.DB_OBJECTS_PASSWORD
    );

    private long id;

    @Before
    public void setUp() {
        taggerTopic = getTaggerTopic();
    }

    @Test
    public void generateSystemTags_afterUploadNewFile() throws Exception {
        // Create object:
        String oldDir = "test-dir-" + RandomStringUtils.randomAlphabetic(8).toLowerCase();
        createDir(oldDir);
        final String expectedFilename = uploadTestFile(oldDir + "/" + getRandomFilenameWithTime(), getTestFileContent());
        id = Poller.awaitAndGet(() -> getObjectIdFromRegistry(expectedFilename));

        taggerTopic.consumeAll(records -> {
            ArrayList<String> expectedTypes = newArrayList(
                    "creation-time-ymdhm",
                    "creation-time-ymd",
                    "creation-time-ym",
                    "creation-time-y",
                    "modification-time-ymdhm",
                    "modification-time-ymd",
                    "modification-time-ym",
                    "modification-time-y",
                    "path",
                    "dir"
            );
            // TODO: should be exact
            assertThat(records.size()).isGreaterThanOrEqualTo(expectedTypes.size());
            ArrayList<String> allTypes = new ArrayList<>();
            records.forEach(record -> {
                String msg = JsonPath.parse(record.value()).read("$.msg");
                assertThat(msg).isEqualTo("Created new object tag");
                assertThatJson(record.value()).node("owner")
                        .isPresent()
                        .isEqualTo("system");
                assertThatJson(record.value()).node("object")
                        .isPresent()
                        // TODO:
                        // .isEqualTo(id)
                        ;
                assertThatJson(record.value()).node("tag")
                        .isPresent();
                Integer tagId = JsonPath.parse(record.value()).read("$.tag");
                allTypes.add(getTagType(tagId));
            });
            assertThat(allTypes).containsAll(expectedTypes);
        });

        String newDir = "test-dir-" + RandomStringUtils.randomAlphabetic(8).toLowerCase();
        createDir(newDir);
        String newFileName = expectedFilename.replace(oldDir, newDir);
        moveTestFileToNewDir(expectedFilename, newFileName);

        taggerTopic.consumeAll(records -> {
            ArrayList<String> expectedTypes = newArrayList(
                    "modification-time-ymdhm",
                    "modification-time-ymd",
                    "modification-time-ym",
                    "modification-time-y",
                    "path",
                    "dir"
            );
            logger.info("tagger tags: " + Arrays.toString(expectedTypes.toArray()));

            // TODO: should be exact
            assertThat(records.size()).isGreaterThanOrEqualTo(6);
            ArrayList<String> allTypes = new ArrayList<>();
            records.forEach(record -> {
                assertThatJson(record.value()).node("owner")
                        .isPresent()
                        .isEqualTo("system");
                assertThatJson(record.value()).node("object")
                        .isPresent()
                        // TODO:
                        // .isEqualTo(id)
                        ;
                assertThatJson(record.value()).node("tag")
                        .isPresent();
                Integer tagId = JsonPath.parse(record.value()).read("$.tag");
                allTypes.add(getTagType(tagId));
            });
            assertThat(allTypes).containsAll(expectedTypes);
        });
    }

    private String getTagType(Integer tagId) {
        final String[] type = new String[1];
        String query = "select * from tag WHERE id =" + tagId + ";";
        try {
            objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
                while (rs.next()) {
                    type[0] = rs.getString("type");
                }
                // When zero, no object has been found:
                assertThat(id).isNotZero();
            });
            logger.info(String.format("tag [%d] has type [%s]", tagId, type[0]));
            return type[0];
        } catch (SQLException e) {
            throw new RuntimeException("Could not get type of tag in registry", e);
        }

    }

    private void createDir(String originalDir) throws IOException, InterruptedException {
        logger.info(String.format("Create directory [%s]", originalDir));
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(ANY_HOST, ANY_PORT),
                new UsernamePasswordCredentials(Config.NEXTCLOUD_ADMIN_NAME, Config.NEXTCLOUD_ADMIN_PASSWORD)
        );
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        HttpUriRequest moveRequest = RequestBuilder
                .create("MKCOL")
                .setUri(Config.NEXTCLOUD_ENDPOINT + originalDir)
                .build();

        CloseableHttpResponse httpResponse = httpclient.execute(moveRequest);
        int status = httpResponse.getStatusLine().getStatusCode();
        assertThat(status).isEqualTo(201);

        // Consume msgs, because sometimes Nextcloud/Fits
        // marks a new dir as a txt file, which results in tags:
        TimeUnit.SECONDS.sleep(4);
        taggerTopic.findNewMessages(new ArrayList<>());
    }

    private String moveTestFileToNewDir(String oldFilename, String newFileName) throws IOException {
        logger.info(String.format("Rename file [%s] to [%s]", oldFilename, newFileName));
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(ANY_HOST, ANY_PORT),
                new UsernamePasswordCredentials(Config.NEXTCLOUD_ADMIN_NAME, Config.NEXTCLOUD_ADMIN_PASSWORD)
        );
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        HttpUriRequest moveRequest = RequestBuilder
                .create("MOVE")
                .setUri(Config.NEXTCLOUD_ENDPOINT + oldFilename)
                .addHeader(HttpHeaders.DESTINATION, Config.NEXTCLOUD_ENDPOINT + newFileName)
                .build();

        CloseableHttpResponse httpResponse = httpclient.execute(moveRequest);
        int status = httpResponse.getStatusLine().getStatusCode();
        assertThat(status).isEqualTo(201);
        return newFileName;
    }

    private KafkaConsumerService getTaggerTopic() {
        KafkaConsumerService taggerKafkaConsumer = new KafkaConsumerService(
                Config.KAFKA_ENDPOINT,
                Config.TAGGER_TOPIC_NAME,
                getRandomGroupName()
        );
        taggerKafkaConsumer.subscribe();
        taggerKafkaConsumer.pollOnce();
        return taggerKafkaConsumer;
    }

}
