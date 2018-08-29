package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.FILES_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OWNCLOUD_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.VRE_DIR;

/**
 * Path of a user file stored in owncloud has the following structure:
 * `/{owncloud}/{user}/{files}/{vre}/{service}/{file}`
 */
public class OwncloudViewPath extends AbstractPath {

    private OwncloudViewPath(String owncloud, String user, String files, String vre, String service, String file) {
        this.owncloud = owncloud;
        this.user = user;
        this.files = files;
        this.vre = vre;
        this.service = service;
        this.file = file;
    }

    @Override
    public Path toPath() {
        return Paths.get(owncloud, user, files, vre, service, file);
    }

    @Override
    public String toObjectPath() {
        return Paths.get(user, files, vre, service, file).toString();
    }

    public static OwncloudViewPath from(String service, String inputFile) {
        return new OwncloudViewPath(
                OWNCLOUD_VOLUME,
                getUserFrom(inputFile),
                FILES_DIR,
                VRE_DIR,
                service,
                getFileFrom(inputFile)
        );
    }

    public String getOwncloud() {
        return owncloud;
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
