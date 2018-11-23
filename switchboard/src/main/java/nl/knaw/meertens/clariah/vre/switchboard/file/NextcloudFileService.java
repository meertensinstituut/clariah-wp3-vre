package nl.knaw.meertens.clariah.vre.switchboard.file;

import nl.knaw.meertens.clariah.vre.switchboard.file.path.AbstractPath;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.DeploymentInputFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.DeploymentOutputDir;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.DeploymentOutputFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.OwncloudInputFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.OwncloudOutputDir;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.OwncloudOutputFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.OwncloudViewPath;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

/**
 * Moves, locks and unlocks files in nextcloud.
 */
public class NextcloudFileService implements FileService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final LockService locker;

  public NextcloudFileService() {
    this.locker = new LockService();
  }

  @Override
  public void stageFiles(String workDir, List<String> objectPaths) {
    var inputPaths = objectPaths
      .stream()
      .map(OwncloudInputFile::from)
      .collect(toList());
    for (var file : inputPaths) {
      locker.lock(file);
      createSoftLink(workDir, file);
    }
  }

  /**
   * Unlock files and move output files to nextcloud
   *
   * <p>At least one input file is needed,
   * next to which the output folder is created.
   */
  @Override
  public void unstage(String workDir, List<String> objectPaths) {
    unstageObjectPaths(objectPaths);
  }

  @Override
  public List<Path> unstageServiceOutputFiles(String workDir, String objectPath) {
    var inputFile = DeploymentInputFile.from(workDir, objectPath);
    var outputDir = moveOutputDir(inputFile);
    var output = unlockOutputFiles(outputDir);
    return output
      .stream()
      .map(f -> Paths.get(f.toObjectPath()))
      .collect(toList());
  }

  @Override
  public void unlock(String objectPath) {
    locker.unlock(OwncloudInputFile.from(objectPath));
  }

  private void unstageObjectPaths(List<String> objectPaths) {
    if (objectPaths.isEmpty()) {
      throw new IllegalArgumentException(
        "Could not move output next to input: input file is unknown"
      );
    }
    for (var file : objectPaths) {
      unlock(file);
    }
  }

  /**
   * Move and unlock viewer output file.
   * Replaces viewer file if it already exists.
   *
   * @return path of viewer file in nextcloud dir
   */
  @Override
  public Path unstageViewerOutputFile(String workDir, String objectPath, String service) {
    var deploymentView = DeploymentOutputFile
      .from(workDir, objectPath);
    var nextcloudView = OwncloudViewPath
      .from(service, objectPath);
    moveFile(deploymentView, nextcloudView);
    locker.unlock(nextcloudView);
    return Paths.get(nextcloudView.toObjectPath());
  }

  private void moveFile(AbstractPath fromPath, AbstractPath toPath) {
    var from = fromPath.toPath().toFile();
    var to = toPath.toPath().toFile();
    logger.info(String.format(
      "Move [%s] to [%s]",
      from, to
    ));
    if (to.exists()) {
      to.delete();
    }
    try {
      FileUtils.moveFile(from, to);
    } catch (IOException e) {
      throw new RuntimeException(String.format(
        "Could not move [%s] to [%s]",
        from, to
      ), e);
    }
  }

  @Override
  public String getContent(String objectPath) {
    var file = OwncloudInputFile
      .from(objectPath)
      .toPath()
      .toFile();
    try {
      return FileUtils.readFileToString(file, UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException(String.format(
        "Could not get content of objectPath [%s]",
        objectPath
      ), e);
    }
  }

  /**
   * Move output files in folder next to input file
   * in date and time labeled output folder
   *
   * @return output dir
   */
  private OwncloudOutputDir moveOutputDir(DeploymentInputFile inputFile) {
    var deployment = DeploymentOutputDir.from(inputFile);
    var nextcloud = OwncloudOutputDir.from(inputFile);
    if (!hasOutput(deployment)) {
      createEmptyOutputFolder(inputFile.getWorkDir(), nextcloud);
    } else {
      moveOutputDir(deployment, nextcloud);
    }
    return nextcloud;
  }

  private void moveOutputDir(DeploymentOutputDir deploymentOutput, OwncloudOutputDir outputDir) {
    var deployment = deploymentOutput.toPath();
    var nextcloud = outputDir.toPath();
    try {
      logger.info(String.format(
        "Move output dir from [%s] to [%s]",
        deployment, nextcloud
      ));
      FileUtils.moveDirectory(
        deployment.toFile(),
        nextcloud.toFile()
      );
    } catch (IOException e) {
      throw new RuntimeException(String.format(
        "Could not move [%s] to [%s]",
        deployment, nextcloud
      ), e);
    }
  }

  private void createEmptyOutputFolder(String workDir, OwncloudOutputDir nextcloudOutput) {
    logger.warn(String.format(
      "No output for [%s], create empty [%s]",
      workDir, nextcloudOutput.toPath())
    );
    nextcloudOutput
      .toPath()
      .toFile()
      .mkdirs();
  }

  private boolean hasOutput(DeploymentOutputDir file) {
    var outputDir = file.toPath().toFile();
    return outputDir.exists() &&
      outputDir.listFiles().length > 0;
  }

  private List<OwncloudOutputFile> unlockOutputFiles(OwncloudOutputDir outputDir) {
    var outputFiles = getFilesFromOutputDir(outputDir);
    for (var file : outputFiles) {
      locker.unlockFileAndParents(file);
    }
    return outputFiles;
  }

  /**
   * Return files, and only files
   */
  private List<OwncloudOutputFile> getFilesFromOutputDir(OwncloudOutputDir outputDir) {
    var outputFiles = outputDir
      .toPath()
      .toFile()
      .listFiles(File::isFile);

    if (isNull(outputFiles)) {
      return emptyList();
    }
    return Arrays
      .stream(outputFiles)
      .map(file -> OwncloudOutputFile.from(outputDir, file))
      .collect(toList());
  }


  private void createSoftLink(String workDir, OwncloudInputFile inputFile) {
    var nextcloud = inputFile.toPath();
    var deployment = DeploymentInputFile
      .from(workDir, inputFile.toObjectPath())
      .toPath();
    deployment
      .toFile()
      .getParentFile()
      .mkdirs();
    try {
      Files.createSymbolicLink(deployment, nextcloud);
    } catch (IOException e) {
      throw new RuntimeIOException(String.format(
        "Could not link nextcloud [%s] and input [%s]",
        nextcloud.toString(), deployment.toString()
      ), e);
    }
  }

}
