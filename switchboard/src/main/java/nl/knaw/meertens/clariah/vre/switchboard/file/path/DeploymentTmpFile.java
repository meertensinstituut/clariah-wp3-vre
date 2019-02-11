package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path of an temporary output file of a deployed service
 * which will not be uploaded to nextcloud
 * It has the following structure:
 * `/{tmp}/{workDir}/{output}/{path}`
 */
public class DeploymentTmpFile extends AbstractSwitchboardPath {

  private final String tmpFile;

  private DeploymentTmpFile(String workDir, String tmpFile) {
    this.workDir = workDir;
    this.tmpFile = tmpFile;
  }

  public static DeploymentTmpFile from(String workDir, String tmpFile) {
    return new DeploymentTmpFile(
      workDir,
      tmpFile
    );
  }

  @Override
  public Path toPath() {
    return Paths.get(tmp, workDir, output, tmpFile);
  }

  @Override
  public ObjectPath toObjectPath() {
    throw new IllegalStateException("A temporary file cannot be converted into a objectPath");
  }

  public String getTmp() {
    return tmp;
  }

  public String getOutput() {
    return output;
  }

  public String getWorkDir() {
    return workDir;
  }

  private String getTmpFile() {
    return tmpFile;
  }
}
