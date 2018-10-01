package nl.knaw.meertens.clariah.vre.switchboard.file;

import nl.knaw.meertens.clariah.vre.switchboard.Config;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.AbstractPath;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.HashSet;
import java.util.Set;

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

/**
 * Locks and unlocks files
 */
public class LockService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Lock staged files
     */
    private final String locker = Config.USER_TO_LOCK_WITH;

    /**
     * Unlocks staged files
     */
    private final String unlocker = Config.USER_TO_UNLOCK_WITH;

    void lock(AbstractPath path) {
        Path file = path.toPath();
        logger.info(String.format("Locking [%s]", file));
        try {
            chown(file, locker);
            setPosixFilePermissions(file, get444());
        } catch (IOException e) {
            logger.error(String.format("Could not lock [%s]", file), e);
        }
    }

    void unlock(AbstractPath abstractPath) {
        Path path = abstractPath.toPath();
        unlock(path);
    }

    void unlockFileAndParents(AbstractPath file) {
        unlock(file);
        try {
            logger.info(String.format(
                    "Unlocking parent dirs of [%s]", file.toPath()
            ));
            Path path = file.toPath();
            String nextcloudDir = Paths
                    .get(Config.NEXTCLOUD_VOLUME)
                    .getFileName()
                    .toString();
            unlockParents(path, nextcloudDir);
        } catch (IOException e) {
            throw new RuntimeIOException(String.format(
                    "Could not unlock [%s]", file
            ), e);
        }
    }

    private void unlockParents(Path path, String stopAt) throws IOException {
        Path parent = path.getParent();
        chown(parent, unlocker);
        setPosixFilePermissions(parent, get755());
        if (!parent.getFileName().toString().equals(stopAt)) {
            unlockParents(parent, stopAt);
        } else {
            logger.info(String.format("Found [%s], stop unlocking", stopAt));
        }
    }

    private void unlock(Path path) {
        logger.info(String.format("Unlocking [%s]", path));
        try {
            chown(path, unlocker);
            setPosixFilePermissions(path, get644());
            Path parent = path.getParent();
            chown(parent, unlocker);
        } catch (IOException e) {
            logger.error(String.format("Could not unlock [%s]", path), e);
        }
    }

    private void chown(Path file, String user) throws IOException {
        UserPrincipalLookupService lookupService = FileSystems
                .getDefault()
                .getUserPrincipalLookupService();
        PosixFileAttributeView fileAttributeView = getFileAttributeView(
                file, PosixFileAttributeView.class, NOFOLLOW_LINKS
        );
        fileAttributeView.setGroup(
                lookupService.lookupPrincipalByGroupName(user)
        );
        fileAttributeView.setOwner(
                lookupService.lookupPrincipalByName(user)
        );
    }

    private Set<PosixFilePermission> get644() {
        Set<PosixFilePermission> permissions = get444();
        permissions.add(OWNER_WRITE);
        return permissions;
    }

    private Set<PosixFilePermission> get755() {
        Set<PosixFilePermission> permissions = get444();
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
