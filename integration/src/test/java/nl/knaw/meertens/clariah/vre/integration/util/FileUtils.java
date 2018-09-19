package nl.knaw.meertens.clariah.vre.integration.util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.AbstractIntegrationTest;
import nl.knaw.meertens.clariah.vre.integration.Config;
import org.apache.commons.io.IOUtils;
import org.apache.maven.surefire.shade.org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.integration.Config.OWNCLOUD_ADMIN_NAME;
import static nl.knaw.meertens.clariah.vre.integration.Config.OWNCLOUD_ADMIN_PASSWORD;
import static nl.knaw.meertens.clariah.vre.integration.Config.OWNCLOUD_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;

public class FileUtils {

    private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static void fileCanBeDownloaded(String inputFile, String someContent) {
        logger.info(String.format("check file [%s] can be downloaded", inputFile));
        HttpResponse<String> downloadResult = downloadFile(inputFile);
        assertThat(downloadResult.getBody()).isEqualTo(someContent);
        assertThat(downloadResult.getStatus()).isEqualTo(200);
    }

    public static HttpResponse<String> downloadFile(String inputFile) {
        try {
            return Unirest
                    .get(OWNCLOUD_ENDPOINT + inputFile)
                    .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                    .asString();
        } catch (UnirestException e) {
            throw new RuntimeException("Could not download file " + inputFile, e);
        }
    }

    public static void fileIsLocked(String inputFile) {
        try {
            logger.info(String.format("check file [%s] is locked", inputFile));
            HttpResponse<String> putAfterDeployment;
            putAfterDeployment = putInputFile(inputFile);

            // http 423 is 'locked'
            assertThat(putAfterDeployment.getStatus()).isIn(403, 423, 500);

            // http 423 is 'locked'
            HttpResponse<String> deleteInputFile = deleteInputFile(inputFile);
            assertThat(deleteInputFile.getStatus()).isIn(403, 423);
        } catch (UnirestException e) {
            throw new RuntimeException("could not check file is locked", e);
        }
    }

    public static HttpResponse<String> putInputFile(String expectedFilename) throws UnirestException {
        return Unirest
                .put(OWNCLOUD_ENDPOINT + expectedFilename)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .body("new content " + RandomStringUtils.random(8))
                .asString();
    }

    public static HttpResponse<String> deleteInputFile(String expectedFilename) throws UnirestException {
        return Unirest
                .delete(OWNCLOUD_ENDPOINT + expectedFilename)
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .asString();
    }

    public static void checkNewFileCanBeAdded(String newInputFile) {
        logger.info("check that a new file is added");
        long newInputFileId = 0;
        newInputFileId = ObjectUtils.getObjectIdFromRegistry(newInputFile);
        assertThat(newInputFileId).isNotEqualTo(0L);
    }

    /**
     * Upload file with a random filename
     */
    public static String uploadTestFile() throws UnirestException, IOException, URISyntaxException {
        return uploadTestFile(getTestFileContent());
    }

    /**
     * Upload file with a random filename
     */
    public static String uploadTestFile(String content) throws UnirestException {
        String expectedFilename = getRandomFilenameWithTime();

        Unirest.put(Config.OWNCLOUD_ENDPOINT + expectedFilename)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .basicAuth(Config.OWNCLOUD_ADMIN_NAME, Config.OWNCLOUD_ADMIN_PASSWORD)
                .body(content)
                .asString();
        logger.info("Uploaded file " + expectedFilename);
        return expectedFilename;
    }

    public static String getTestFileContent() {
        String defaultTestFileName = "test.txt";
        return getTestFileContent(defaultTestFileName);
    }

    public static String getTestFileContent(String defaultTestFileName) {
        try {
            return IOUtils.toString(AbstractIntegrationTest.class.getResource(defaultTestFileName).toURI(), UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeIOException("Could not get test file content", e);
        }
    }

    public static String getRandomFilenameWithTime() {
        return "test-" + UUID.randomUUID() + new SimpleDateFormat("-yyyyMMdd_HHmmss")
                .format(Calendar.getInstance().getTime()) + ".txt";
    }

}
