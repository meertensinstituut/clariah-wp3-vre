package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path of a user file stored in nextcloud has the following structure:
 * `/{nextcloud}/{user}/{files}/{file}`
 */
public class NextcloudInputFile extends AbstractSwitchboardPath {

  private NextcloudInputFile(String user, String file) {
    this.user = user;
    this.file = file;
  }

  public static NextcloudInputFile from(ObjectPath objectPath) {
    return new NextcloudInputFile(
      objectPath.getUser(),
      objectPath.getFile()
    );
  }

  public String getNextcloud() {
    return nextcloud;
  }

  @Override
  public Path toPath() {
    return Paths.get(nextcloud, user, files, file);
  }

  /**
   * @return {user}/{files}/{file}
   */
  @Override
  public ObjectPath toObjectPath() {
    return new ObjectPath(Paths.get(user, files, file).toString());
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
