package nl.knaw.meertens.clariah.vre.recognizer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_FILES_ROOT;
import static nl.knaw.meertens.clariah.vre.recognizer.util.FileUtil.getTestFileContent;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MimetypeServiceTest {

  @Test
  public void getMimetype_canDetectFolia() throws IOException {
    var service = new MimetypeService();
    var fitsFoliaReport = getTestFileContent("fits-folia-report.xml");
    var folia = getTestFileContent("folia.xml");
    var objectPath = "/tmp/xml-" + UUID.randomUUID() + ".xml";
    var testPath = Path.of(FITS_FILES_ROOT, objectPath);
    FileUtils.writeStringToFile(testPath.toFile(), folia, UTF_8);

    var result = service.getMimetype(fitsFoliaReport, objectPath);

    assertThat(result).isEqualTo("text/folia+xml");
  }

  @Test
  public void getMimetype_canDetectXmlOtherThanFolia() throws IOException {
    var service = new MimetypeService();
    var fitsReport = getTestFileContent("fits-tei-report.xml");
    var fileContent = getTestFileContent("tei.xml");
    var objectPath = "/tmp/xml-" + UUID.randomUUID() + ".xml";
    var testPath = Path.of(FITS_FILES_ROOT, objectPath);
    FileUtils.writeStringToFile(testPath.toFile(), fileContent, UTF_8);

    var result = service.getMimetype(fitsReport, objectPath);

    assertThat(result).isEqualTo("text/xml");
  }

  @Test
  public void getMimetype_canDetectText() throws IOException {
    var service = new MimetypeService();
    var fitsTextReport = getTestFileContent("fits-text-report.xml");
    var text = getTestFileContent("text.txt");
    var objectPath = "/tmp/xml-" + UUID.randomUUID() + ".xml";
    var textTestPath = Path.of(FITS_FILES_ROOT, objectPath);
    FileUtils.writeStringToFile(textTestPath.toFile(), text, UTF_8);

    var result = service.getMimetype(fitsTextReport, objectPath);

    assertThat(result).isEqualTo("text/plain");
  }


  @Test
  public void getMimetype_canDetectHtml() throws IOException {
    var service = new MimetypeService();
    var fitsTextReport = getTestFileContent("fits-html-report-with-conflict.xml");
    var text = getTestFileContent("test.html");
    var objectPath = "/tmp/xml-" + UUID.randomUUID() + ".xml";
    var testPath = Path.of(FITS_FILES_ROOT, objectPath);
    FileUtils.writeStringToFile(testPath.toFile(), text, UTF_8);

    var result = service.getMimetype(fitsTextReport, objectPath);

    assertThat(result).isEqualTo("text/html");
  }

}
