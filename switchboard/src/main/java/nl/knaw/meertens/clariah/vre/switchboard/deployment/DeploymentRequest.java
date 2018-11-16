package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.annotation.JsonFormat;
import nl.knaw.meertens.clariah.vre.switchboard.param.Param;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class DeploymentRequest {

  @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  private final LocalDateTime dateTime;
  private final String service;
  private final String workDir;
  private final List<Param> params;
  /**
   * Files used in deployment request
   * Key: object id
   * Value: dile path relative to data directory> files
   */
  private HashMap<Long, String> files;
  private DeploymentStatusReport statusReport;

  public DeploymentRequest(String service, String workDir, LocalDateTime dateTime, List<Param> params) {
    this.service = service;
    this.workDir = workDir;
    this.dateTime = dateTime;
    this.params = params;
    this.files = new HashMap<>();
  }

  public DeploymentStatusReport getStatusReport() {
    return statusReport;
  }

  public void setStatusReport(DeploymentStatusReport statusReport) {
    this.statusReport = statusReport;
  }

  public LocalDateTime getDateTime() {
    return dateTime;
  }

  public List<Param> getParams() {
    return params;
  }

  public HashMap<Long, String> getFiles() {
    return files;
  }

  public void setFiles(HashMap<Long, String> files) {
    this.files = files;
  }

  public String getService() {
    return service;
  }

  public String getWorkDir() {
    return workDir;
  }
}
