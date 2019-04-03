package nl.knaw.meertens.clariah.vre.integration.util;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.App;
import org.apache.commons.io.IOUtils;
import org.apache.maven.surefire.shade.org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.integration.Config.NEXTCLOUD_ADMIN_NAME;
import static nl.knaw.meertens.clariah.vre.integration.Config.NEXTCLOUD_ADMIN_PASSWORD;
import static nl.knaw.meertens.clariah.vre.integration.Config.NEXTCLOUD_ENDPOINT;

public class FileUtils {

  private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

  public static boolean fileHasContent(String inputFile, String someContent) {
    logger.info(format("check file [%s] can be downloaded", inputFile));
    HttpResponse<String> downloadResult = downloadFile(inputFile);
    return downloadResult.getStatus() == 200
      && downloadResult.getBody().equals(someContent);
  }

  public static HttpResponse<String> downloadFile(String inputFile) {
    try {
      return Unirest
        .get(NEXTCLOUD_ENDPOINT + inputFile)
        .basicAuth(NEXTCLOUD_ADMIN_NAME, NEXTCLOUD_ADMIN_PASSWORD)
        .asString();
    } catch (UnirestException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean fileIsLocked(String inputFile) {
    try {
      logger.info(format("check file [%s] is locked", inputFile));
      HttpResponse<String> putAfterDeployment;
      putAfterDeployment = putInputFile(inputFile);

      List lockedStatus = newArrayList(403, 423, 500);
      List deleteStatus = newArrayList(403, 423);

      HttpResponse<String> deleteInputFile = deleteInputFile(inputFile);
      return lockedStatus.contains(putAfterDeployment.getStatus())
        && deleteStatus.contains(deleteInputFile.getStatus());
    } catch (UnirestException e) {
      throw new RuntimeException(e);
    }
  }

  public static HttpResponse<String> putInputFile(String expectedFilename) throws UnirestException {
    String body = "new content " + RandomStringUtils.random(8);
    return putInputFile(expectedFilename, body);
  }

  public static HttpResponse<String> putInputFile(String expectedFilename, String body) throws UnirestException {
    return Unirest
      .put(NEXTCLOUD_ENDPOINT + expectedFilename)
      .header("Content-Type", "text/plain; charset=UTF-8")
      .basicAuth(NEXTCLOUD_ADMIN_NAME, NEXTCLOUD_ADMIN_PASSWORD)
      .body(body)
      .asString();
  }

  public static HttpResponse<String> deleteInputFile(String expectedFilename) throws UnirestException {
    return Unirest
      .delete(NEXTCLOUD_ENDPOINT + expectedFilename)
      .basicAuth(NEXTCLOUD_ADMIN_NAME, NEXTCLOUD_ADMIN_PASSWORD)
      .asString();
  }

  public static boolean newObjectIsAdded(String newInputFile) {
    logger.info("check that a new file is added");
    return 0L != ObjectUtils.getObjectIdFromRegistry(newInputFile);
  }

  /**
   * Upload file with a random filename
   */
  public static String uploadTestFile() throws UnirestException {
    return uploadTestFile(getTestFileContent());
  }

  /**
   * Upload file with a random filename
   */
  public static String uploadTestFile(String content) throws UnirestException {
    String expectedFilename = getRandomFilenameWithTime();
    return uploadTestFile(expectedFilename, content);
  }

  public static String uploadTestFile(String path, String content) throws UnirestException {
    Unirest.put(NEXTCLOUD_ENDPOINT + path)
           .header("Content-Type", "text/plain; charset=UTF-8")
           .basicAuth(NEXTCLOUD_ADMIN_NAME, NEXTCLOUD_ADMIN_PASSWORD)
           .body(content)
           .asString();
    logger.info("Uploaded file " + path);
    return path;
  }

  public static String getTestFileContent() {
    String defaultTestFileName = "test.txt";
    return getTestFileContent(defaultTestFileName);
  }

  public static String getTestFileContent(String defaultTestFileName) {
    try {
      return IOUtils.toString(App.class.getResource(defaultTestFileName).toURI(), UTF_8);
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(format("Could not get content of [%s]", defaultTestFileName), e);
    }
  }

  public static String getRandomFilenameWithTime() {
    return "test-" + UUID.randomUUID() + new SimpleDateFormat("-yyyyMMdd_HHmmss")
      .format(Calendar.getInstance().getTime()) + ".txt";
  }

  /**
   * Wait for nextcouds occ cronjob to scan all files
   * Should happen every 2-3 seconds.
   * (see nextcloud/docker-scan-files.sh)
   */
  public static void awaitOcc() {
    try {
      TimeUnit.SECONDS.sleep(4);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
