package nl.knaw.meertens.clariah.vre.integration;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract class containing environment variables
 * and test util methods.
 */
public abstract class AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(UploadingNewFileTest.class);
    private ParseContext jsonPath;

    final static String OWNCLOUD_ENDPOINT = "http://owncloud:80/remote.php/webdav/";
    final static String OWNCLOUD_ADMIN_NAME = System.getenv("OWNCLOUD_ADMIN_NAME");
    final static String OWNCLOUD_ADMIN_PASSWORD = System.getenv("OWNCLOUD_ADMIN_PASSWORD");
    final static String KAFKA_ENDPOINT = "kafka:" + System.getenv("KAFKA_PORT");
    final static String OWNCLOUD_TOPIC_NAME = System.getenv("OWNCLOUD_TOPIC_NAME");
    final static String RECOGNIZER_TOPIC_NAME = System.getenv("RECOGNIZER_TOPIC_NAME");
    final static String DB_OBJECTS_USER = System.getenv("DB_OBJECTS_USER");
    final static String DB_OBJECTS_PASSWORD = System.getenv("DB_OBJECTS_PASSWORD");
    final static String DB_OBJECTS_DATABASE = System.getenv("DB_OBJECTS_DATABASE");
    final static String SWITCHBOARD_ENDPOINT = "http://switchboard:8080/switchboard/rest/";



    @Before
    public void setUp() {
        jsonPath = JsonPath.using(
                Configuration
                        .builder()
                        .options(Option.SUPPRESS_EXCEPTIONS)
                        .build()
        );
    }


    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            logger.info(String.format("Starting test [%s]", description.getMethodName()));
        }
    };

    static String getRandomGroupName() {
        return "vre_integration_group" + UUID.randomUUID();
    }

    /**
     * Upload file with a random filename
     */
    String uploadTestFile() throws UnirestException, IOException, URISyntaxException {
        return uploadTestFile(new String(getTestFileContent()));
    }

    /**
     * Upload file with a random filename
     */
    String uploadTestFile(String content) throws UnirestException {
        String expectedFilename = getRandomFilenameWithTime();

        Unirest.put(OWNCLOUD_ENDPOINT + expectedFilename)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .body(content)
                .asString();
        logger.info("Uploaded file " + expectedFilename);
        return expectedFilename;
    }


    byte[] getTestFileContent() throws IOException, URISyntaxException {
        return IOUtils.toByteArray(this.getClass().getResource("test.txt").toURI());
    }

    String getRandomFilenameWithTime() {
        return "test-" + UUID.randomUUID() + new SimpleDateFormat("-yyyyMMdd_HHmmss")
                .format(Calendar.getInstance().getTime()) + ".txt";
    }

    long getObjectIdFromRegistry(String inputFile) throws SQLException {
        ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
                DB_OBJECTS_DATABASE, DB_OBJECTS_USER, DB_OBJECTS_PASSWORD);
        String query = "select * from object WHERE filepath LIKE '%" + inputFile + "%' LIMIT 1;";
        final long[] id = new long[1];
        objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
            while (rs.next()) {
                id[0] = (long) rs.getInt("id");
            }
            // When zero, no object has been found:
            assertThat(id[0]).isNotZero();
        });
        logger.info(String.format("uploaded file [%s] has object id [%d]", inputFile, id[0]));
        return id[0];
    }

    String startDeploymentWithInputFileId(Long expectedFilename) throws UnirestException {
        HttpResponse<String> result = Unirest
                .post(SWITCHBOARD_ENDPOINT + "exec/TEST")
                .header("Content-Type", "application/json; charset=UTF-8")
                .body("{\"params\":[{\"name\":\"untokinput\",\"type\":\"file\",\"value\":\"" + expectedFilename + "\",\"params\":[{\"language\":\"eng\",\"author\":\"J. Jansen\"}]}]}")
                .asString();

        assertThat(result.getStatus()).isIn(200, 202);
        return JsonPath.parse(result.getBody()).read("$.workDir");
    }

    HttpResponse<String> checkDeploymentStatus(
            String workDir,
            int pollPeriod,
            String expectedStatus
    ) throws UnirestException, InterruptedException {
        logger.info(String.format("Check status is [%s] of [%s]", expectedStatus, workDir));
        int waited = 0;
        boolean httpStatusSuccess = false;
        boolean deploymentStatusFound = false;
        HttpResponse<String> deploymentStatusResponse = null;
        while (waited <= pollPeriod) {
            deploymentStatusResponse = getDeploymentStatus(workDir);
            String body = deploymentStatusResponse.getBody();
            String deploymentStatus = jsonPath.parse(body).read("$.status", String.class);
            int responseStatus = deploymentStatusResponse.getStatus();
            logger.info(String.format("Http status was [%s] and response body was [%s]",
                    responseStatus, body
            ));
            TimeUnit.SECONDS.sleep(1);
            waited++;

            if (responseStatus == 200 || responseStatus == 202) {
                httpStatusSuccess = true;
            }
            if (!isNull(deploymentStatus) && deploymentStatus.contains(expectedStatus)) {
                deploymentStatusFound = true;
            }
            if (httpStatusSuccess && deploymentStatusFound) {
                break;
            }
        }
        assertThat(httpStatusSuccess).isTrue();
        assertThat(deploymentStatusFound).isTrue();
        return deploymentStatusResponse;
    }


    private HttpResponse<String> getDeploymentStatus(String workDir) throws UnirestException {
        return Unirest
                .get(SWITCHBOARD_ENDPOINT + "exec/task/" + workDir + "/")
                .header("Content-Type", "application/json; charset=UTF-8")
                .asString();
    }


}
