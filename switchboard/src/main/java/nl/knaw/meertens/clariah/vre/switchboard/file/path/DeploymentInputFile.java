package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Temporary path of a file used as an object path for a deployed service
 * It has the following structure:
 * `/{tmp}/{workDir}/{input}/{user}/{files}/{file}`
 */
public class DeploymentInputFile extends AbstractSwitchboardPath {

  private DeploymentInputFile(String workDir, String user, String file) {
    this.workDir = workDir;
    this.user = user;
    this.file = file;
  }

  public static DeploymentInputFile from(String workDir, ObjectPath objectPath) {
    return new DeploymentInputFile(
      workDir,
      objectPath.getUser(),
      objectPath.getFile()
    );
  }

  @Override
  public Path toPath() {
    return Paths.get(tmp, workDir, input, user, files, file);
  }

  @Override
  public ObjectPath toObjectPath() {
    return ObjectPath.of(user, files, file);
  }

  public String getTmp() {
    return tmp;
  }

  public String getWorkDir() {
    return workDir;
  }

  public String getUser() {
    return user;
  }

  public String getFiles() {
    return files;
  }

  public String getFile() {
    return file;
  }

}
