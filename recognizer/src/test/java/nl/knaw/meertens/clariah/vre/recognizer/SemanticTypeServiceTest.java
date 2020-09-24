package nl.knaw.meertens.clariah.vre.recognizer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nl.knaw.meertens.clariah.vre.recognizer.util.FileUtil.getTestFileContent;
import static org.assertj.core.api.Assertions.assertThat;

public class SemanticTypeServiceTest {

  @Test
  public void detectSemanticTypes_shouldFindFoliaToken() throws IOException {
    var folia = getTestFileContent("folia-token.xml");
    var foliaTestPath = Path.of("/tmp/folia-" + UUID.randomUUID() + ".xml");
    FileUtils.writeStringToFile(foliaTestPath.toFile(), folia, UTF_8);

    var semanticTypeService = new SemanticTypeService(new MimetypeService());
    var types = semanticTypeService.detectSemanticTypes("text/folia+xml", foliaTestPath);

    assertThat(types).containsExactly("folia.token");
  }

  @Test
  public void detectSemanticTypes_shouldFindFoliaPos() throws IOException {
    var folia = getTestFileContent("folia-token-and-pos.xml");
    var foliaTestPath = Path.of("/tmp/folia-" + UUID.randomUUID() + ".xml");
    FileUtils.writeStringToFile(foliaTestPath.toFile(), folia, UTF_8);

    var semanticTypeService = new SemanticTypeService(new MimetypeService());
    var types = semanticTypeService.detectSemanticTypes("text/folia+xml", foliaTestPath);

    assertThat(types).containsExactly("folia.token", "folia.pos", "folia.pos.cgn");
  }

}
