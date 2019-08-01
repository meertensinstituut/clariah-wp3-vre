package nl.knaw.meertens.deployment.lib.recipe;

import nl.knaw.meertens.deployment.lib.AbstractDeploymentTest;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import static nl.knaw.meertens.deployment.lib.FileUtil.createFile;
import static nl.knaw.meertens.deployment.lib.FileUtil.createInputFile;
import static nl.knaw.meertens.deployment.lib.FileUtil.getTestFileContent;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.USER_CONF_FILE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpClassCallback.callback;
import static org.mockserver.model.HttpRequest.request;

public class TextstatsTest extends AbstractDeploymentTest {

  @Test
  public void executeShouldSendPostRequest() throws Exception {
    // 1. Arrange:
    var workDir = UUID.randomUUID().toString();

    // create work dir
    var configPath = Paths.get(ROOT_WORK_DIR, workDir, USER_CONF_FILE);
    var testFileContent = getTestFileContent("configTextstats.json");
    createFile(configPath.toString(), testFileContent);
    createInputFile(workDir, "inputTextstats.xml", "inputTextstats.xml");

    // create service object
    var recipe = new Textstats();
    recipe.init(workDir, new Service(), "http://localhost:" + mockPort, null);

    // create service mock
    createTextstatsServiceMock();

    // 2. Act:
    recipe.execute();

    // 3. Assert:
    var outputFilename = "result.json";
    var outputFile = Paths.get(ROOT_WORK_DIR, workDir, OUTPUT_DIR, outputFilename);
    assertThat(outputFile.toFile()).exists();
    assertThat(outputFile.toFile()).hasContent(getTestFileContent("outputTextstats.json"));
  }

  private void createTextstatsServiceMock() {
    getMockServer()
      .when(
        request()
          .withMethod("POST"),
        exactly(1)
      )
      .respond(
        callback()
          .withCallbackClass("nl.knaw.meertens.deployment.lib.recipe.TextstatsTestExpectationResponseCallback")
      );
  }


}
