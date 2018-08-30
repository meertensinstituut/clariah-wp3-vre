package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path of an output file of a deployed service
 * It has the following structure:
 * `/{tmp}/{workDir}/{output}/`
 */
public class DeploymentOutputDir extends AbstractPath {

    private DeploymentOutputDir(String workDir) {
        this.workDir = workDir;
    }

    @Override
    public Path toPath() {
        return Paths.get(tmp, workDir, output);
    }

    @Override
    public String toObjectPath() {
        throw new UnsupportedOperationException();
    }

    public static DeploymentOutputDir from(DeploymentInputFile file) {
        return new DeploymentOutputDir(file.getWorkDir());
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
