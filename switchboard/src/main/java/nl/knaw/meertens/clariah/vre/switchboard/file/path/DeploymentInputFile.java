package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.FILES_DIR;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.INPUT_DIR;

/**
 * Temporary path of a file used as an input file for a deployed service
 * It has the following structure:
 * `/{tmp}/{workDir}/{input}/{user}/{files}/{file}`
 */
public class DeploymentInputFile extends AbstractPath {

    private DeploymentInputFile(String tmp, String workDir, String input, String user, String files, String file) {
        this.tmp = tmp;
        this.workDir = workDir;
        this.input = input;
        this.user = user;
        this.files = files;
        this.file = file;
    }

    @Override
    public Path toPath() {
        return Paths.get(tmp, workDir, input, user, files, file);
    }

    @Override
    public String toObjectPath() {
        return Paths.get(user, files, file).toString();
    }

    public static DeploymentInputFile from(String workDir, String inputFile) {
        return new DeploymentInputFile(
                DEPLOYMENT_VOLUME,
                workDir,
                INPUT_DIR,
                getUserFrom(inputFile),
                FILES_DIR,
                getFileFrom(inputFile)
        );
    }

    public String getTmp() {
        return tmp;
    }

    public String getWorkDir() {
        return workDir;
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
