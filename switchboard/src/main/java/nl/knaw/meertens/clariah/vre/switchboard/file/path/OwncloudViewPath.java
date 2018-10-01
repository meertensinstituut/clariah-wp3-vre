package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * View path of a user file stored in nextcloud has the following structure:
 * `/{nextcloud}/{user}/{files}/{vre}/{service}/{file}`
 */
public class OwncloudViewPath extends AbstractPath {

    private OwncloudViewPath(String user, String service, String file) {
        this.user = user;
        this.service = service;
        this.file = file;
    }

    @Override
    public Path toPath() {
        return Paths.get(nextcloud, user, files, vre, service, file);
    }

    @Override
    public String toObjectPath() {
        return Paths.get(user, files, vre, service, file).toString();
    }

    public static OwncloudViewPath from(String service, String objectPath) {
        return new OwncloudViewPath(
                getUserFrom(objectPath),
                service,
                getFileFrom(objectPath)
        );
    }

    public String getOwncloud() {
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
