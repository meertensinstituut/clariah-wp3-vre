package nl.knaw.meertens.clariah.vre.recognizer.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.recognizer.AbstractRecognizerTest;
import nl.knaw.meertens.clariah.vre.recognizer.ObjectMapperFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockserver.matchers.Times;
import org.mockserver.model.Header;
import org.mockserver.verify.VerificationTimes;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECT_SEMANTIC_TYPE_TABLE;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

@RunWith(MockitoJUnitRunner.class)
public class ObjectSemanticTypeRepositoryTest extends AbstractRecognizerTest {

  private ObjectSemanticTypeRepository repository;
  private static ObjectMapper mapper = ObjectMapperFactory.getInstance();

  @Before
  public void setUp() {
    setupMockServer();

    repository = new ObjectSemanticTypeRepository(
      mockUrl,
      "",
      OBJECT_SEMANTIC_TYPE_TABLE,
      mapper
    );
  }

  @After
  public void tearDown() {
    mockServer.reset();
  }

  @Test
  public void createSemanticTypes_shouldPost() {
    startPostObjectSemanticTypeMock("{\"resource\" : [[\"txt.ultimate\",\"txt.v2.0\",\"txt.waterproof\"]]}", 3L);

    List<String> semanticTypes = newArrayList("txt.ultimate", "txt.v2.0", "txt.waterproof");
    repository.createSemanticTypes(3L, semanticTypes);

    mockServer.verify(request()
      .withMethod("POST")
      .withPath("/_table/object_semantic_type"),
      exactly(1)
    );
  }

  @Test
    public void createSemanticTypes_shouldNotPost_whenEmpty() {
    startPostObjectSemanticTypeMock("{\"resource\" : []]}", 3L);

    List<String> semanticTypes = new ArrayList<>();
    repository.createSemanticTypes(3L, semanticTypes);

    mockServer.verify(request()
      .withMethod("POST")
      .withPath("/_table/object_semantic_type"),
      VerificationTimes.exactly(0)
    );
  }

  @Test
  public void deleteSemanticTypes_shouldDelete() {
    startDeleteObjectSemanticTypeMock(3L);

    repository.deleteSemanticTypes(3L);

    mockServer.verify(request()
        .withMethod("DELETE")
        .withPath("/_table/object_semantic_type")
        .withQueryStringParameter("filter", "(object_id=3)"),
      exactly(1)
    );
  }

  private void startPostObjectSemanticTypeMock(String content, Long id) {
    mockServer
      .when(request()
          .withMethod("POST")
          .withPath("/_table/object_semantic_type"),
        Times.exactly(1)
      )
      .respond(response()
        .withStatusCode(200)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(content)
      );
  }

  private void startDeleteObjectSemanticTypeMock(Long id) {
    mockServer
      .when(request()
          .withMethod("DELETE")
          .withPath("/_table/object_semantic_type")
          .withQueryStringParameter("filter", "(object_id=" + id + ")"),
        Times.exactly(1)
      )
      .respond(response()
        .withStatusCode(200)
      );
  }

}
