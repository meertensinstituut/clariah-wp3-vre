package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    public void testFilesAreLocked_whenServiceIsDeployed() throws Exception {

        String inputFile = uploadTestFile(someContent);

        HttpResponse<String> downloadResult = downloadFile(inputFile);
        assertThat(downloadResult.getBody()).isEqualTo(someContent);
        assertThat(downloadResult.getStatus()).isEqualTo(200);

        TimeUnit.SECONDS.sleep(6);

        long inputFileId = getObjectIdFromRegistry(inputFile);

        HttpResponse<String> startDeployment = startDeploymentWithInputFileId(inputFileId);
        assertThat(startDeployment.getStatus()).isEqualTo(200);

        HttpResponse<String> putAfterDeployment = putInputFile(inputFile);
        assertThat(putAfterDeployment.getStatus()).isIn(403, 500);

        HttpResponse<String> deleteInputFile = deleteInputFile(inputFile);
        assertThat(deleteInputFile.getStatus()).isEqualTo(403);

        String workDir = JsonPath.parse(startDeployment.getBody()).read("$.workDir");

        HttpResponse<String> getDeploymentStatus = getDeploymentStatus(workDir);
        assertThat(getDeploymentStatus.getStatus()).isIn(200, 202);
        assertThatJson(getDeploymentStatus.getBody()).node("status").matches(containsString("RUNNING"));

        TimeUnit.SECONDS.sleep(6);

        HttpResponse<String> getDeploymentStatusAfter5Secs = getDeploymentStatus(workDir);
        assertThat(getDeploymentStatus.getStatus()).isIn(200, 202);
        assertThatJson(getDeploymentStatusAfter5Secs.getBody()).node("status").matches(containsString("FINISHED"));

        HttpResponse<String> downloadResult2 = downloadFile(inputFile);
        assertThat(downloadResult2.getBody()).isEqualTo(someContent);
        assertThat(downloadResult2.getStatus()).isIn(200, 202);

        HttpResponse<String> putAfterDeployment2 = putInputFile(inputFile);
        assertThat(putAfterDeployment2.getStatus()).isEqualTo(204);

        HttpResponse<String> deleteInputFile2 = deleteInputFile(inputFile);
        assertThat(deleteInputFile2.getStatus()).isEqualTo(204);

    }

    @Test
    public void testThatResultOfDeployment_isMovedToOwncloud() throws UnirestException, InterruptedException, SQLException {
        // Upload file:
        String inputFile = uploadTestFile(someContent);
        assertThat(downloadFile(inputFile).getStatus()).isEqualTo(200);
        TimeUnit.SECONDS.sleep(6);
        long inputFileId = getObjectIdFromRegistry(inputFile);

        // Start deployment:
        HttpResponse<String> startDeployment = startDeploymentWithInputFileId(inputFileId);
        assertThat(startDeployment.getStatus()).isEqualTo(200);
        String workDir = JsonPath.parse(startDeployment.getBody()).read("$.workDir");

        // Wait until finish:
        TimeUnit.SECONDS.sleep(6);

        // Find output dir:
        HttpResponse<String> finishedDeployment = getDeploymentStatus(workDir);
        assertThat(finishedDeployment.getStatus()).isIn(200, 202);
        assertThatJson(finishedDeployment.getBody()).node("status").matches(containsString("FINISHED"));
        String resultFile = getOutputPath(finishedDeployment);

        // Download result:
        HttpResponse<String> downloadResultTxt = downloadFile(resultFile);
        assertThat(downloadResultTxt.getBody()).contains("Insanity");

    }

    private String getOutputPath(HttpResponse<String> finishedDeployment) {
        String outputDir = JsonPath.parse(finishedDeployment.getBody()).read("$.outputDir");
        Path pathAbsolute = Paths.get(outputDir);
        Path pathBase = Paths.get("admin/files/");
        Path pathRelative = pathBase.relativize(pathAbsolute);
        return Paths.get(pathRelative.toString(), "result.txt").toString();
    }


    private long getObjectIdFromRegistry(String inputFile) throws SQLException {
        ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
                DB_OBJECTS_DATABASE, DB_OBJECTS_USER, DB_OBJECTS_PASSWORD);
        String query = "select * from object WHERE filepath LIKE '%" + inputFile + "%' LIMIT 1;";
        objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
            while (rs.next()) {
                id = (long) rs.getInt("id");
            }
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

    private HttpResponse<String> startDeploymentWithInputFileId(Long expectedFilename) throws UnirestException {
        return Unirest
                    .post(SWITCHBOARD_ENDPOINT + "exec/TEST")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body("{\"params\":[{\"name\":\"untokinput\",\"type\":\"file\",\"value\":\"" + expectedFilename + "\",\"params\":[{\"language\":\"eng\",\"author\":\"J. Jansen\"}]}]}")
                    .asString();
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

}
