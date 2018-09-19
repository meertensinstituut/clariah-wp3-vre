package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.checkDeploymentStatus;
import static nl.knaw.meertens.clariah.vre.integration.util.DeployUtils.startDeploymentWithInputFileId;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.checkNewFileCanBeAdded;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.deleteInputFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.downloadFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileCanBeDownloaded;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileIsLocked;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.putInputFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.pollUntil;
import static org.assertj.core.api.Assertions.assertThat;

public class DeployServiceTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private long id;
    private String deploymentTestFile = "deployment-test.txt";

    /**
     * Test switchboard and deployment-service before, during and after deployment of TEST-service.
     * For details of TEST-service, see Test-class in deployment-service
     */
    @Test
    public void testDeployment_locksFiles_movesOutput_unlocksFiles() throws Exception {
        String testFileContent = getTestFileContent(deploymentTestFile);
        String testFilename = uploadTestFile(testFileContent);

        pollUntil(() -> fileCanBeDownloaded(testFilename, testFileContent), maxPollPeriod);
        pollUntil(() -> fileExistsInRegistry(testFilename), maxPollPeriod);
        long inputFileId = pollUntil(() -> getObjectIdFromRegistry(testFilename), maxPollPeriod);
        logger.info(String.format("input file has object id [%d]", inputFileId));

        String workDir = startDeploymentWithInputFileId(inputFileId);
        logger.info(String.format("deployment has workdir [%s]", workDir));

        checkDeploymentStatus(workDir, 20, "RUNNING");

        // Wait for occ cronjob to scan all files:
        // (see owncloud/docker-scan-files.sh)
        TimeUnit.SECONDS.sleep(10);

        pollUntil(() -> fileCanBeDownloaded(testFilename, testFileContent), maxPollPeriod);
        pollUntil(() -> fileIsLocked(testFilename), maxPollPeriod);

        String newInputFile = uploadTestFile(testFileContent);

        pollUntil(() -> checkNewFileCanBeAdded(newInputFile), maxPollPeriod);

        String resultFile = pollUntil(() -> deploymentIsFinished(workDir), maxPollPeriod);

        pollUntil(() -> fileCanBeDownloaded(resultFile, getTestFileContent("test-result.txt")), maxPollPeriod);

        pollUntil(() -> checkFilesAreUnlocked(testFilename), maxPollPeriod);

        checkKafkaMsgsAreCreatedForOutputFiles(resultFile);

        String secondNewInputFile = uploadTestFile(testFileContent);

        pollUntil(() -> checkNewFileCanBeAdded(secondNewInputFile), maxPollPeriod);

    }

    private void checkFilesAreUnlocked(String inputFile) {
        try {
            logger.info(String.format("check file [%s] is unlocked", inputFile));
            HttpResponse<String> downloadResult = downloadFile(inputFile);
            assertThat(downloadResult.getBody()).isEqualTo(getTestFileContent(deploymentTestFile));
            assertThat(downloadResult.getStatus()).isIn(200, 202);

            HttpResponse<String> putAfterDeployment = null;
            putAfterDeployment = putInputFile(inputFile);
            assertThat(putAfterDeployment.getStatus()).isEqualTo(204);

            HttpResponse<String> deleteInputFile = deleteInputFile(inputFile);
            assertThat(deleteInputFile.getStatus()).isEqualTo(204);
        } catch (UnirestException e) {
            throw new RuntimeException("Could not check files are unlocked", e);
        }
    }

    private void checkResultCanBeDownloaded(String resultFile) throws UnirestException {
        logger.info(String.format("check file [%s] can be downloaded", resultFile));
        HttpResponse<String> downloadResultTxt = downloadFile(resultFile);
        assertThat(downloadResultTxt.getBody()).contains("Insanity");
    }

    private String deploymentIsFinished(String workDir) {
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

    private String getOutputFilePath(HttpResponse<String> finishedDeployment) {
        String outputDir = JsonPath.parse(finishedDeployment.getBody()).read("$.outputDir");
        Path pathAbsolute = Paths.get(outputDir);
        Path pathBase = Paths.get("admin/files/");
        Path pathRelative = pathBase.relativize(pathAbsolute);
        String resultFileName = "result.txt";
        String outputPath = Paths.get(pathRelative.toString(), resultFileName).toString();
        logger.info(String.format("output file path is [%s]", outputPath));
        return outputPath;
    }

    private KafkaConsumerService getOwncloudTopic() {
        KafkaConsumerService recognizerKafkaConsumer = new KafkaConsumerService(
                Config.KAFKA_ENDPOINT, Config.OWNCLOUD_TOPIC_NAME, getRandomGroupName());
        recognizerKafkaConsumer.subscribe();
        recognizerKafkaConsumer.pollOnce();
        return recognizerKafkaConsumer;
    }

}
