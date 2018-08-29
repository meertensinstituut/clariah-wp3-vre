package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import nl.knaw.meertens.clariah.vre.switchboard.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.*;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.DEPLOYMENT_VOLUME;
import static nl.knaw.meertens.clariah.vre.switchboard.Config.OUTPUT_DIR;

/**
 * Path of an output file of a deployed service
 * It has the following structure:
 * `/{tmp}/{workDir}/{output}/`
 */
public class DeploymentOutputFile extends AbstractPath {

    private DeploymentOutputFile(String tmp, String workDir, String output, String user, String files, String file) {
        this.tmp = tmp;
        this.workDir = workDir;
        this.output = output;
        this.user = user;
        this.files = files;
        this.file = file;
    }

    @Override
    public Path toPath() {
        return Paths.get(tmp, workDir, output, user, files, file);
    }

    @Override
    public String toObjectPath() {
        return Paths.get(user, files, file).toString();
    }

    public static DeploymentOutputFile from(String workDir, String inputFile) {
        return new DeploymentOutputFile(
                DEPLOYMENT_VOLUME,
                workDir,
                OUTPUT_DIR,
                getUserFrom(inputFile),
                FILES_DIR,
                getFileFrom(inputFile)
        );
    }

    public String getTmp() {
        return tmp;
    }

    public String getOutput() {
        return output;
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
