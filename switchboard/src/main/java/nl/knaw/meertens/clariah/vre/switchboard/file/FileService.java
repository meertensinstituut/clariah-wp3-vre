package nl.knaw.meertens.clariah.vre.switchboard.file;

import nl.knaw.meertens.clariah.vre.switchboard.file.path.AbstractSwitchboardPath;

import java.nio.file.Path;
import java.util.List;

/**
 * FileService stages and unstages files and folders
 * found in srcPath for a service to be deployed.
 */
public interface FileService {

  /**
   * Create folder structure with object paths for deployed service:
   * - Lock files
   * - Create workdir
   * - Create links of object paths in work dir
   */
  void stageFiles(String workDir, List<String> objectPaths);

  /**
   * Clean up files and folders used by deployed service:
   * - Unlock files
   * - Move output files back to source
   */
  void unstage(String workDir, List<String> objectPaths);

  List<Path> unstageServiceOutputFiles(String workDir, String objectPath);

  Path unstageViewerOutputFile(String workDir, String objectPath, String service);

  /**
   * Get content from nextcloud
   */
  String getContent(String objectPath);

  void moveFile(AbstractSwitchboardPath fromPath, AbstractSwitchboardPath toPath);

  /**
   * Get content from deployment workDir
   */
  String getDeployContent(String workDir, String objectPath);

  void unlock(String objectPath);
}
