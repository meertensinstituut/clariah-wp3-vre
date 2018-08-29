package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import nl.knaw.meertens.clariah.vre.switchboard.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.meertens.clariah.vre.switchboard.Config.*;

/**
 * Path of an output file of a deployed service
 * It has the following structure:
 * `/{tmp}/{workDir}/{output}/`
 */
public class DeploymentOutputDir extends AbstractPath {

    private DeploymentOutputDir(String tmp, String workDir, String output) {
        this.tmp = tmp;
        this.workDir = workDir;
        this.output = output;
    }

    @Override
    public Path toPath() {
        return Paths.get(tmp, workDir, output);
    }

    @Override
    public String toObjectPath() {
        throw new UnsupportedOperationException();
    }

    public static DeploymentOutputDir from(String workDir) {
        return new DeploymentOutputDir(DEPLOYMENT_VOLUME, workDir, OUTPUT_DIR);
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

}
