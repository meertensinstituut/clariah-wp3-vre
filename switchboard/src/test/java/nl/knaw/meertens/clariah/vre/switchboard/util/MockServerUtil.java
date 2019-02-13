package nl.knaw.meertens.clariah.vre.switchboard.util;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockServerUtil {
  private static Logger logger = LoggerFactory.getLogger(MockServerUtil.class);

  private static ClientAndServer mockServer;

  public static ClientAndServer getMockServer() {
    return mockServer;
  }

  public static void setMockServer(ClientAndServer mockServer) {
    MockServerUtil.mockServer = mockServer;
  }

  public static void startDeployMockServerWithUcto(Integer status) {
    String serviceName = "UCTO";
    startDeployMockServer(serviceName, status);
  }

  public static void startDeployMockServer(String serviceName, Integer status) {
    logger.info(format("start deploy mock server of service [%s] with status [%d]", serviceName, status));
    var deploymentStatus = DeploymentStatus.getDeployStatus(status);
    mockServer
      .when(
        request()
          .withMethod("PUT")
          .withPath("/deployment-service/a/exec/" + serviceName + "/.*"))
      .respond(
        response()
          .withStatusCode(status)
          .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
          .withBody("{\"message\":\"running\",\"status\":\"" + deploymentStatus.toString() + "\"}")
      );
  }

  public static void startServicesRegistryMockServer(String serviceJson) {
    mockServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/_table/service/"))
      .respond(
        response()
          .withStatusCode(200)
          .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
          .withBody("{ \"resource\": [" + serviceJson + "]}")
      );
  }

  public static void startOrUpdateStatusMockServer(int status, String workDir, String body, String serviceName) {
    var getStatusOfWorkDirRequest = request()
      .withMethod("GET")
      .withPath("/deployment-service/a/exec/" + serviceName + "/" + workDir);

    mockServer.clear(getStatusOfWorkDirRequest);

    mockServer
      .when(getStatusOfWorkDirRequest)
      .respond(
        response()
          .withStatusCode(status)
          .withHeaders(new Header("Content-Type", "application/json; charset=utf-8"))
          .withBody(body)
      );
  }
}
