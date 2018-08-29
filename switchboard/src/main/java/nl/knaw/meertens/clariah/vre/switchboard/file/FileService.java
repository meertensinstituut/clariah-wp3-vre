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
    void stageFiles(String workDir, List<String> inputFiles);

    /**
     * Clean up files and folders used by deployed service:
     * - Unlock files
     * - Move output files to source
     * @return folder containing output files
     */
    void unstage(String workDir, List<String> inputFiles);

    /**
     * Lock "<source path>/<file string>"
     */
    void lock(String fileString);

    void unlock(String fileString);

    List<Path> unstageServiceOutputFiles(String workDir, String inputFile);

    Path unstageViewerOutputFile(String workDir, String inputFile, String service);

    String getContent(String inputFile);
}
