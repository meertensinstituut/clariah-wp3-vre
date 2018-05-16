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

@Ignore
public class UploadingNewFileTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

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

}
