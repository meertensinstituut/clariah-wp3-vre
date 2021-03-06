package nl.knaw.meertens.clariah.vre.recognizer;

import nl.knaw.meertens.clariah.vre.recognizer.object.ObjectRepository;
import nl.knaw.meertens.clariah.vre.recognizer.object.ObjectSemanticTypeRepository;
import nl.knaw.meertens.clariah.vre.recognizer.object.ObjectsService;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;

import java.io.File;
import java.io.IOException;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_FILES_ROOT;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECT_SEMANTIC_TYPE_TABLE;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECT_TABLE;
import static org.apache.commons.codec.Charsets.UTF_8;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@RunWith(MockitoJUnitRunner.class)
public class ObjectsServiceTest extends AbstractRecognizerTest {

  private ObjectsService objectsService;

  @Before
  public void setup() {
    setupMockServer();
    var mimetypeService = new MimetypeService();

    objectsService = new ObjectsService(
      new SemanticTypeService(mimetypeService),
      new ObjectRepository(mockUrl, "", OBJECT_TABLE, ObjectMapperFactory.getInstance()),
      new ObjectSemanticTypeRepository(mockUrl, "", OBJECT_SEMANTIC_TYPE_TABLE, ObjectMapperFactory.getInstance()),
      mimetypeService
    );
  }

  @Test
  public void testCreateJsonCreatesJson() throws Exception {
    var fits = fitsService.unmarshalFits(testFitsXml);
    var report = new Report();
    report.setFits(fits);
    var expectedPath = "/some/path.txt";
    report.setPath(expectedPath);
    var expectedUser = "some-user";
    report.setUser(expectedUser);
    report.setXml(testFitsXml);

    var result = ObjectMapperFactory.getInstance().writeValueAsString(
      objectsService.createObjectRecordDto(report)
    );

    assertThatJson(result).node("time_created").matches(
      containsString(now().format(ofPattern("yyyy-MM-dd'T'HH"))));
    assertThatJson(result).node("time_changed").matches(
      containsString(now().format(ofPattern("yyyy-MM-dd'T'HH"))));
    assertThatJson(result).node("user_id").isEqualTo(expectedUser);
    assertThatJson(result).node("filepath").isEqualTo(expectedPath);
    assertThatJson(result).node("mimetype").isEqualTo("text/plain");
    assertThatJson(result).node("format").isEqualTo("Plain text");
    assertThatJson(result).node("deleted").isEqualTo(false);
  }

  @Test
  public void testSoftDelete() {
    var expectedId = 8L;
    var annotatedBanana = "john-doe/files/banana.xml";
    startObjectsRegistryMock(
      "objects-not-deleted.json",
      "(filepath='" + annotatedBanana + "') AND (deleted='0')"
    );
    startObjectsPatchRegistryMock("soft-delete-object.json", expectedId);
    Long deletedId = objectsService.softDelete(annotatedBanana);
    assertThat(deletedId).isEqualTo(expectedId);
  }

  private void startObjectsPatchRegistryMock(String patchFile, Long id) {
    var content = getContentOfResource(patchFile);

    mockServer
      .when(request()
          .withMethod("PATCH")
          .withPath("/_table/object/" + id),
        exactly(1)
      )
      .respond(response()
        .withStatusCode(200)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(content)
      );
  }

  private void startObjectsRegistryMock(String objectsFile, String filter) {
    var content = getContentOfResource(objectsFile);

    mockServer
      .when(request()
          .withMethod("GET")
          .withPath("/_table/object")
          .withQueryStringParameter(new Parameter("filter", filter)),
        exactly(1)
      )
      .respond(response()
        .withStatusCode(200)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(content)
      );
  }

  private String getContentOfResource(String objectsFile) {
    var file = new File(getClass().getClassLoader().getResource(objectsFile).getFile());
    String content;
    try {
      content = FileUtils.readFileToString(file, UTF_8);
    } catch (IOException e) {
      throw new RuntimeException();
    }
    return content;
  }


}