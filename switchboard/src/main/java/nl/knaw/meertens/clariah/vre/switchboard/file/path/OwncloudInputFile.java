package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import nl.knaw.meertens.clariah.vre.switchboard.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.*;

/**
 * Path of a user file stored in owncloud has the following structure:
 * `/{owncloud}/{user}/{files}/{file}`
 */
public class OwncloudInputFile extends AbstractPath {

    private OwncloudInputFile(String user, String file) {
        this.user = user;
        this.file = file;
    }

    public String getOwncloud() {
        return owncloud;
    }

    public static OwncloudInputFile from(String objectPath) {
        return new OwncloudInputFile(
                getUserFrom(objectPath),
                getFileFrom(objectPath)
        );
    }

    @Override
    public Path toPath() {
        return Paths.get(owncloud, user, files, file);
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
