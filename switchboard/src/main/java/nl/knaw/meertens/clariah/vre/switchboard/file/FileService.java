package nl.knaw.meertens.clariah.vre.switchboard.file;

import nl.knaw.meertens.clariah.vre.switchboard.file.path.AbstractSwitchboardPath;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.ObjectPath;

import java.util.Collection;
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
  void stageFiles(String workDir, Collection<ObjectPath> objectPaths);

  /**
   * Clean up files and folders used by deployed service:
   * - Unlock files
   * - Move output files back to user storage
   */
  void unstage(String workDir, List<ObjectPath> objectPaths);

  List<ObjectPath> unstageServiceOutputFiles(String workDir, ObjectPath objectPath);

  ObjectPath unstageViewerOutputFile(String workDir, ObjectPath objectPath, String service);

  /**
   * Get content from nextcloud
   */
  String getContent(ObjectPath objectPath);

  void moveFile(AbstractSwitchboardPath fromPath, AbstractSwitchboardPath toPath);

  /**
   * Get content from deployment workDir
   */
  String getDeployContent(String workDir, String path);

  void unlock(ObjectPath objectPath);
}
