package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path of an output file of a deployed service
 * It has the following structure:
 * `/{tmp}/{workDir}/{output}/{objectPath}`
 */
public class DeploymentOutputFile extends AbstractSwitchboardPath {

  private DeploymentOutputFile(String workDir, String user, String file) {
    this.workDir = workDir;
    this.user = user;
    this.file = file;
  }

  public static DeploymentOutputFile from(String workDir, ObjectPath objectPath) {
    return new DeploymentOutputFile(
      workDir,
      objectPath.getUser(),
      objectPath.getFile()
    );
  }

  @Override
  public Path toPath() {
    return Paths.get(tmp, workDir, output, user, files, file);
  }

  @Override
  public ObjectPath toObjectPath() {
    return new ObjectPath(Paths.get(user, files, file).toString());
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
