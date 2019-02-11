package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * View of a user file stored in nextcloud has the following structure:
 * `/{nextcloud}/{user}/{files}/{vre}/{service}/{file}`
 */
public class NextcloudViewPath extends AbstractSwitchboardPath {

  private NextcloudViewPath(String user, String service, String file) {
    this.user = user;
    this.service = service;
    this.file = file;
  }

  public static NextcloudViewPath from(String service, ObjectPath objectPathToView) {
    return new NextcloudViewPath(
      objectPathToView.getUser(),
      service,
      objectPathToView.getFile()
    );
  }

  @Override
  public Path toPath() {
    return Paths.get(nextcloud, user, files, vre, service, file);
  }

  @Override
  public ObjectPath toObjectPath() {
    return ObjectPath.of(user, files, vre, service, file);
  }

  public String getNextcloud() {
    return nextcloud;
  }

  public String getVre() {
    return vre;
  }

  public String getService() {
    return service;
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
