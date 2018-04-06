package nl.knaw.meertens.clariah.vre.integration;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

/**
 * Abstract class containing environment variables
 * and test util methods.
 */
public abstract class AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(UploadingNewFileTest.class);

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


}
