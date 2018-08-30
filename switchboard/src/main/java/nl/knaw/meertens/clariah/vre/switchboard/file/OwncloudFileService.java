package nl.knaw.meertens.clariah.vre.switchboard.file;

import nl.knaw.meertens.clariah.vre.switchboard.Config;
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.getFileAttributeView;
import static java.nio.file.Files.setPosixFilePermissions;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

/**
 * Moves, locks and unlocks files in owncloud.
 */
public class OwncloudFileService implements FileService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final LockService locker;

    public OwncloudFileService() {
        this.locker = new LockService();
    }

    @Override
    public void stageFiles(String workDir, List<String> objectPaths) {
        List<OwncloudInputFile> inputPaths = objectPaths
                .stream()
                .map(OwncloudInputFile::from)
                .collect(toList());
        for (OwncloudInputFile file : inputPaths) {
            locker.lock(file);
            createSoftLink(workDir, file);
        }
    }

    /**
     * Unlock files and move output files to owncloud
     * <p>
     * At least one input file is needed,
     * next to which the output folder is created.
     */
    @Override
    public void unstage(String workDir, List<String> objectPaths) {
        unstageObjectPaths(objectPaths);
    }

    @Override
    public List<Path> unstageServiceOutputFiles(String workDir, String objectPath) {
        DeploymentInputFile inputFile = DeploymentInputFile.from(workDir, objectPath);
        OwncloudOutputDir outputDir = moveOutputDir(inputFile);
        List<OwncloudOutputFile> output = unlockOutputFiles(outputDir);
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
        for (String file : objectPaths) {
            unlock(file);
        }
    }

    /**
     * Move and unlock viewer output file.
     * Replaces viewer file if it already exists.
     *
     * @return path of viewer file in owncloud dir
     */
    @Override
    public Path unstageViewerOutputFile(String workDir, String objectPath, String service) {
        DeploymentOutputFile deploymentView = DeploymentOutputFile
                .from(workDir, objectPath);
        OwncloudViewPath owncloudView = OwncloudViewPath
                .from(service, objectPath);
        moveFile(deploymentView, owncloudView);
        locker.unlock(owncloudView);
        return Paths.get(owncloudView.toObjectPath());
    }

    private void moveFile(AbstractPath fromPath, AbstractPath toPath) {
        File from = fromPath.toPath().toFile();
        File to = toPath.toPath().toFile();
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
        File file = OwncloudInputFile
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
        DeploymentOutputDir deployment = DeploymentOutputDir.from(inputFile);
        OwncloudOutputDir owncloud = OwncloudOutputDir.from(inputFile);
        if (!hasOutput(deployment)) {
            createEmptyOutputFolder(inputFile.getWorkDir(), owncloud);
        } else {
            moveOutputDir(deployment, owncloud);
        }
        return owncloud;
    }

    private void createEmptyOutputFolder(String workDir, OwncloudOutputDir owncloudOutput) {
        logger.warn(String.format(
                "No output for [%s], create empty [%s]",
                workDir, owncloudOutput.toPath())
        );
        owncloudOutput
                .toPath()
                .toFile()
                .mkdirs();
    }

    private boolean hasOutput(DeploymentOutputDir file) {
        File outputDir = file.toPath().toFile();
        return outputDir.exists()
                &&
                outputDir.listFiles().length > 0;
    }


    private void moveOutputDir(DeploymentOutputDir deploymentOutput, OwncloudOutputDir outputDir) {
        Path deployment = deploymentOutput.toPath();
        Path owncloud = outputDir.toPath();
        try {
            logger.info(String.format(
                    "Move output dir from [%s] to [%s]",
                    deployment, owncloud
            ));
            FileUtils.moveDirectory(
                    deployment.toFile(),
                    owncloud.toFile()
            );
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                    "Could not move [%s] to [%s]",
                    deployment, owncloud
            ), e);
        }
    }

    private List<OwncloudOutputFile> unlockOutputFiles(OwncloudOutputDir outputDir) {
        List<OwncloudOutputFile> outputFiles = getFilesFromOutputDir(outputDir);
        for (OwncloudOutputFile file : outputFiles) {
            locker.unlockFileAndParents(file);
        }
        return outputFiles;
    }

    private List<OwncloudOutputFile> getFilesFromOutputDir(OwncloudOutputDir outputDir) {
        File[] outputFiles = outputDir
                .toPath()
                .toFile()
                .listFiles();
        if (isNull(outputFiles)) {
            return emptyList();
        }
        return Arrays
                .stream(outputFiles)
                .map(file -> OwncloudOutputFile.from(outputDir, file))
                .collect(toList());
    }


    private void createSoftLink(String workDir, OwncloudInputFile inputFile) {
        Path owncloud = inputFile.toPath();
        Path deployment = DeploymentInputFile
                .from(workDir, inputFile.toObjectPath())
                .toPath();
        deployment
                .toFile()
                .getParentFile()
                .mkdirs();
        try {
            Files.createSymbolicLink(deployment, owncloud);
        } catch (IOException e) {
            throw new RuntimeIOException(String.format(
                    "Could not link owncloud [%s] and input [%s]",
                    owncloud.toString(), deployment.toString()
            ), e);
        }
    }

}
