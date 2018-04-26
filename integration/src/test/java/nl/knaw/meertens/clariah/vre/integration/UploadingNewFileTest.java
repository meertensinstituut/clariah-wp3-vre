package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.http.auth.AuthScope.ANY_HOST;
import static org.apache.http.auth.AuthScope.ANY_PORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class UploadingNewFileTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static Integer id;

    @Test
    public void testOwncloudFileUpload() throws Exception {
        logger.info("Test upload and download of file");
        String expectedFilename = getRandomFilenameWithTime();
        byte[] expectedFileContent = getTestFileContent();

        logger.info("Uploading file...");
        HttpResponse<String> uploadResult = Unirest
                .put(OWNCLOUD_ENDPOINT + expectedFilename)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .basicAuth("admin", "admin")
                .body(expectedFileContent)
                .asString();
        assertThat(uploadResult.getStatus()).isEqualTo(201);
        logger.info("Uploaded file");

        logger.info("Downloading file...");
        HttpResponse<String> downloadResult = Unirest
                .get(OWNCLOUD_ENDPOINT + expectedFilename)
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .asString();
        assertThat(downloadResult.getBody()).isEqualTo(new String(expectedFileContent));
        logger.info("Downloaded file");

    }

    @Test
    public void testOwncloudProducesKafkaMessagesAfterFileUpload() throws Exception {
        String expectedFilename = getRandomFilenameWithTime();
        List<String> expectedActions = newArrayList(
                "create"
        );

        KafkaConsumerService owncloudKafkaConsumer = new KafkaConsumerService(
                KAFKA_ENDPOINT, OWNCLOUD_TOPIC_NAME, getRandomGroupName());
        owncloudKafkaConsumer.subscribe();
        owncloudKafkaConsumer.pollOnce();

        logger.info("Uploading file...");
        Unirest.put(OWNCLOUD_ENDPOINT + expectedFilename)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .body(getTestFileContent())
                .asString();
        logger.info("Uploaded file");

        owncloudKafkaConsumer.consumeAll(consumerRecords -> {
            logger.info("consumerRecords: " + Arrays.toString(consumerRecords.toArray()));
            assertThat(consumerRecords.size()).isGreaterThanOrEqualTo(expectedActions.size());

            List<String> resultActions = new ArrayList<>();
            consumerRecords.forEach(record -> {
                String filePath = JsonPath.parse(record.value()).read("$.path");
                if (filePath.contains(expectedFilename)) {
                    resultActions.add(JsonPath.parse(record.value()).read("$.action"));
                }
            });
            assertThat(resultActions).hasSize(expectedActions.size());
            assertThat(resultActions).containsAll(expectedActions);
        });

    }

    @Test
    public void testRecognizerProducesKafkaMessagesAfterFileUpload() throws Exception {
        KafkaConsumerService recognizerKafkaConsumer = getRecognizerTopic();

        final String expectedFilename = uploadTestFile();
        logger.info("Uploaded file");
        TimeUnit.SECONDS.sleep(5);

        recognizerKafkaConsumer.consumeAll(consumerRecords -> {
            logger.info("Check recognizer results");
            ConsumerRecord<String, String> testFileResultFromRecognizer = findTestFile(expectedFilename, consumerRecords);
            assertThat(testFileResultFromRecognizer).isNotNull();
            String foundMimetype = JsonPath.parse(testFileResultFromRecognizer.value()).read("fitsMimetype");
            assertThat(foundMimetype).isEqualTo("text/plain");
            String foundFitdFullResult = JsonPath.parse(testFileResultFromRecognizer.value()).read("fitsFullResult");
            assertThat(foundFitdFullResult).contains("<fits xmlns=\"http://hul.harvard.edu/ois/xml/ns/fits/fits_output\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        });

    }

    @Test
    public void testRecognizer_adds_updates_deletes_recordInObjectsRegistry() throws Exception {
        final String expectedFilename = uploadTestFile();
        TimeUnit.SECONDS.sleep(6);

        ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
                DB_OBJECTS_DATABASE, DB_OBJECTS_USER, DB_OBJECTS_PASSWORD);

        String count = "SELECT count(*) AS exact_count FROM object;";
        objectsRepositoryService.processQuery(count, (ResultSet rs) -> {
            rs.next();
            int recordsCount = rs.getInt("exact_count");
            assertThat(recordsCount).isGreaterThanOrEqualTo(1);
        });
        String query = "select * from object WHERE filepath LIKE '%" + expectedFilename + "%' LIMIT 1;";
        objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
            while (rs.next()) {
                id = rs.getInt("id");
                assertThat(id).isNotZero();
                assertThat(rs.getString("filepath")).contains(expectedFilename);
                assertThat(rs.getString("format")).isEqualTo("Plain text");
                assertThat(rs.getString("mimetype")).isEqualTo("text/plain");
            }
        });

        String newHtmlFileName = getRandomFilenameWithTime().split("\\.")[0] + ".html";
        updateTestFilePath(expectedFilename, newHtmlFileName);
        TimeUnit.SECONDS.sleep(6);

        String query2 = "select * from object WHERE id=" + id;
        objectsRepositoryService.processQuery(query2, (ResultSet rs) -> {
            while (rs.next()) {
                assertThat(rs.getString("filepath")).contains(newHtmlFileName);
                assertThat(rs.getString("format")).isEqualTo("Plain text");
                assertThat(rs.getString("mimetype")).isEqualTo("text/plain");
            }
        });

        updateContentToHtml(newHtmlFileName);
        TimeUnit.SECONDS.sleep(6);

        String queryHtml = "select * from object WHERE id=" + id;
        objectsRepositoryService.processQuery(queryHtml, (ResultSet rs) -> {
            while (rs.next()) {
                assertThat(rs.getString("filepath")).contains(newHtmlFileName);
                assertThat(rs.getString("mimetype")).isEqualTo("text/html");
            }
        });

        deleteFile(newHtmlFileName);
        TimeUnit.SECONDS.sleep(6);

        String countAfterDelete = "SELECT count(*) AS exact_count FROM object WHERE id=" + id;
        objectsRepositoryService.processQuery(countAfterDelete, (ResultSet rs) -> {
            rs.next();
            int recordsCount = rs.getInt("exact_count");
            assertThat(recordsCount).isGreaterThanOrEqualTo(0);
        });

    }

    private void updateContentToHtml(String newFileName) throws UnirestException {
        logger.info("Add html to html file");
        Unirest.put(OWNCLOUD_ENDPOINT + newFileName)
                .header("Content-Type", "text/html; charset=utf-8") // set type to html
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .body("<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "<div>Lorem ipsum!</div></body></html>")
                .asString();
    }

    private void updateTestFilePath(String oldFilename, String newFileName) throws IOException {
        logger.info(String.format("Rename file [%s] to [%s]", oldFilename, newFileName));
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(ANY_HOST, ANY_PORT),
                new UsernamePasswordCredentials(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
        );
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        HttpUriRequest moveRequest = RequestBuilder
                .create("MOVE")
                .setUri(OWNCLOUD_ENDPOINT + oldFilename)
                .addHeader(HttpHeaders.DESTINATION, OWNCLOUD_ENDPOINT + newFileName)
                .build();

        CloseableHttpResponse httpResponse = httpclient.execute(moveRequest);
        int status = httpResponse.getStatusLine().getStatusCode();
        assertThat(status).isEqualTo(201);
    }

    private ConsumerRecord<String, String> findTestFile(String expectedFilename, List<ConsumerRecord<String, String>> consumerRecords) {
        ConsumerRecord<String, String> testRecord = null;
        for (ConsumerRecord<String, String> record : consumerRecords) {
            File resultPath = new File(JsonPath.parse(record.value()).read("$.path").toString());
            String resultFilename = resultPath.getName();
            if (resultFilename.equals(expectedFilename)) {
                testRecord = record;
            }
        }
        return testRecord;
    }

    private KafkaConsumerService getRecognizerTopic() {
        KafkaConsumerService recognizerKafkaConsumer = new KafkaConsumerService(
                KAFKA_ENDPOINT, RECOGNIZER_TOPIC_NAME, getRandomGroupName());
        recognizerKafkaConsumer.subscribe();
        recognizerKafkaConsumer.pollOnce();
        return recognizerKafkaConsumer;
    }

    private void deleteFile(String file) throws UnirestException {
        logger.info(String.format("Delete file [%s]", file));
        HttpResponse<String> response = Unirest.delete(OWNCLOUD_ENDPOINT + file)
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .asString();
        assertThat(response.getStatus()).isEqualTo(204);
    }

}
