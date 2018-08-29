package nl.knaw.meertens.clariah.vre.switchboard.file;

import nl.knaw.meertens.clariah.vre.switchboard.Config;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.DeploymentInputFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.DeploymentOutputDir;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.DeploymentOutputFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.OwncloudInputFile;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.OwncloudOutputDir;
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
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Moves, locks and unlocks files in owncloud.
 * <p>
 * It expects the following owncloud dir structure:
 * /{srcPath}/{username}/files/{inputFile}
 * <p>
 * It expects the following tmp dir structures:
 * /{tmpPath}/{workDir}/{inputDir}/{inputFile}
 * /{tmpPath}/{workDir}/{outputDir}/{outputFile}
 * <p>
 * An input- and output file exist of the following properties:
 * {user}/files/{filepath}
 */
public class OwncloudFileService implements FileService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Root path containing files staged for deployment
     */
    private final Path tmpPath;

    /**
     * User that is used to lock staged files
     */
    private final String locker;

    public OwncloudFileService(
            String tmpPath,
            String locker
    ) {
        this.tmpPath = Paths.get(tmpPath);
        this.locker = locker;
    }

    @Override
    public void stageFiles(String workDir, List<String> inputFiles) {
        List<OwncloudInputFile> inputPaths = inputFiles.stream()
                .map(OwncloudInputFile::from)
                .collect(toList());

        for (OwncloudInputFile file : inputPaths) {
            lock(file.toObjectPath());
            createSoftLink(workDir, file);
        }
    }

    /**
     * Unlock files and move output files to owncloud
     * <p>
     * At least one input file is needed, next to which
     * an output folder is created.
     */
    @Override
    public void unstage(String workDir, List<String> inputFiles) {
        unstageInputFiles(inputFiles);
    }

    private void unstageInputFiles(List<String> inputFiles) {
        if (inputFiles.isEmpty()) {
            throw new IllegalArgumentException("Cannot move output when no input file is provided");
        }
        for (String file : inputFiles) {
            unlock(file);
        }
    }

    @Override
    public List<Path> unstageServiceOutputFiles(String workDir, String inputFile) {
        DeploymentInputFile owncloudPath = DeploymentInputFile.from(workDir, inputFile);
        Path outputFilesDir = moveOutputFiles(workDir, owncloudPath);
        unlockOutputFiles(outputFilesDir);
        return getRelativePathsIn(outputFilesDir);
    }

    /**
     * Move and unlock viewer output file.
     * Replaces viewer file if it already exists.
     *
     * @return path of viewer file in owncloud dir
     */
    @Override
    public Path unstageViewerOutputFile(
            String workDir,
            String inputFile,
            String service
    ) {
        File viewPath = DeploymentOutputFile
                .from(workDir, inputFile)
                .toPath()
                .toFile();
        OwncloudViewPath from = OwncloudViewPath
                .from(service, inputFile);
        File viewFile = from
                .toPath()
                .toFile();
        try {
            logger.info(String.format(
                    "Move viewer output from [%s] to [%s]",
                    viewPath, viewFile
            ));
            if (viewFile.exists()) {
                viewFile.delete();
            }
            FileUtils.moveFile(viewPath, viewFile);
            Path view = from
                    .toPath();
            unlockAbs(view);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not move viewer file from [%s] to [%s]", viewPath, viewFile), e);
        }
        return Paths.get(from.toObjectPath());
    }

    @Override
    public String getContent(String inputFile) {
        File file = OwncloudInputFile
                .from(inputFile)
                .toPath()
                .toFile();
        try {
            return FileUtils.readFileToString(file, UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Could not get content of inputFile [%s]", inputFile), e);
        }
    }

    private Path getPathRelativeToOwncloud(OwncloudViewPath view) {
        return Paths.get(
                view.getUser(),
                view.getFiles(),
                view.getVre(),
                view.getService(),
                view.getFile()
        );
    }

    @Override
    public void lock(String fileString) {
        Path file = OwncloudInputFile.from(fileString).toPath();
        try {
            chown(file, locker);
            setPosixFilePermissions(file, get444());
        } catch (IOException e) {
            logger.error(String.format("Could not lock file [%s]", fileString), e);
        }
        logger.info(String.format("Locked file [%s]", file));
    }

    @Override
    public void unlock(String inputfile) {
        Path file = OwncloudInputFile.from(inputfile).toPath();
        unlockAbs(file);
    }

    private void unlockAbs(Path absoluteFilePath) {
        try {
            chown(absoluteFilePath, "www-data");
            setPosixFilePermissions(absoluteFilePath, get644());
            Path parent = absoluteFilePath.getParent();
            chown(parent, "www-data");
        } catch (IOException e) {
            logger.error(String.format("Could not unlock file [%s]", absoluteFilePath.toString()), e);
        }
        logger.info(String.format("Unlocked file [%s]", absoluteFilePath));
    }

    /**
     * Move output files to src, next to input file
     * in date and time labeled output folder
     *
     * @return output dir
     */
    private Path moveOutputFiles(String workDir, DeploymentInputFile file) {
        Path deploymentOutput = DeploymentOutputDir.from(workDir).toPath();

        Path owncloudOutput = OwncloudOutputDir.from(file.getUser()).toPath();
        owncloudOutput.getParent().toFile().mkdirs();
        if (!deploymentOutput.toFile().exists()) {
            return createEmptyOutputFolder(workDir, owncloudOutput);
        }
        return moveOutputFolder(deploymentOutput, owncloudOutput);
    }

    private Path createEmptyOutputFolder(String workDir, Path outputDir) {
        outputDir.toFile().mkdirs();
        logger.warn(String.format("No output folder for deployment [%s], created empty [%s]", workDir, outputDir.toString()));
        return outputDir;
    }

    private Path moveOutputFolder(Path deploymentOutput, Path outputDir) {
        try {
            FileUtils.moveDirectory(deploymentOutput.toFile(), outputDir.toFile());
            logger.info(String.format(
                    "Move output files from workdir [%s] to [%s]",
                    deploymentOutput, outputDir
            ));
        } catch (IOException e) {
            throw new RuntimeIOException(
                    String.format("Could not move output folder from deployment [%s] to [%s]",
                            deploymentOutput.toString(), outputDir.toString()), e
            );
        }
        unlockOutputFiles(outputDir);
        return outputDir;
    }

    private void unlockOutputFiles(Path outputDir) {
        List<Path> outputFilePaths = getRelativePathsIn(outputDir);
        List<String> filePaths = outputFilePaths
                .stream()
                .map(Path::toString)
                .collect(toList());
        for (String file : filePaths) {
            unlockFileAndParents(file);
        }
    }

    private void unlockFileAndParents(String file) {
        unlock(file);
        try {
            logger.info(String.format("Unlocking [%s]", file));
            Path path = OwncloudInputFile.from(file).toPath();
            unlockParents(path, Paths.get(Config.OWNCLOUD_VOLUME).getFileName().toString());
        } catch (IOException e) {
            throw new RuntimeIOException(String.format("Could not unlock [%s]", file), e);
        }
    }

    private List<Path> getRelativePathsIn(Path outputDir) {
        return Arrays
                .stream(requireNonNull(outputDir.toFile().listFiles()))
                .map(file -> Paths.get(Config.OWNCLOUD_VOLUME).relativize(file.toPath()))
                .collect(toList());
    }

    private void unlockParents(Path path, String stopAt) throws IOException {
        Path parent = path.getParent();
        chown(parent, "www-data");
        setPosixFilePermissions(parent, get755());
        if (!parent.getFileName().toString().equals(stopAt)) {
            unlockParents(parent, stopAt);
        } else {
            logger.info(String.format("found endpoint [%s], stop unlocking", stopAt));
        }
    }

    private void createSoftLink(String workDir, OwncloudInputFile relativeFilepath) {
        Path owncloudFilePath = relativeFilepath.toPath();
        Path inputFilePath = DeploymentInputFile
                .from(workDir, relativeFilepath.toObjectPath())
                .toPath();
        inputFilePath
                .toFile()
                .getParentFile()
                .mkdirs();
        try {
            Files.createSymbolicLink(inputFilePath, owncloudFilePath);
            logger.info(String.format("Created symbolic link for [%s]", inputFilePath.toString()));
        } catch (IOException e) {
            throw new RuntimeIOException(String.format("Could not link owncloud [%s] and input [%s]",
                    owncloudFilePath.toString(), inputFilePath.toString()
            ), e);
        }
    }

    private void chown(Path file, String user) throws IOException {
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        PosixFileAttributeView fileAttributeView = getFileAttributeView(
                file, PosixFileAttributeView.class, NOFOLLOW_LINKS
        );
        fileAttributeView.setGroup(lookupService.lookupPrincipalByGroupName(user));
        fileAttributeView.setOwner(lookupService.lookupPrincipalByName(user));
    }

    private Set<PosixFilePermission> get644() {
        Set<PosixFilePermission> permissions = get444();
        permissions.add(OWNER_WRITE);
        return permissions;
    }

    private Set<PosixFilePermission> get755() {
        Set<PosixFilePermission> permissions = get444();
        permissions.add(OWNER_EXECUTE);
        permissions.add(OTHERS_EXECUTE);
        permissions.add(GROUP_EXECUTE);

        permissions.add(OWNER_WRITE);

        return permissions;
    }

    private Set<PosixFilePermission> get444() {
        Set<PosixFilePermission> permissions = new HashSet<>();
        permissions.add(OWNER_READ);
        permissions.add(OTHERS_READ);
        permissions.add(GROUP_READ);
        return permissions;
    }
}
