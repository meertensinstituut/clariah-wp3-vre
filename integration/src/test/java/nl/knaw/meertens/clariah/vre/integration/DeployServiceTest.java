package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
import org.apache.maven.surefire.shade.org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;

public class DeployServiceTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private long id;
    private String someContent = "En het beeld werd een volmaakte verschrikking als men in zijn gevolg " +
            "dien reus zag aanslingeren met zijn slappen hals, zijn grooten bungelenden kop, en zijn mond " +
            "die zich boven een prooi kon openen, openen.";

    private final String resultFileName = "result.txt";

    /**
     * Test switchboard and deployment-service before, during and after deployment of TEST-service.
     * For details of TEST-service, see Test-class in deployment-service
     */
    @Test
    public void testDeployment_locksFiles_movesOutput_unlocksFiles() throws Exception {
        String inputFile = uploadTestFile(someContent);

        // wait for services to process new file:
        TimeUnit.SECONDS.sleep(6);

        long inputFileId = getObjectIdFromRegistry(inputFile);
        logger.info(String.format("input file has object id [%d]", inputFileId));

        String workDir = startDeploymentWithInputFileId(inputFileId);
        logger.info(String.format("deployment has workdir [%s]", workDir));

        checkDeploymentStatus(workDir, 6, "RUNNING");

        // wait for occ cronjob to scan all files:
        // (see owncloud/docker-scan-files.sh)
        TimeUnit.SECONDS.sleep(6);

        checkFileCanBeDownloaded(inputFile);
        
        checkFileIsLocked(inputFile);

        String newInputFile = uploadTestFile(someContent);

        // wait for services to process new file:
        TimeUnit.SECONDS.sleep(6);

        checkNewFileCanBeAdded(newInputFile);

        // wait for TEST deployment to finish:
        TimeUnit.SECONDS.sleep(6);
        String resultFile = checkDeploymentIsFinished(workDir);

        checkResultCanBeDownloaded(resultFile);

        checkFilesAreUnlocked(inputFile);

        checkKafkaMsgsAreCreatedForOutputFiles(resultFile);

        String secondNewInputFile = uploadTestFile(someContent);

        // wait for services to process new file:
        TimeUnit.SECONDS.sleep(6);

        checkNewFileCanBeAdded(secondNewInputFile);

    }

    private void checkFileCanBeDownloaded(String inputFile) throws UnirestException {
        logger.info(String.format("check file [%s] can be downloaded", inputFile));
        HttpResponse<String> downloadResult = downloadFile(inputFile);
        assertThat(downloadResult.getBody()).isEqualTo(someContent);
        assertThat(downloadResult.getStatus()).isEqualTo(200);
    }

    private void checkFilesAreUnlocked(String inputFile) throws UnirestException {
        logger.info(String.format("check file [%s] is unlocked", inputFile));
        HttpResponse<String> downloadResult = downloadFile(inputFile);
        assertThat(downloadResult.getBody()).isEqualTo(someContent);
        assertThat(downloadResult.getStatus()).isIn(200, 202);

        HttpResponse<String> putAfterDeployment = putInputFile(inputFile);
        assertThat(putAfterDeployment.getStatus()).isEqualTo(204);

        HttpResponse<String> deleteInputFile = deleteInputFile(inputFile);
        assertThat(deleteInputFile.getStatus()).isEqualTo(204);
    }

    private void checkFileIsLocked(String inputFile) throws UnirestException, InterruptedException {
        logger.info(String.format("check file [%s] is locked", inputFile));
        HttpResponse<String> putAfterDeployment = putInputFile(inputFile);
        // http 423 is 'locked'
        assertThat(putAfterDeployment.getStatus()).isIn(403, 423, 500);
        
        // http 423 is 'locked'
        HttpResponse<String> deleteInputFile = deleteInputFile(inputFile);
        assertThat(deleteInputFile.getStatus()).isIn(403, 423);
    }

    private void checkResultCanBeDownloaded(String resultFile) throws UnirestException {
        logger.info(String.format("check file [%s] can be downloaded", resultFile));
        HttpResponse<String> downloadResultTxt = downloadFile(resultFile);
        assertThat(downloadResultTxt.getBody()).contains("Insanity");
    }

    private String checkDeploymentIsFinished(String workDir) throws UnirestException, InterruptedException {
        logger.info(String.format("check deployment [%s] is finished", workDir));
        HttpResponse<String> statusResponse = checkDeploymentStatus(workDir, 3, "FINISHED");
        String outputFilePath = getOutputFilePath(statusResponse);
        logger.info(String.format("deployment has result file [%s]", outputFilePath));
        return outputFilePath;
    }

    private void checkKafkaMsgsAreCreatedForOutputFiles(String outputFilename) throws InterruptedException {
        logger.info(String.format("check kafka message is created for output file [%s]", outputFilename));
        KafkaConsumerService owncloudKafkaConsumer = getOwncloudTopic();
        owncloudKafkaConsumer.consumeAll(consumerRecords -> {
            assertThat(consumerRecords.size()).isGreaterThan(0);
            List<String> resultActions = new ArrayList<>();
            consumerRecords.forEach(record -> {
                String filePath = JsonPath.parse(record.value()).read("$.path");
                if (filePath.contains(outputFilename)) {
                    resultActions.add(JsonPath.parse(record.value()).read("$.action"));
                }
            });
            assertThat(resultActions).hasSize(1);
            assertThat(resultActions.get(0)).isEqualTo("create");
        });
    }

    private void checkNewFileCanBeAdded(String newInputFile) throws SQLException {
        logger.info("check that a new file is added");
        long newInputFileId = getObjectIdFromRegistry(newInputFile);
        assertThat(newInputFileId).isNotEqualTo(0L);
    }

    private String getOutputFilePath(HttpResponse<String> finishedDeployment) {
        String outputDir = JsonPath.parse(finishedDeployment.getBody()).read("$.outputDir");
        Path pathAbsolute = Paths.get(outputDir);
        Path pathBase = Paths.get("admin/files/");
        Path pathRelative = pathBase.relativize(pathAbsolute);
        String outputPath = Paths.get(pathRelative.toString(), resultFileName).toString();
        logger.info(String.format("output file path is [%s]", outputPath));
        return outputPath;
    }

    private HttpResponse<String> downloadFile(String inputFile) throws UnirestException {
        return Unirest
                .get(OWNCLOUD_ENDPOINT + inputFile)
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .asString();
    }

    private HttpResponse<String> putInputFile(String expectedFilename) throws UnirestException {
        return Unirest
                .put(OWNCLOUD_ENDPOINT + expectedFilename)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .body("new content " + RandomStringUtils.random(8))
                .asString();
    }

    private HttpResponse<String> deleteInputFile(String expectedFilename) throws UnirestException {
        return Unirest
                .delete(OWNCLOUD_ENDPOINT + expectedFilename)
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .asString();
    }

    private KafkaConsumerService getOwncloudTopic() {
        KafkaConsumerService recognizerKafkaConsumer = new KafkaConsumerService(
                KAFKA_ENDPOINT, OWNCLOUD_TOPIC_NAME, getRandomGroupName());
        recognizerKafkaConsumer.subscribe();
        recognizerKafkaConsumer.pollOnce();
        return recognizerKafkaConsumer;
    }


}
