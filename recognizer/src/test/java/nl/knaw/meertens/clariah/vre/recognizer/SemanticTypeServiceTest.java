package nl.knaw.meertens.clariah.vre.recognizer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.recognizer.util.FileUtil.getTestFileContent;

public class SemanticTypeServiceTest {

  @Test
  public void detectSemanticTypes_shouldFindFoliaToken() throws IOException {
    var folia = getTestFileContent("folia-tokenized.xml");
    var foliaTestPath = Path.of("/tmp/folia-" + UUID.randomUUID() + ".xml");
    FileUtils.writeStringToFile(foliaTestPath.toFile(), folia, UTF_8);

    var semanticTypeService = new SemanticTypeService(new MimetypeService());
    List<String> types = semanticTypeService.detectSemanticTypes("text/folia+xml", foliaTestPath);


  }

}
