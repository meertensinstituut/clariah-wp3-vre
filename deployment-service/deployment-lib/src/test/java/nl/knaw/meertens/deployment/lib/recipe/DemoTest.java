package nl.knaw.meertens.deployment.lib.recipe;

import nl.knaw.meertens.deployment.lib.FileUtil;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

import static nl.knaw.meertens.deployment.lib.FileUtil.createConfigFile;
import static nl.knaw.meertens.deployment.lib.FileUtil.createFile;
import static nl.knaw.meertens.deployment.lib.FileUtil.createInputFile;
import static nl.knaw.meertens.deployment.lib.FileUtil.getTestFileContent;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.USER_CONF_FILE;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DemoTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  public void getResultString_multiline() {
    Demo demo = new Demo();
    String response = "<!DOCTYPE HTML>\n" +
      "<html>\n" +
      "    <head>\n" +
      "        <meta charset=\"UTF-8\">\n" +
      "        <META NAME=\"ROBOTS\" CONTENT=\"NOINDEX, NOFOLLOW\">\n" +
      "        <title>Deduplicate</title>\n" +
      "        <link rel=\"icon\" type=\"image/x-icon\" href=\"/img/favicon.ico\">\n" +
      "        <link rel='stylesheet' type='text/css' href='/style/toolstyle.css'>\n" +
      "        <script type='text/javascript' src='/script/jquery.js'></script>\n" +
      "        <script type='text/javascript' src='/script/dmi.js'></script>\n" +
      "        <script type='text/javascript' src='/script/new_elements.js'></script>\n" +
      "        <script type='text/javascript' src='/script/ajax.js'></script>\n" +
      "        <script type='text/javascript' src='/script/utilities.js'></script>\n" +
      "        <!-- customhead -->\n" +
      "    </head>\n" +
      "<body>\n" +
      "    <div id='container'>\n" +
      "        <div id='head'>\n" +
      "            <div id='menu'></div>\n" +
      "            <div id='line'>Deduplicate</div>\n" +
      "        </div>\n" +
      "        <div id='main'>\n" +
      "<div id='column_left'>\n" +
      "    <fieldset id='input'>\n" +
      "        <legend class='heading'>Input</legend>\n" +
      "        <form name='' action='' method='POST'>Input tag cloud in the format tag1 (13) tag2 (2) .." +
      ".<br/><textarea rows=\"10\" cols=\"60\" name=\"text\" class=\"\"><result>(1) test(10) </result>(1)" +
      "</textarea>\n" +
      "<br /><br /><input type='submit' class='' value='Deduplicate'></form>\n" +
      "    </fieldset>\n" +
      "</div>\n" +
      "<div id='column_right'>\n" +
      "    <fieldset id='howto'>\n" +
      "        <legend class='heading'>Deduplicate, an Introduction</legend>\n" +
      "        <span class='text'>Replicates the tags in a tag cloud by their value<br/><br/>Insert a tag cloud, e.g." +
      " war (5) peace (6) and the tool will write ouput 'war' five times and 'peace' six times.<br/>\n" +
      "<br/>\n" +
      "Can be used to input preformatted tag clouds into services like <a href='http://wordle.net'>wordle</a>" +
      ".<br/></span>\n" +
      "    </fieldset>\n" +
      "</div>\n" +
      "<div style='clear:both'></div>\n" +
      "</div><result>  test  test  test  test  test  test  test  test  test  test  </result> \t</div>\n" +
      "</div>\n" +
      "</body>\n" +
      "</html>";
    String result = demo.getResultString(response);

    assertThat(result).isEqualToIgnoringWhitespace("  test  test  test  test  test  test  test  test  test  test  ");
  }

  @Test
  public void testCreateRequestCookie() {
    String expectedCookie =
      "PHPSESSID=3b26uu7dki19j3fqv15ihv1bdf; " +
        "gcosvcauth=a00b6b173c5be71f1aa37bd2cf9d5ac642605b17bf826bd0de2a3a0bef4df3ac";

    String setCookieValue = "[PHPSESSID=3b26uu7dki19j3fqv15ihv1bdf; " +
      "path=/, gcosvcauth=a00b6b173c5be71f1aa37bd2cf9d5ac642605b17bf826bd0de2a3a0bef4df3ac; " +
      "expires=Wed, 05-Dec-2018 12:48:12 GMT; Max-Age=3600; path=/]";

    String headerCookie = Demo.createRequestCookie(setCookieValue);

    assertThat(headerCookie).isEqualTo(expectedCookie);
  }

  @Test
  public void testRetrieveHeaderCookie() throws RecipePluginException {
    for (int i = 0; i < 10; i++) {
      String result = Demo.retrieveHeaderCookie();
      assertThat(result).matches(Pattern.compile("PHPSESSID=.+; gcosvcauth=.+"));
    }
  }

  @Test
  public void execute_shouldExecute() throws RecipePluginException, IOException {
    // create work dir with a dash to test it is removed when requesting ucto:
    String workDir = "test-" + UUID.randomUUID();
    FileUtil.createWorkDir(workDir);
    logger.info(String.format("create workdir [%s]", workDir));

    createConfigFile(workDir, "configDemo.json");

    createInputFile(workDir, "inputDemo.txt", "inputDemo.txt");

    // instantiate recipe:
    Demo demo = new Demo();
    final String serviceSemantics = FileUtil.getTestFileContent("demo.cmdi");
    Service service = new Service("0", "DEMO", "DEMO", serviceSemantics, "");
    demo.init(workDir, service);
    demo.execute();

    // assert output file exists:
    Path outputFile = Paths.get(ROOT_WORK_DIR, workDir, OUTPUT_DIR, Demo.OUTPUT_FILENAME);
    logger.info("output path expected: " + outputFile);
    boolean outputExists = outputFile.toFile().exists();
    assertThat(outputExists).isTrue();
    assertThat(FileUtils.readFileToString(outputFile.toFile()))
      .isEqualToIgnoringWhitespace(getTestFileContent("outputDemo.txt"));

  }

}
