package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
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

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class DeployServiceTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private long id;
    private String someContent = "En het beeld werd een volmaakte verschrikking als men in zijn gevolg " +
            "dien reus zag aanslingeren met zijn slappen hals, zijn grooten bungelenden kop, en zijn mond " +
            "die zich boven een prooi kon openen, openen.";

    @Before
    public void setUp( ) {
        id = 0;
    }

    @Test
    public void testDeployment_locksFiles_movesOutput_unlocksFiles() throws Exception {

        String inputFile = uploadTestFile(someContent);

        TimeUnit.SECONDS.sleep(6);

        long inputFileId = getObjectIdFromRegistry(inputFile);

        String workDir = startDeploymentWithInputFileId(inputFileId);

        TimeUnit.SECONDS.sleep(1);

        checkStatusIsRunning(workDir);

        checkFileCanBeDownloaded(inputFile);

        checkFileIsLocked(inputFile);

        checkNewFileCanBeAddedIn7Seconds();

        String resultFile = checkDeploymentIsFinished(workDir, "result.txt");

        checkResultCanBeDownloaded(resultFile);

        checkFilesAreUnlocked(inputFile);

        checkKafkaMsgsAreCreatedForOutputFiles(resultFile);

        checkNewFileCanBeAddedIn7Seconds();

    }

    private void checkFileCanBeDownloaded(String inputFile) throws UnirestException {
        HttpResponse<String> downloadResult = downloadFile(inputFile);
        assertThat(downloadResult.getBody()).isEqualTo(someContent);
        assertThat(downloadResult.getStatus()).isEqualTo(200);
    }

    private void checkFilesAreUnlocked(String inputFile) throws UnirestException {
        HttpResponse<String> downloadResult = downloadFile(inputFile);
        assertThat(downloadResult.getBody()).isEqualTo(someContent);
        assertThat(downloadResult.getStatus()).isIn(200, 202);

        HttpResponse<String> putAfterDeployment = putInputFile(inputFile);
        assertThat(putAfterDeployment.getStatus()).isEqualTo(204);

        HttpResponse<String> deleteInputFile = deleteInputFile(inputFile);
        assertThat(deleteInputFile.getStatus()).isEqualTo(204);
    }

    private void checkStatusIsRunning(String workDir) throws UnirestException {
        HttpResponse<String> getDeploymentStatus = getDeploymentStatus(workDir);
        assertThat(getDeploymentStatus.getStatus()).isIn(200, 202);
        assertThatJson(getDeploymentStatus.getBody()).node("status").matches(containsString("RUNNING"));
    }

    private void checkFileIsLocked(String inputFile) throws UnirestException {
        HttpResponse<String> putAfterDeployment = putInputFile(inputFile);
        assertThat(putAfterDeployment.getStatus()).isIn(403, 500);

        HttpResponse<String> deleteInputFile = deleteInputFile(inputFile);
        assertThat(deleteInputFile.getStatus()).isEqualTo(403);
    }

    private void checkResultCanBeDownloaded(String resultFile) throws UnirestException {
        HttpResponse<String> downloadResultTxt = downloadFile(resultFile);
        assertThat(downloadResultTxt.getBody()).contains("Insanity");
    }

    private String checkDeploymentIsFinished(String workDir, String resultFileName) throws UnirestException {
        HttpResponse<String> finishedDeployment = getDeploymentStatus(workDir);
        assertThat(finishedDeployment.getStatus()).isIn(200, 202);
        logger.info("finished deployment result: " + finishedDeployment.getBody());
        assertThatJson(finishedDeployment.getBody()).node("status").matches(containsString("FINISHED"));
        return getOutputFilePath(finishedDeployment, resultFileName);
    }

    private void checkKafkaMsgsAreCreatedForOutputFiles(String outputFilename) throws InterruptedException {
        KafkaConsumerService owncloudKafkaConsumer = getOwncloudTopic();
        owncloudKafkaConsumer.consumeAll(consumerRecords -> {
            assertThat(consumerRecords.size()).isGreaterThan(0);
            List<String> resultActions = new ArrayList<>();
            consumerRecords.forEach(record -> {
                String filePath = JsonPath.parse(record.value()).read("$.userPath");
                if(filePath.contains(outputFilename)) {
                    resultActions.add(JsonPath.parse(record.value()).read("$.action"));
                }
            });
            assertThat(resultActions).hasSize(1);
            assertThat(resultActions.get(0)).isEqualTo("create");
        });
    }

    private void checkNewFileCanBeAddedIn7Seconds() throws UnirestException, InterruptedException, SQLException {
        // Check a new file can be added:
        String newInputFile = uploadTestFile(someContent);
        assertThat(downloadFile(newInputFile).getStatus()).isEqualTo(200);
        TimeUnit.SECONDS.sleep(7);
        long newInputFileId = getObjectIdFromRegistry(newInputFile);
        assertThat(newInputFileId).isNotEqualTo(0L);
    }

    private String getOutputFilePath(HttpResponse<String> finishedDeployment, String resultFileName) {
        String outputDir = JsonPath.parse(finishedDeployment.getBody()).read("$.outputDir");
        Path pathAbsolute = Paths.get(outputDir);
        Path pathBase = Paths.get("admin/files/");
        Path pathRelative = pathBase.relativize(pathAbsolute);
        return Paths.get(pathRelative.toString(), resultFileName).toString();
    }

    private long getObjectIdFromRegistry(String inputFile) throws SQLException {
        ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
                DB_OBJECTS_DATABASE, DB_OBJECTS_USER, DB_OBJECTS_PASSWORD);
        String query = "select * from object WHERE filepath LIKE '%" + inputFile + "%' LIMIT 1;";
        objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
            while (rs.next()) {
                id = (long) rs.getInt("id");
            }
            // When zero, no object has been found:
            assertThat(id).isNotZero();
        });
        logger.info(String.format("uploaded file [%s] has object id [%d]", inputFile, id));
        return id;
    }

    private HttpResponse<String> getDeploymentStatus(String workDir) throws UnirestException {
        return Unirest
                    .get(SWITCHBOARD_ENDPOINT + "exec/task/" + workDir + "/")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .asString();
    }

    private HttpResponse<String> downloadFile(String inputFile) throws UnirestException {
        return Unirest
                    .get(OWNCLOUD_ENDPOINT + inputFile)
                    .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                    .asString();
    }

    private String startDeploymentWithInputFileId(Long expectedFilename) throws UnirestException {
        HttpResponse<String> result = Unirest
                .post(SWITCHBOARD_ENDPOINT + "exec/TEST")
                .header("Content-Type", "application/json; charset=UTF-8")
                .body("{\"params\":[{\"name\":\"untokinput\",\"type\":\"file\",\"value\":\"" + expectedFilename + "\",\"params\":[{\"language\":\"eng\",\"author\":\"J. Jansen\"}]}]}")
                .asString();

        assertThat(result.getStatus()).isIn(200, 202);
        return JsonPath.parse(result.getBody()).read("$.workDir");
    }

    private HttpResponse<String> putInputFile(String expectedFilename) throws UnirestException {
        return Unirest
                    .put(OWNCLOUD_ENDPOINT + expectedFilename)
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                    .body("new content")
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
