package nl.knaw.meertens.clariah.vre.recognizer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.recognizer.util.FileUtil.getTestFileContent;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MimetypeServiceTest {

  @Test
  public void getMimetype_canDetectFolia() throws IOException {
    var service = new MimetypeService();
    var fitsFoliaReport = getTestFileContent("fits-folia-report.xml");
    var folia = getTestFileContent("folia.xml");
    var foliaTestPath = Path.of("/tmp/folia-" + UUID.randomUUID() + ".xml");
    FileUtils.writeStringToFile(foliaTestPath.toFile(), folia, UTF_8);

    var result = service.getMimetype(fitsFoliaReport, foliaTestPath);

    assertThat(result).isEqualTo("text/folia+xml");
  }

  @Test
  public void getMimetype_canDetectXmlOtherThanFolia() throws IOException {
    var service = new MimetypeService();
    var fitsReport = getTestFileContent("fits-tei-report.xml");
    var fileContent = getTestFileContent("tei.xml");
    var testPath = Path.of("/tmp/xml-" + UUID.randomUUID() + ".xml");
    FileUtils.writeStringToFile(testPath.toFile(), fileContent, UTF_8);

    var result = service.getMimetype(fitsReport, testPath);

    assertThat(result).isEqualTo("text/xml");
  }

  @Test
  public void getMimetype_canDetectText() throws IOException {
    var service = new MimetypeService();
    var fitsTextReport = getTestFileContent("fits-text-report.xml");
    var text = getTestFileContent("text.txt");
    var textTestPath = Path.of("/tmp/folia-" + UUID.randomUUID() + ".xml");
    FileUtils.writeStringToFile(textTestPath.toFile(), text, UTF_8);

    var result = service.getMimetype(fitsTextReport, textTestPath);

    assertThat(result).isEqualTo("text/plain");
  }


  @Test
  public void getMimetype_canDetectHtml() throws IOException {
    var service = new MimetypeService();
    var fitsTextReport = getTestFileContent("fits-html-report-with-conflict.xml");
    var text = getTestFileContent("test.html");
    var textTestPath = Path.of("/tmp/folia-" + UUID.randomUUID() + ".html");
    FileUtils.writeStringToFile(textTestPath.toFile(), text, UTF_8);

    var result = service.getMimetype(fitsTextReport, textTestPath);

    assertThat(result).isEqualTo("text/html");
  }

}
