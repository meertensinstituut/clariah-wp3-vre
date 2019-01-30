package nl.knaw.meertens.clariah.vre.integration;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.util.ObjectsRepositoryService;
import nl.knaw.meertens.clariah.vre.integration.util.Poller;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.fileCanBeDownloaded;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getRandomFilenameWithTime;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.uploadTestFile;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.fileExistsInRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.ObjectUtils.getObjectIdFromRegistry;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.apache.http.auth.AuthScope.ANY_HOST;
import static org.apache.http.auth.AuthScope.ANY_PORT;
import static org.assertj.core.api.Assertions.assertThat;

public class CrudObjectTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
            Config.DB_OBJECTS_DATABASE, Config.DB_OBJECTS_USER, Config.DB_OBJECTS_PASSWORD
    );

    private static final String html = "test.html";
    private long id;

    @Test
    public void testRecognizer_creates_updates_deletes_recordInObjectsRegistry() throws Exception {
        final String expectedFilename = uploadTestFile();
        Poller.awaitAndGet(() -> fileExistsInRegistry(expectedFilename));
        Poller.awaitAndGet(() -> fileCanBeDownloaded(expectedFilename, getTestFileContent()));
        id = Poller.awaitAndGet(() -> getObjectIdFromRegistry(expectedFilename));

        String newHtmlFileName = updateTestFilePath(expectedFilename);

        Poller.awaitAndGet(() -> fileCanBeDownloaded(newHtmlFileName, getTestFileContent()));
        awaitAndGet(() -> fileNameChangedButTypeDidNot(newHtmlFileName));

        updateContentToHtml(newHtmlFileName);

        Poller.awaitAndGet(() -> fileCanBeDownloaded(newHtmlFileName, getTestFileContent(html)));
        awaitAndGet(() -> fileTypeIsHtml(newHtmlFileName));

        deleteFile(newHtmlFileName);

        awaitAndGet(this::fileIsSoftDeletedInRegistry);
    }

    private String updateTestFilePath(String oldFilename) throws IOException {
        String newFileName = getRandomFilenameWithTime().split("\\.")[0] + ".html";

        logger.info(String.format("Rename file [%s] to [%s]", oldFilename, newFileName));
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(ANY_HOST, ANY_PORT),
                new UsernamePasswordCredentials(Config.NEXTCLOUD_ADMIN_NAME, Config.NEXTCLOUD_ADMIN_PASSWORD)
        );
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        HttpUriRequest moveRequest = RequestBuilder
                .create("MOVE")
                .setUri(Config.NEXTCLOUD_ENDPOINT + oldFilename)
                .addHeader(HttpHeaders.DESTINATION, Config.NEXTCLOUD_ENDPOINT + newFileName)
                .build();

        CloseableHttpResponse httpResponse = httpclient.execute(moveRequest);
        int status = httpResponse.getStatusLine().getStatusCode();
        assertThat(status).isEqualTo(201);
        return newFileName;
    }

    private void updateContentToHtml(String newFileName) throws UnirestException {
        logger.info("Add html to html file");
        Unirest.put(Config.NEXTCLOUD_ENDPOINT + newFileName)
                .header("Content-Type", "text/html; charset=utf-8") // set type to html
                .basicAuth(Config.NEXTCLOUD_ADMIN_NAME, Config.NEXTCLOUD_ADMIN_PASSWORD)
                .body(getTestFileContent(html))
                .asString();
    }

    private void fileNameChangedButTypeDidNot(String newHtmlFileName) {
        String query = "select * from object WHERE id=" + id;
        try {
            objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
                while (rs.next()) {
                    assertThat(rs.getString("filepath")).contains(newHtmlFileName);
                    assertThat(rs.getString("format")).isEqualTo("Plain text");
                    assertThat(rs.getString("mimetype")).isEqualTo("text/plain");
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void fileTypeIsHtml(String newHtmlFileName) {
        String queryHtml = "select * from object WHERE id=" + id;
        try {
            objectsRepositoryService.processQuery(queryHtml, (ResultSet rs) -> {
                while (rs.next()) {
                    assertThat(rs.getString("filepath")).contains(newHtmlFileName);
                    assertThat(rs.getString("mimetype")).isEqualTo("text/html");
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteFile(String file) throws UnirestException {
        logger.info(String.format("Delete file [%s]", file));
        HttpResponse<String> response = Unirest.delete(Config.NEXTCLOUD_ENDPOINT + file)
                .basicAuth(Config.NEXTCLOUD_ADMIN_NAME, Config.NEXTCLOUD_ADMIN_PASSWORD)
                .asString();
        assertThat(response.getStatus()).isEqualTo(204);
    }

    private void fileIsSoftDeletedInRegistry() {
        String countAfterDelete = "SELECT count(*) AS exact_count FROM object WHERE id=" + id + " AND deleted=false";
        try {
            objectsRepositoryService.processQuery(countAfterDelete, (ResultSet rs) -> {
                rs.next();
                int recordsCount = rs.getInt("exact_count");
                assertThat(recordsCount).isEqualTo(0);
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
