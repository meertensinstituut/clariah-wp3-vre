package nl.knaw.meertens.deployment.lib.recipe;

import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static nl.knaw.meertens.deployment.lib.FileUtil.getTestFileContent;
import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;

public class TextstatsTestExpectationResponseCallback implements ExpectationResponseCallback {

  private static Logger logger = LoggerFactory.getLogger(TextstatsTest.class);

  @Override
  public HttpResponse handle(HttpRequest httpRequest) {
    var body = httpRequest.getBodyAsString();
    logger.info(format("httpRequest body: [%s]", body));

    var containsLayer = body.contains("name=\"layer\"") && body.contains("original");
    var containsFile = body.contains("name=\"file\"") && body.contains("<TEI xmlns:vg=\"http://www.vangoghletters" +
      ".org/ns/\" xmlns=\"http://www.tei-c.org/ns/1.0\">");

    if (containsLayer && containsFile) {
      logger.info("found param layer with value original in request");
      return response()
        .withStatusCode(200)
        .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
        .withBody(getTestFileContent("outputTextstats.json"));

    } else {
      logger.error("found layer and/or file param in request");
      return notFoundResponse();
    }
  }
}

