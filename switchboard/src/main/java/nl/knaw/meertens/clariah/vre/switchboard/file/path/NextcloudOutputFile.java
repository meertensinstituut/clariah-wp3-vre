package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path of a output file stored in nextcloud has the following structure:
 * `/{nextcloud}/{user}/{files}/{outputResult}/{file}`
 */
public class NextcloudOutputFile extends AbstractSwitchboardPath {

  private NextcloudOutputFile(String user, String outputResult, String file) {
    this.outputResult = outputResult;
    this.user = user;
    this.file = file;
  }

  public static NextcloudOutputFile from(NextcloudOutputDir dir, File file) {
    return new NextcloudOutputFile(
      dir.getUser(),
      dir.getOutputResult(),
      getRelativeFilePath(dir, file)
    );
  }

  private static String getRelativeFilePath(NextcloudOutputDir dir, File file) {
    return dir.toPath().relativize(file.toPath()).toString();
  }

  @Override
  public Path toPath() {
    return Paths.get(nextcloud, user, files, outputResult, file);
  }

  @Override
  public ObjectPath toObjectPath() {
    return new ObjectPath(Paths.get(user, files, outputResult, file).toString());
  }

  public String getNextcloud() {
    return nextcloud;
  }

  public String getUser() {
    return user;
  }

  public String getFiles() {
    return files;
  }

  public String getOutputResult() {
    return outputResult;
  }

  public String getFile() {
    return file;
  }

}
