package nl.knaw.meertens.clariah.vre.switchboard.file;

import java.nio.file.Path;
import java.util.List;

/**
 * FileService stages and unstages files and folders
 * found in srcPath for a service to be deployed.
 */
public interface FileService {

    /**
     * Create folder structure with input files for deployed service:
     * - Lock files
     * - Create workdir
     * - Create links of input files in work dir
     */
    void stage(String workDir, List<String> inputFiles);

    /**
     * Clean up files and folders used by deployed service:
     * - Unlock files
     * - Move output files to source
     * @return folder containing output files
     */
    String unstage(String workDir, List<String> inputFiles);

    /**
     * Lock "<source path>/<file string>"
     */
    void lock(String fileString);

    void unlock(String fileString);

    Path getSrcPath();
}
