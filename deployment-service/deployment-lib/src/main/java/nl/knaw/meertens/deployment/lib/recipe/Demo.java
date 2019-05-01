package nl.knaw.meertens.deployment.lib.recipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.deployment.lib.DeploymentLib;
import nl.knaw.meertens.deployment.lib.DeploymentResponse;
import nl.knaw.meertens.deployment.lib.DeploymentStatus;
import nl.knaw.meertens.deployment.lib.RecipePlugin;
import nl.knaw.meertens.deployment.lib.RecipePluginException;
import nl.knaw.meertens.deployment.lib.Service;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonArray;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static nl.knaw.meertens.deployment.lib.DeploymentLib.buildInputPath;
import static nl.knaw.meertens.deployment.lib.DeploymentLib.buildOutputFilePath;
import static nl.knaw.meertens.deployment.lib.DeploymentLib.createOutputFolder;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.FINISHED;
import static nl.knaw.meertens.deployment.lib.DeploymentStatus.RUNNING;
import static nl.knaw.meertens.deployment.lib.SystemConf.INPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.ROOT_WORK_DIR;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Demo recipe duplicates words using service:
 * tools.digitalmethods.net/beta/deduplicate/
 */
public class Demo implements RecipePlugin {
  private static Logger logger = LoggerFactory.getLogger(Demo.class);

  private static final String SERVICE_URL = "https://tools.digitalmethods.net/beta/deduplicate/";
  private static final String RESULT_TAG = "result";
  static final String OUTPUT_FILENAME = "result.txt";

  private String workDir;
  private Service service;
  private DeploymentStatus status;

  /**
   * Cookie is saved for next deployments
   */
  private static String cookie;

  /**
   * Cookies expire and are reset every {expireLength} seconds
   */
  private static LocalDateTime cookieExpireDate;

  /**
   * Expire interval
   */
  private static final Duration expireLength = Duration.ofSeconds(3600);

  private static final Pattern setCookiePattern =
    Pattern.compile(".*PHPSESSID=(.*?);.*gcosvcauth=(.*?);.*");

  @Override
  public void init(String workDir, Service service, String serviceLocation) throws RecipePluginException {
    logger.info(format("init [%s]", workDir));
    this.workDir = workDir;
    this.service = service;
    this.status = DeploymentStatus.CREATED;
  }

  @Override
  public DeploymentResponse execute() throws RecipePluginException {
    logger.info(format("execute [%s][%s]", service.getName(), workDir));
    status = RUNNING;
    this.runProject(workDir);
    return status.toResponse();
  }

  @Override
  public DeploymentResponse getStatus() throws RecipePluginException {
    return status.toResponse();
  }

  private void runProject(String projectName) throws RecipePluginException {
    checkCookie();

    String txt;
    try {
      String inputContent = retrieveInputContent(projectName);
      HttpResponse<String> response = Unirest
        .post(SERVICE_URL)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Cookie", cookie)
        .body("text=<" + RESULT_TAG + ">(1) " + inputContent + " </" + RESULT_TAG + ">(1)")
        .asString();
      txt = getResultString(response.getBody());
    } catch (UnirestException ex) {
      throw new RecipePluginException(format("Request to [%s] failed", SERVICE_URL));
    }

    Path outputFile = buildOutputFilePath(workDir, OUTPUT_FILENAME);
    createOutputFolder(outputFile);

    try {
      writeStringToFile(outputFile.toFile(), txt, UTF_8);
      logger.info(format("saved result to [%s]", outputFile.toString()));
    } catch (IOException exception) {
      throw new RecipePluginException(format("could not write output to file [%s]", outputFile));
    }
    status = FINISHED;
  }

  String getResultString(String html) {
    return Jsoup
      .parse(html, "UTF-8")
      .getElementsByTag(RESULT_TAG)
      .text();
  }

  private static void checkCookie() throws RecipePluginException {
    if (isBlank(cookie) || cookieExpireDate.isAfter(now())) {
      setCookieAndExpireDate();
    }
  }

  /**
   * Sets cookie and cookieExpireDate
   *
   * <p>Creates new `Cookie` request header value
   * from `set-cookie` response header value of service
   */
  private static void setCookieAndExpireDate() throws RecipePluginException {
    logger.info("get new cookie");
    cookie = retrieveHeaderCookie();
    cookieExpireDate = now().plusSeconds(expireLength.getSeconds());
  }

  static String retrieveHeaderCookie() throws RecipePluginException {
    disableRedirect();
    HttpResponse<String> cookieRequest;
    try {
      cookieRequest = Unirest
        .get(SERVICE_URL)
        .asString();
    } catch (UnirestException ex) {
      throw new RecipePluginException("could not get cookie header", ex);
    }
    enableRedirect();

    String setCookie = findSetCookieHeaderValue(cookieRequest);
    return createRequestCookie(setCookie);
  }

  private static void disableRedirect() {
    Unirest.setHttpClient(HttpClients
      .custom()
      .disableRedirectHandling()
      .build());
  }

  private static void enableRedirect() {
    Unirest.setHttpClient(HttpClients
      .custom()
      .build());
  }

  private static String findSetCookieHeaderValue(HttpResponse<String> cookieRequest) throws RecipePluginException {
    return cookieRequest
      .getHeaders()
      .entrySet()
      .stream()
      .filter((entry) -> setCookiePattern
        .matcher(entry.getValue().toString())
        .matches()).findFirst()
      .orElseThrow(() -> new RecipePluginException("could not find session cookie header"))
      .getValue()
      .toString();
  }

  static String createRequestCookie(String setCookieValue) {
    Matcher matcher = setCookiePattern.matcher(setCookieValue);
    matcher.find();
    String phpSessionId = matcher.group(1);
    String gcosvcauth = matcher.group(2);
    return format("PHPSESSID=%s; gcosvcauth=%s", phpSessionId, gcosvcauth);
  }

  private String retrieveInputContent(String projectName) throws RecipePluginException {
    ObjectNode userConfig = DeploymentLib.parseUserConfig(projectName);
    JsonNode params = userConfig.get("params");
    String inputFilename = params.get(0).get("value").asText();
    String inputPath = buildInputPath(projectName, inputFilename);
    try {
      return new String(Files.readAllBytes(Paths.get(inputPath)));
    } catch (IOException e) {
      throw new RecipePluginException(format("Could not read input file [%s]", inputPath));
    }
  }

}
