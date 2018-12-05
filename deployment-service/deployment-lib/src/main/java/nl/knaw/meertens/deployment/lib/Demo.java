package nl.knaw.meertens.deployment.lib;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.io.FileUtils;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.LocalDateTime.now;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.deployment.lib.SystemConf.INPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.OUTPUT_DIR;
import static nl.knaw.meertens.deployment.lib.SystemConf.WORK_DIR;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Demo recipe scrapes text from an html file
 */
public class Demo implements RecipePlugin {
  private static Logger logger = LoggerFactory.getLogger(Demo.class);

  private static final String COOKIE_SERVICE_URL = "https://tools.digitalmethods.net/beta/deduplicate/";
  private static final String SERVICE_URL = "https://tools.digitalmethods.net/beta/deduplicate/";
  private static final File COOKIE_FILE = Paths.get(WORK_DIR, "cookies/demo-cookie.tmp.txt").toFile();
  private static final File COOKIE_EXPIRE_FILE = Paths.get(WORK_DIR, "cookies/demo-cookie-expire.tmp.txt").toFile();
  private boolean isFinished;

  private String workDir;
  private Service service;

  static {
    try {
      if (COOKIE_FILE.getParentFile().mkdirs()) {
        logger.info(String.format("created cookie parent dirs to [%s]", COOKIE_FILE));
      }
      if (COOKIE_FILE.createNewFile()) {
        logger.info(String.format("created cookie file [%s]", COOKIE_SERVICE_URL));
      }
      if (COOKIE_EXPIRE_FILE.getParentFile().mkdirs()) {
        logger.info(String.format("created cookie expire dirs to [%s]", COOKIE_EXPIRE_FILE));
      }
      if (COOKIE_EXPIRE_FILE.createNewFile()) {
        logger.info(String.format("created cookie expire file [%s]", COOKIE_EXPIRE_FILE));
      }
    } catch (IOException ex) {
      logger.error("could not create cookie files", ex);
    }
  }

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
    Pattern.compile(".*PHPSESSID=(.*?);.*gcosvcauth=(.*?);.*expires=(.*?);.*");

  @Override
  public void init(String workDir, Service service) throws RecipePluginException {
    logger.info(String.format("init [%s][%s]", service.getName(), workDir));
    this.workDir = workDir;
    this.service = service;
  }

  @Override
  public JSONObject execute() throws RecipePluginException {
    logger.info(String.format("execute [%s][%s]", service.getName(), workDir));
    this.runProject(workDir);
    return createRunningStatus();
  }

  @Override
  public JSONObject getStatus() throws RecipePluginException {
    try {
      TimeUnit.SECONDS.sleep(3);
    } catch (InterruptedException e) {
      throw new RecipePluginException("sleep interrupted");
    }
    logger.info(String.format("return status [%b] for [%s][%s]", true, service.getName(), workDir));
    return createFinishedStatus();
  }

  String getResultString(String html) {
    Document doc = Jsoup.parse(html, "UTF-8");
    return doc.getElementsByTag("result").text();
  }

  private JSONObject createRunningStatus() {
    JSONObject json = new JSONObject();
    json.put("key", workDir);
    json.put("status", 202);
    return json;
  }

  private JSONObject createFinishedStatus() {
    JSONObject json = new JSONObject();
    json.put("finished", true);
    json.put("success", true);
    return json;
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
        .body("text=<result>(1) " + inputContent + " </result>(1)")
        .asString();
      logger.info("cookie: " + cookie);
      logger.info("body: " + response.getBody());
      txt = getResultString(response.getBody());
      logger.info("text: " + txt);
    } catch (UnirestException ex) {
      throw new RecipePluginException(String.format("Request to [%s] failed", SERVICE_URL));
    }

    Path outputFile = buildOutputFilePath("result.txt");
    createOutputFolder(outputFile);

