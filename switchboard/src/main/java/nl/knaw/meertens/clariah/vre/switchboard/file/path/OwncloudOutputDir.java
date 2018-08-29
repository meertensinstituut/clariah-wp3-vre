package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.*;

/**
 * Path of a output dir stored in owncloud has the following structure:
 * `/{owncloud}/{user}/{files}/{output}/`
 */
public class OwncloudOutputDir extends AbstractPath {

    private OwncloudOutputDir(String owncloud, String user, String files, String output) {
        this.owncloud = owncloud;
        this.user = user;
        this.files = files;
        this.output = output;
    }

    @Override
    public Path toPath() {
        return Paths.get(owncloud, user, files, output);
    }

    @Override
    public String toObjectPath() {
        throw new UnsupportedOperationException();
    }

    public static OwncloudOutputDir from(String user) {
        return new OwncloudOutputDir(
                OWNCLOUD_VOLUME,
                user,
                FILES_DIR,
                generateOutputDirName()
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

    private static String generateOutputDirName() {
        return OUTPUT_DIR
                + "-"
                + now().format(ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
    }

}
