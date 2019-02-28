package nl.knaw.meertens.clariah.vre.switchboard.exec;

import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatus;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.DeploymentStatusReport;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class DeploymentStatusReportDto {

  public LocalDateTime polled;
  public DeploymentStatus status;
  public String msg;
  public String outputDir;
  public String service;
  public String workDir;
  public String viewerFile;
  public String viewerFileContent;

  /**
   * Return object paths as string to UI
   */
  public List<String> files;

  public DeploymentStatusReportDto(DeploymentStatusReport original) {
    this.polled = original.getPolled();
    this.status = original.getStatus();
    this.msg = original.getMsg();
    this.outputDir = original.getOutputDir();
    this.service = original.getService();
    this.workDir = original.getWorkDir();
    this.viewerFile = original.getViewerFile();
    this.viewerFileContent = original.getViewerFileContent();
    this.files = original
      .getFiles()
      .stream()
      .map((file) -> file.toPath().toString())
      .collect(toList());
  }

}