    try {
      FileUtils.writeStringToFile(outputFile.toFile(), txt, UTF_8);
      logger.info(String.format("saved result to [%s]", outputFile.toString()));
    } catch (IOException exception) {
      throw new RecipePluginException(String.format("could not write output to file [%s]", outputFile));
    }
  }

  private static void checkCookie() throws RecipePluginException {

    if (isBlank(cookie)) {
      try {
        cookie = FileUtils.readFileToString(COOKIE_FILE);
        logger.info(String.format("read cookie from file: [%s]", cookie));
      } catch (IOException ex) {
        logger.error("could not read cookie file value", ex);
      }
      try {
        logger.info("read cookie expire date from file");
        String fileWithoutWhitespace = FileUtils.readFileToString(COOKIE_EXPIRE_FILE).replaceAll("\\s+", "");
        cookieExpireDate = LocalDateTime.parse(fileWithoutWhitespace);
        logger.info(String.format(
          "read cookie expire date from file: [%s] -> [%s]; now: [%s]",
          fileWithoutWhitespace, cookieExpireDate.toString(), now()
        ));
      } catch (DateTimeParseException | IOException ex) {
        logger.error("could not read expire file value", ex);
      }
    }

    if (isNull(cookieExpireDate) || cookieExpireDate.isAfter(now())) {
      logger.info("get new cookie");
      cookie = getHeaderCookie();
      cookieExpireDate = now().plusSeconds(expireLength.getSeconds());
      try {
        FileUtils.writeStringToFile(COOKIE_FILE, cookie, UTF_8);
        FileUtils.writeStringToFile(COOKIE_EXPIRE_FILE, cookieExpireDate.toString(), UTF_8);
      } catch (IOException ex) {
        logger.error("could not save cookie to file", ex);
      }
    }
  }

  private static String getHeaderCookie() throws RecipePluginException {

    HttpResponse<String> cookieRequest;
    try {
      cookieRequest = Unirest
        .get(COOKIE_SERVICE_URL)
        .asString();
    } catch (UnirestException ex) {
      throw new RecipePluginException("could not get cookie header", ex);
    }

    disableRedirect();

    String setCookieHeaderValue = cookieRequest
      .getHeaders().entrySet().stream()
      .filter((entry) -> {
        logger.info("value 0:" + entry.getValue().toString());
        boolean pass = setCookiePattern.matcher(entry.getValue().toString()).matches();
        logger.info("headers: " + entry.getKey() + ";" + entry.getValue() + "; pass: " + pass);
        return pass;
      }).findFirst()
      .orElseThrow(() -> new RecipePluginException("could not find session cookie header"))
      .getValue().toString();

    enableRedirect();

    logger.info("set cookie header: " + setCookieHeaderValue);

    return createRequestCookie(setCookieHeaderValue);

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

  static String createRequestCookie(String setCookieValue) {
    Matcher matcher = setCookiePattern.matcher(setCookieValue);
    matcher.find();
    String phpSessionId = matcher.group(1);
    String gcosvcauth = matcher.group(2);
    return String.format("PHPSESSID=%s; gcosvcauth=%s", phpSessionId, gcosvcauth);
  }

  private String retrieveInputContent(String projectName) throws RecipePluginException {
    JSONObject userConfig = DeploymentLib.parseUserConfig(projectName);
    JSONArray params = (JSONArray) userConfig.get("params");
    String inputFilename = (String) ((JSONObject) params.get(0)).get("value");
    String inputPath = buildInputPath(projectName, inputFilename);
    try {
      return new String(Files.readAllBytes(Paths.get(inputPath)));
    } catch (IOException e) {
      throw new RecipePluginException(String.format("Could not read input file [%s]", inputPath));
    }
  }

  private String buildInputPath(String projectName, String inputFile) {
    return Paths.get(
      WORK_DIR,
      projectName,
      INPUT_DIR, inputFile
    ).normalize().toString();
  }

  private Path buildOutputFilePath(String resultFilename) {
    return Paths.get(
      WORK_DIR,
      workDir,
      OUTPUT_DIR,
      resultFilename
    ).normalize();
  }

  private void createOutputFolder(Path outputfilePath) {
    File outputFolder = outputfilePath.getParent().normalize().toFile();
    if (!outputFolder.exists()) {
      outputFolder.mkdirs();
      logger.info(String.format("created folder [%s]", outputFolder.toString()));
    }
  }

}
