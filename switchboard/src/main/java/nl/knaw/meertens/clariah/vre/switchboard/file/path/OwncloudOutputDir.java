package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;

/**
 * Path of a output dir stored in owncloud has the following structure:
 * `/{owncloud}/{user}/{files}/{outputResult}/`
 */
public class OwncloudOutputDir extends AbstractPath {

    private OwncloudOutputDir(String user, String outputResult) {
        this.user = user;
        this.outputResult = outputResult;
    }

    @Override
    public Path toPath() {
        return Paths.get(owncloud, user, files, outputResult);
    }

    @Override
    public String toObjectPath() {
        throw new UnsupportedOperationException();
    }

    public static OwncloudOutputDir from(DeploymentInputFile file) {
        return new OwncloudOutputDir(
                file.user,
                generateOutputPath(file)
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

    public String getOutputResult() {
        return outputResult;
    }

    /**
     * Put timestamped output dir next to input file
     */
    private static String generateOutputPath(DeploymentInputFile file) {
        String parent = FilenameUtils.getPath(file.getFile());
        return Paths.get(parent, generateOutputDir()).toString();
    }

    private static String generateOutputDir() {
        return OUTPUT_DIR
        + "-"
        + now().format(ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
    }
}
