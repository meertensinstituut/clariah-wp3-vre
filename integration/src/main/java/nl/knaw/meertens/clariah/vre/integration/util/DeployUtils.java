package nl.knaw.meertens.clariah.vre.integration.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.integration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.deleteInputFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.downloadFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.putInputFile;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.awaitAndGet;
import static org.assertj.core.api.Assertions.assertThat;

public class DeployUtils {

  private static Logger logger = LoggerFactory.getLogger(DeployUtils.class);

  private static ParseContext jsonPath = JsonPath.using(
    Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build()
  );

  public static HttpResponse<String> deploymentWithStatus(
    String workDir,
    String status
  ) {
    logger.info(format("Check that deployment [%s] has status [%s]", workDir, status));
    var httpStatusSuccess = false;
    var deploymentStatusFound = false;
    HttpResponse<String> deploymentStatusResponse = null;
    deploymentStatusResponse = getDeploymentStatus(workDir);
    String body = deploymentStatusResponse.getBody();
    int responseStatus = deploymentStatusResponse.getStatus();
    logger.info(format("Http status was [%s] and response body was [%s]",
      responseStatus, body
    ));
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException("polling interrupted", e);
    }

    Integer[] success = {200, 201, 202};
    if (asList(success).contains(responseStatus)) {
      httpStatusSuccess = true;
    }

    var deploymentStatus = jsonPath.parse(body).read("$.status", String.class);
    if (!isNull(deploymentStatus) && deploymentStatus.contains(status)) {
      deploymentStatusFound = true;
    }

    logger.info(format(
      "httpStatusSuccess is [%s]; deploymentStatusFound is [%s]; deploymentStatus is [%s]",
      httpStatusSuccess, deploymentStatusFound, deploymentStatus
    ));

    assertThat(httpStatusSuccess).isTrue();
    assertThat(deploymentStatusFound).isTrue();
    return deploymentStatusResponse;
  }

  public static boolean deploymentHasStatus(
    String workDir,
    String status
  ) {
    logger.info(format("Check status is [%s] of [%s]", status, workDir));
    var httpStatusSuccess = false;
    var deploymentStatusFound = false;
    HttpResponse<String> deploymentStatusResponse = null;
    deploymentStatusResponse = getDeploymentStatus(workDir);
    var body = deploymentStatusResponse.getBody();
    var responseStatus = deploymentStatusResponse.getStatus();
    logger.info(format("Http status was [%s] and response body was [%s]",
      responseStatus, body
    ));
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      throw new RuntimeException("polling interrupted", e);
    }

    Integer[] success = {200, 201, 202};
    if (asList(success).contains(responseStatus)) {
      httpStatusSuccess = true;
    }
    var deploymentStatus = jsonPath.parse(body).read("$.status", String.class);
    if (!isNull(deploymentStatus) && deploymentStatus.contains(status)) {
      deploymentStatusFound = true;
    }
    return httpStatusSuccess && deploymentStatusFound;
  }

  public static String startTestDeploymentWithInputFileId(Long expectedFilename) throws UnirestException {
    var serviceName = "TEST";
    var serviceConfig = "{\"params\":[{\"name\":\"untokinput\",\"type\":\"file\",\"value\":\"" + expectedFilename +
      "\",\"params\":[{\"name\":\"language\", \"value\":\"eng\"},{\"name\":\"author\", \"value\":\"J. Jansen\"}]}]}";
    return startDeployment(serviceName, serviceConfig);
  }

  public static String startDeployment(String serviceName, String serviceConfig) throws UnirestException {
    var result = Unirest
      .post(Config.SWITCHBOARD_ENDPOINT + "/exec/" + serviceName)
      .header("Content-Type", "application/json; charset=UTF-8")
      .body(serviceConfig)
      .asString();

    assertThat(result.getStatus()).isIn(200, 201, 202);
    String workDir = JsonPath.parse(result.getBody()).read("$.workDir");
    logger.info(format("deployment has workdir [%s]", workDir));
    return workDir;
  }

  public static String resultWhenDeploymentFinished(String workDir) {
    logger.info(format("check deployment [%s] is finished", workDir));
    var statusResponse = awaitAndGet(() -> deploymentWithStatus(workDir, "FINISHED"));
    var outputFilePath = getOutputFilePath(statusResponse, "result.txt");
    logger.info(format("deployment has result file [%s]", outputFilePath));
    return outputFilePath;
  }

  public static String getOutputFilePath(HttpResponse<String> finishedDeployment, String resultFileName) {
    String outputDir = JsonPath.parse(finishedDeployment.getBody()).read("$.outputDir");
    var pathAbsolute = Paths.get(outputDir);
    var pathBase = Paths.get("admin/files/");
    var pathRelative = pathBase.relativize(pathAbsolute);
    var outputPath = Paths.get(pathRelative.toString(), resultFileName).toString();
    logger.info(format("output file path is [%s]", outputPath));
    return outputPath;
  }

  public static boolean filesAreUnlockedAfterEdit(
    String inputFile,
    String testFileContent
  ) {
    return filesAreUnlocked(
      inputFile,
      testFileContent,
      (String content) -> content.contains(testFileContent)
    );
  }

  public static boolean filesAreUnlocked(
    String inputFile,
    String testFileContent
  ) {
    return filesAreUnlocked(
      inputFile,
      testFileContent,
      (String content) -> content.equals(testFileContent)
    );
  }

  private static boolean filesAreUnlocked(
    String inputFile,
    String testFileContent,
    Function<String, Boolean> contentOfFileTester
  ) {
    try {
      var put = false;
      var delete = false;

      var downloadResult = downloadFile(inputFile);
      List expected = newArrayList(200, 202);
      logger.info(format("input file content is: [%s]", downloadResult.getBody()));
      logger.info(format("output file content should contain: [%s]", testFileContent));

      var expectedContent = contentOfFileTester.apply(downloadResult.getBody());
      var expectedStatus = expected.contains(downloadResult.getStatus());
      var get = expectedContent && expectedStatus;

      if (get) {
        var putAfterDeployment = putInputFile(inputFile);
        put = putAfterDeployment.getStatus() == 204;

        var deleteInputFile = deleteInputFile(inputFile);
        delete = deleteInputFile.getStatus() == 204;
      }

      logger.info(format(
        "checks: get status [%b], get content [%b], put [%b], delete [%b]",
        expectedStatus, expectedContent, put, delete
      ));

      return get && put && delete;
    } catch (UnirestException e) {
      throw new RuntimeException("Could not check files are unlocked", e);
    }

  }

  private static HttpResponse<String> getDeploymentStatus(String workDir) {
    try {
      return Unirest
        .get(Config.SWITCHBOARD_ENDPOINT + "/exec/task/" + workDir + "/")
        .header("Content-Type", "application/json; charset=UTF-8")
        .asString();
    } catch (UnirestException e) {
      throw new RuntimeException("Could not get deployment status", e);
    }
  }


}
