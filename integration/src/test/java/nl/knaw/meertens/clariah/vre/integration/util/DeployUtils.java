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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.deleteInputFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.downloadFile;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.getTestFileContent;
import static nl.knaw.meertens.clariah.vre.integration.util.FileUtils.putInputFile;
import static nl.knaw.meertens.clariah.vre.integration.util.Poller.pollAndAssert;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Ignore; @Ignore public class DeployUtils {

    private static Logger logger = LoggerFactory.getLogger(DeployUtils.class);

    private static ParseContext jsonPath = JsonPath.using(
            Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build()
    );

    public static HttpResponse<String> deploymentHasStatus(
            String workDir,
            String status
    ) {
        logger.info(String.format("Check status is [%s] of [%s]", status, workDir));
        boolean httpStatusSuccess = false;
        boolean deploymentStatusFound = false;
        HttpResponse<String> deploymentStatusResponse = null;
        deploymentStatusResponse = getDeploymentStatus(workDir);
        String body = deploymentStatusResponse.getBody();
        String deploymentStatus = jsonPath.parse(body).read("$.status", String.class);
        int responseStatus = deploymentStatusResponse.getStatus();
        logger.info(String.format("Http status was [%s] and response body was [%s]",
                responseStatus, body
        ));
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            throw new RuntimeException("polling interrupted", e);
        }

        Integer[] success = {200, 201, 202};
        if (Arrays.asList(success).contains(responseStatus)) {
            httpStatusSuccess = true;
        }
        if (!isNull(deploymentStatus) && deploymentStatus.contains(status)) {
            deploymentStatusFound = true;
        }
        assertThat(httpStatusSuccess).isTrue();
        assertThat(deploymentStatusFound).isTrue();
        return deploymentStatusResponse;
    }

    public static String startDeploymentWithInputFileId(Long expectedFilename) throws UnirestException {
        HttpResponse<String> result = Unirest
                .post(Config.SWITCHBOARD_ENDPOINT + "/exec/TEST")
                .header("Content-Type", "application/json; charset=UTF-8")
                .body("{\"params\":[{\"name\":\"untokinput\",\"type\":\"file\",\"value\":\"" + expectedFilename + "\",\"params\":[{\"name\":\"language\", \"value\":\"eng\"},{\"name\":\"author\", \"value\":\"J. Jansen\"}]}]}")
                .asString();

        assertThat(result.getStatus()).isIn(200, 201, 202);
        return JsonPath.parse(result.getBody()).read("$.workDir");
    }

    public static String deploymentIsFinished(String workDir) {
        logger.info(String.format("check deployment [%s] is finished", workDir));
        HttpResponse<String> statusResponse = pollAndAssert(() -> deploymentHasStatus(workDir, "FINISHED"));
        String outputFilePath = getOutputFilePath(statusResponse);
        logger.info(String.format("deployment has result file [%s]", outputFilePath));
        return outputFilePath;
    }

    private static String getOutputFilePath(HttpResponse<String> finishedDeployment) {
        String outputDir = JsonPath.parse(finishedDeployment.getBody()).read("$.outputDir");
        Path pathAbsolute = Paths.get(outputDir);
        Path pathBase = Paths.get("admin/files/");
        Path pathRelative = pathBase.relativize(pathAbsolute);
        String resultFileName = "result.txt";
        String outputPath = Paths.get(pathRelative.toString(), resultFileName).toString();
        logger.info(String.format("output file path is [%s]", outputPath));
        return outputPath;
    }

    public static void filesAreUnlocked(String inputFile, String testFileContent) {
        try {
            logger.info(String.format("check file [%s] is unlocked", inputFile));
            HttpResponse<String> downloadResult = downloadFile(inputFile);
            assertThat(downloadResult.getBody()).isEqualTo(testFileContent);
            assertThat(downloadResult.getStatus()).isIn(200, 202);

            HttpResponse<String> putAfterDeployment = putInputFile(inputFile);
            assertThat(putAfterDeployment.getStatus()).isEqualTo(204);

            HttpResponse<String> deleteInputFile = deleteInputFile(inputFile);
            assertThat(deleteInputFile.getStatus()).isEqualTo(204);
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
