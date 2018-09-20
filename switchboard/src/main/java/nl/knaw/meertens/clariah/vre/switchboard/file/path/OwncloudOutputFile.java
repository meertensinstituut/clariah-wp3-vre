package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.FILES_DIR;

/**
 * Path of a output file stored in owncloud has the following structure:
 * `/{owncloud}/{user}/{files}/{outputResult}/{file}`
 */
public class OwncloudOutputFile extends AbstractPath {

    private OwncloudOutputFile(String user, String outputResult, String file) {
        this.outputResult = outputResult;
        this.user = user;
        this.file = file;
    }

    @Override
    public Path toPath() {
        return Paths.get(owncloud, user, files, outputResult, file);
    }

    @Override
    public String toObjectPath() {
        return Paths.get(user, files, outputResult, file).toString();
    }

    public static OwncloudOutputFile from(OwncloudOutputDir dir, File file) {
        return new OwncloudOutputFile(
                dir.getUser(),
                dir.getOutputResult(),
                getRelativeFilePath(dir, file)
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

    public String getFile() {
        return file;
    }

    private static String getRelativeFilePath(OwncloudOutputDir dir, File file) {
        return dir.toPath().relativize(file.toPath()).toString();
    }

}
