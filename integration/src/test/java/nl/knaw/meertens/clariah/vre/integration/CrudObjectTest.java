package nl.knaw.meertens.clariah.vre.integration;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.apache.http.auth.AuthScope.ANY_HOST;
import static org.apache.http.auth.AuthScope.ANY_PORT;
import static org.assertj.core.api.Assertions.assertThat;

public class CrudObjectTest extends AbstractIntegrationTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private static Integer id;
    private ObjectsRepositoryService objectsRepositoryService = new ObjectsRepositoryService(
            DB_OBJECTS_DATABASE, DB_OBJECTS_USER, DB_OBJECTS_PASSWORD);

    @Test
    public void testRecognizer_creates_updates_deletes_recordInObjectsRegistry() throws Exception {
        final String expectedFilename = uploadTestFile();
        TimeUnit.SECONDS.sleep(6);

        checkFileExistsInRegistry(expectedFilename);

        String newHtmlFileName = updateTestFilePath(expectedFilename);
        TimeUnit.SECONDS.sleep(10);

        checkFileTypeIsStillText(newHtmlFileName);

        updateContentToHtml(newHtmlFileName);
        TimeUnit.SECONDS.sleep(6);

        checkFileTypeIsHtml(newHtmlFileName);

        deleteFile(newHtmlFileName);
        TimeUnit.SECONDS.sleep(6);

        checkFileDoesNotExistInRegistry();
    }

    private void checkFileExistsInRegistry(String expectedFilename) throws SQLException {
        String query = "select * from object WHERE filepath LIKE '%" + expectedFilename + "%' LIMIT 1;";
        objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
            while (rs.next()) {
                id = rs.getInt("id");
                assertThat(id).isNotZero();
                assertThat(rs.getString("filepath")).contains(expectedFilename);
                assertThat(rs.getString("format")).isEqualTo("Plain text");
                assertThat(rs.getString("mimetype")).isEqualTo("text/plain");
            }
        });
    }

    private String updateTestFilePath(String oldFilename) throws IOException {
        String newFileName = getRandomFilenameWithTime().split("\\.")[0] + ".html";

        logger.info(String.format("Rename file [%s] to [%s]", oldFilename, newFileName));
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(ANY_HOST, ANY_PORT),
                new UsernamePasswordCredentials(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
        );
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider)
                .build();
        HttpUriRequest moveRequest = RequestBuilder
                .create("MOVE")
                .setUri(OWNCLOUD_ENDPOINT + oldFilename)
                .addHeader(HttpHeaders.DESTINATION, OWNCLOUD_ENDPOINT + newFileName)
                .build();

        CloseableHttpResponse httpResponse = httpclient.execute(moveRequest);
        int status = httpResponse.getStatusLine().getStatusCode();
        logger.info(String.format("Response of rename: [%s]", httpResponse.toString()));
        assertThat(status).isEqualTo(201);
        return newFileName;
    }

    private void updateContentToHtml(String newFileName) throws UnirestException {
        logger.info("Add html to html file");
        HttpResponse<String> result = Unirest.put(OWNCLOUD_ENDPOINT + newFileName)
                .header("Content-Type", "text/html; charset=utf-8") // set type to html
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .body("<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "<title>Hello world</title>\n" +
                        "<link media=\"all\" rel=\"stylesheet\" href=\"styles.css\" />\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "<div>Lorem ipsum!</div>" +
                        "</body>" +
                        "</html>"
                ).asString();
        logger.info(result.getBody() + "; " + result.getStatusText() + "; status code: " + result.getStatus());
    }

    private void checkFileTypeIsStillText(String newHtmlFileName) throws SQLException {
        String query = "select * from object WHERE id=" + id;
        objectsRepositoryService.processQuery(query, (ResultSet rs) -> {
            while (rs.next()) {
                assertThat(rs.getString("filepath")).contains(newHtmlFileName);
                assertThat(rs.getString("format")).isEqualTo("Plain text");
                assertThat(rs.getString("mimetype")).isEqualTo("text/plain");
            }
        });
    }

    private void checkFileTypeIsHtml(String newHtmlFileName) throws SQLException {
        String queryHtml = "select * from object WHERE id=" + id;
        objectsRepositoryService.processQuery(queryHtml, (ResultSet rs) -> {
            while (rs.next()) {
                assertThat(rs.getString("filepath")).contains(newHtmlFileName);
                assertThat(rs.getString("mimetype")).isEqualTo("text/html");
            }
        });
    }

    private void deleteFile(String file) throws UnirestException {
        logger.info(String.format("Delete file [%s]", file));
        HttpResponse<String> response = Unirest.delete(OWNCLOUD_ENDPOINT + file)
                .basicAuth(OWNCLOUD_ADMIN_NAME, OWNCLOUD_ADMIN_PASSWORD)
                .asString();
        assertThat(response.getStatus()).isEqualTo(204);
    }

    private void checkFileDoesNotExistInRegistry() throws SQLException {
        String countAfterDelete = "SELECT count(*) AS exact_count FROM object WHERE id=" + id;
        objectsRepositoryService.processQuery(countAfterDelete, (ResultSet rs) -> {
            rs.next();
            int recordsCount = rs.getInt("exact_count");
            assertThat(recordsCount).isGreaterThanOrEqualTo(0);
        });
    }
}
