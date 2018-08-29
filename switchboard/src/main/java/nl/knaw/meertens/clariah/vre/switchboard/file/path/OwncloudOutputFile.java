package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import nl.knaw.meertens.clariah.vre.switchboard.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.*;

/**
 * Path of a output file stored in owncloud has the following structure:
 * `/{owncloud}/{user}/{files}/{output}/{file}`
 */
public class OwncloudOutputFile extends AbstractPath {

    private OwncloudOutputFile(String owncloud, String user, String files, String output, String file) {
        this.owncloud = owncloud;
        this.output = output;
        this.user = user;
        this.files = files;
        this.file = file;
    }

    @Override
    public Path toPath() {
        return Paths.get(owncloud, user, files, output, file);
    }

    @Override
    public String toObjectPath() {
        return Paths.get(user, files, output, file).toString();
    }

    public static OwncloudOutputFile from(OwncloudOutputDir dir, String file) {
        return new OwncloudOutputFile(
                OWNCLOUD_VOLUME,
                dir.getUser(),
                FILES_DIR,
                dir.getOutput(),
                file
        );
    }

    public String getOwncloud() {
        return owncloud;
    }

    public String getUser() {
        return user;
    }

    public String getFiles() {
        return files;
    }

    public String getOutput() {
        return output;
    }

    public String getFile() {
        return file;
    }

}
