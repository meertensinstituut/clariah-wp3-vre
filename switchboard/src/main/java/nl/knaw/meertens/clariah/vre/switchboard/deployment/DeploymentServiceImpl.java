package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.knaw.meertens.clariah.vre.switchboard.consumer.DeploymentConsumer;
import nl.knaw.meertens.clariah.vre.switchboard.poll.PollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus.DEPLOYED;

public class DeploymentServiceImpl implements DeploymentService {

  private final String hostName;
  private final RequestRepository requestRepositoryService;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public DeploymentServiceImpl(
    String hostName,
    RequestRepository requestRepositoryService,
    PollService pollService
  ) {
    this.hostName = hostName;
    this.requestRepositoryService = requestRepositoryService;
    pollService.startPolling();
  }

  @Override
  public DeploymentStatusReport deploy(
    DeploymentRequest request,
    DeploymentConsumer deploymentConsumer
  ) {
    var report = requestDeployment(request);
    requestRepositoryService.saveDeploymentRequest(report, deploymentConsumer);
    return report;
  }

  private DeploymentStatusReport requestDeployment(DeploymentRequest request) {
    var report = new DeploymentStatusReport();
    report.setUri(createDeployServiceUri(request));
    report.setService(request.getService());
    report.setFiles(new ArrayList<>(request.getFiles().values()));
    report.setWorkDir(request.getWorkDir());

    var response = sendRequest(report.getUri());
    var httpStatus = response.getStatus();
    report.setMsg(response.getBody());

    // 200 means successful deployment here (and not finished):
    if (httpStatus == 200) {
      report.setStatus(DEPLOYED);
    } else {
      report.setStatus(DeploymentStatus.getDeployStatus(httpStatus));
    }

    return report;
  }

  private URI createDeployServiceUri(DeploymentRequest request) {
    return URI.create(format(
      "%s/%s/%s/%s/",
      hostName,
      "deployment-service/a/exec",
      request.getService(),
      request.getWorkDir()
    ));
  }

  @Override
  public DeploymentStatusReport getStatus(String workDir) {
    return requestRepositoryService.getStatusReport(workDir);
  }

  @Override
  public boolean stop(String workDir) {
    throw new UnsupportedOperationException("Stopping deployments has not been implemented");
  }

  private HttpResponse<String> sendRequest(URI uri) {
    try {
      var response = Unirest
        .put(uri.toString())
        .asString();
      logger.info(format("Requested deployment of [%s]", uri.toString()));
      return response;
    } catch (UnirestException e) {
      throw new RuntimeException(format("Deployment request of [%s] failed", uri.toString()), e);
    }
  }

}
