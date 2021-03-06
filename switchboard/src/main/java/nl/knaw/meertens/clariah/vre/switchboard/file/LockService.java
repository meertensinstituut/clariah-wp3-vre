package nl.knaw.meertens.clariah.vre.switchboard.file;

import nl.knaw.meertens.clariah.vre.switchboard.file.path.AbstractSwitchboardPath;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.nio.file.Files.getFileAttributeView;
import static java.nio.file.Files.setPosixFilePermissions;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.NEXTCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.USER_TO_LOCK_WITH;
import static nl.knaw.meertens.clariah.vre.switchboard.SystemConfig.USER_TO_UNLOCK_WITH;

/**
 * Locks and unlocks files
 */
public class LockService {

  /**
   * Lock staged files
   */
  private final String locker = USER_TO_LOCK_WITH;

  /**
   * Unlocks staged files
   */
  private final String unlocker = USER_TO_UNLOCK_WITH;

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  void lock(AbstractSwitchboardPath path) {
    var file = path.toPath();
    logger.info("Locking [{}]", file);
    try {
      chown(file, locker);
      setPosixFilePermissions(file, get444());
    } catch (IOException e) {
      logger.error(format("Could not lock [%s] using [%s]", path.toString(), locker), e);
    }
  }

  void unlockFileAndParents(AbstractSwitchboardPath file) {
    unlock(file);
    try {
      logger.info(format(
        "Unlocking parent dirs of [%s]", file.toPath()
      ));
      var path = file.toPath();
      var nextcloudDir = Paths
        .get(NEXTCLOUD_VOLUME)
        .getFileName()
        .toString();
      unlockParents(path, nextcloudDir);
    } catch (IOException e) {
      throw new RuntimeIOException(format(
        "Could not unlock [%s]", file
      ), e);
    }
  }

  private void unlockParents(Path path, String stopAt) throws IOException {
    var parent = path.getParent();
    chown(parent, unlocker);
    setPosixFilePermissions(parent, get755());
    if (!parent.getFileName().toString().equals(stopAt)) {
      unlockParents(parent, stopAt);
    } else {
      logger.info(format("Found [%s], stop unlocking", stopAt));
    }
  }

  void unlock(AbstractSwitchboardPath abstractPath) {
    var path = abstractPath.toPath();
    unlock(path);
  }

  private void unlock(Path path) {
    logger.info(format("Unlocking [%s]", path));
    try {
      chown(path, unlocker);
      setPosixFilePermissions(path, get644());
      var parent = path.getParent();
      chown(parent, unlocker);
    } catch (IOException e) {
      logger.error(format("Could not unlock [%s] using [%s]", path.toString(), unlocker), e);
    }
  }

  private void chown(Path file, String user) throws IOException {
    logger.info("Chown using [{}]", locker);
    var lookupService = FileSystems
      .getDefault()
      .getUserPrincipalLookupService();
    var fileAttributeView = getFileAttributeView(
      file, PosixFileAttributeView.class, NOFOLLOW_LINKS
    );
    try {
      fileAttributeView.setGroup(lookupService.lookupPrincipalByGroupName(user));
    } catch (UserPrincipalNotFoundException e) {
      logger.error("Could not find user group to lock with [{}]", user);
    }
    fileAttributeView.setOwner(
      lookupService.lookupPrincipalByName(user)
    );
  }

  private Set<PosixFilePermission> get644() {
    var permissions = get444();
    permissions.add(OWNER_WRITE);
    return permissions;
  }

  private Set<PosixFilePermission> get755() {
    var permissions = get444();
    permissions.add(OWNER_EXECUTE);
    permissions.add(OTHERS_EXECUTE);
    permissions.add(GROUP_EXECUTE);

    permissions.add(OWNER_WRITE);

    return permissions;
  }

  private Set<PosixFilePermission> get444() {
    Set<PosixFilePermission> permissions = new HashSet<>();
    permissions.add(OWNER_READ);
    permissions.add(OTHERS_READ);
    permissions.add(GROUP_READ);
    return permissions;
  }

}
