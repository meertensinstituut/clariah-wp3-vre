package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path of a user file stored in nextcloud has the following structure:
 * `/{nextcloud}/{user}/{files}/{file}`
 */
public class OwncloudInputFile extends AbstractPath {

    private OwncloudInputFile(String user, String file) {
        this.user = user;
        this.file = file;
    }

    public String getOwncloud() {
        return nextcloud;
    }

    public static OwncloudInputFile from(String objectPath) {
        return new OwncloudInputFile(
                getUserFrom(objectPath),
                getFileFrom(objectPath)
        );
    }

    @Override
    public Path toPath() {
        return Paths.get(nextcloud, user, files, file);
    }

    /**
     * @return {user}/{files}/{file}
     */
    @Override
    public String toObjectPath() {
        return Paths.get(user, files, file).toString();
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
