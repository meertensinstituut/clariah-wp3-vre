package nl.knaw.meertens.clariah.vre.switchboard.file;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.file.Files.getFileAttributeView;
import static java.nio.file.Files.setPosixFilePermissions;
import static java.nio.file.LinkOption.*;
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class OwncloudFileService implements FileService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Path srcPath;
    private final Path tmpPath;
    private final String outputDir;
    private final String inputDir;
    private final String locker;

    public OwncloudFileService(
            String srcPath,
            String tmpPath,
            String outputDir,
            String inputDir,
            String locker
    ) {
        this.srcPath = Paths.get(srcPath);
        this.tmpPath = Paths.get(tmpPath);
        this.outputDir = outputDir;
        this.inputDir = inputDir;
        this.locker = locker;
    }

    @Override
    public void stageFiles(String workDir, List<String> inputFiles) {
        Path inputPath = getInputDir(workDir);
        for (String file : inputFiles) {
            lock(file);
            createSoftLink(inputPath, file);
        }
    }

    /**
     * Unlock files and move output files to owncloud
     *
     * At least one input file is needed, next to which
     * an output folder is created.
     *
     * @return output files
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
        Path outputFilesDir = moveOutputFiles(workDir, inputFile);
        unlockOutputFiles(outputFilesDir);
        return getRelativePathsIn(outputFilesDir);
    }

    /**
     * @return path of viewer file in owncloud dir
     */
    @Override
    public Path unstageViewerOutputFile(String workDir, String inputFile, String service) {
        File resultFile = createViewerFilepath(inputFile, service);
        File tmpInputFile = createWorkdirFilepath(workDir, inputFile);
        try {
            FileUtils.moveFile(tmpInputFile, resultFile);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not move viewer file from [%s] to [%s]", tmpInputFile, resultFile));
        }
        return getPathRelativeToOwncloud(resultFile);
    }

    private Path getPathRelativeToOwncloud(File resultFile) {
        return resultFile.toPath().subpath(3, resultFile.toPath().getNameCount());
    }

    private File createWorkdirFilepath(String workDir, String inputFile) {
        return Paths.get(
                    tmpPath.toString(),
                    workDir,
                    "/input",
                    inputFile
            ).toFile();
    }

    private File createViewerFilepath(String inputFile, String service) {
        String toOwncloud = srcPath.toString();
        String toUser = getPathToUserDir(inputFile).toString();
        String fileInUserDir = getPathInUserDir(inputFile).toString();
        String toViewer = "/.vre/" + service;
        return Paths
                .get(toOwncloud, toUser, toViewer, fileInUserDir)
                .toFile();
    }

    /**
     * Path if input files constist of:
     * {username}/files/{path}
     * @return {username}/files
     */
    private Path getPathToUserDir(String inputFile) {
        Path inputPath = Paths.get(inputFile);
        return inputPath.subpath(0, 2);
    }

    /**
     * Path if input files constist of:
     * {username}/files/{path}
     * @return {path}
     */
    private Path getPathInUserDir(String inputFile) {
        Path inputPath = Paths.get(inputFile);
        return inputPath.subpath(2, inputPath.getNameCount());
    }

    @Override
    public Path getSrcPath() {
        return srcPath;
    }

    @Override
    public void lock(String fileString) {
        assert (!isBlank(fileString));
        Path file = toSrcPath(fileString);
        try {
            chown(file, locker);
            setPosixFilePermissions(file, get444());
        } catch (IOException e) {
            logger.error(String.format("Could not lock file [%s]", fileString), e);
        }
        logger.info(String.format("Locked file [%s]", file));
    }

    @Override
    public void unlock(String fileString) {
        assert (!isBlank(fileString));
        Path file = toSrcPath(fileString);
        try {
            chown(file, "www-data");
            setPosixFilePermissions(file, get644());
            Path parent = file.getParent();
            chown(parent, "www-data");
        } catch (IOException e) {
            logger.error(String.format("Could not unlock file [%s]", fileString), e);
        }
        logger.info(String.format("Unlocked file [%s]", file));
    }

    private Path toSrcPath(String fileString) {
        return new File(srcPath + "/" + fileString).toPath();
    }

    /**
     * Move output files to src, next to input file
     * in date and time labeled output folder
     *
     * @return output dir
     */
    private Path moveOutputFiles(String workDir, String file) {
        Path deploymentOutput = Paths.get(tmpPath.toString(), workDir, outputDir);
        String pathWithoutFile = FilenameUtils.getPath(file);
        Path outputDir = Paths.get(srcPath.toString(), pathWithoutFile, generateOutputDirName());
        outputDir.getParent().toFile().mkdirs();
        if (!deploymentOutput.toFile().exists()) {
            return createEmptyOutputFolder(workDir, outputDir);
        }
        return moveOutputFolder(deploymentOutput, outputDir);
    }

    private Path createEmptyOutputFolder(String workDir, Path outputDir) {
        logger.warn(String.format(
                "No output folder for deployment [%s], created empty output folder [%s]",
                workDir, outputDir.toString()
        ));
        outputDir.toFile().mkdirs();
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
            unlock(file);
            try {
                logger.info(String.format("Unlocking [%s]", file));
                unlockParents(toSrcPath(file), srcPath.getFileName().toString());
            } catch (IOException e) {
                throw new RuntimeIOException(String.format("Could not unlock [%s]", file), e);
            }
        }
    }

    private List<Path> getRelativePathsIn(Path outputDir) {
        return Arrays
                .stream(requireNonNull(outputDir.toFile().listFiles()))
                .map(file -> srcPath.relativize(file.toPath()))
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

    private String generateOutputDirName() {
        return outputDir + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS"));
    }

    private void removeSymbolicLinks(List<String> files, String workDir) {
        Path inputPath = getInputDir(workDir);
        for (String file : files) {
            removeSymbolicLink(inputPath, file);
            logger.info(String.format("Removed symbolic link for [%s]", file));
        }
    }

    private void removeSymbolicLink(Path inputPath, String relativeFilepath) {
        Path inputFilePath = Paths.get(inputPath.toString(), relativeFilepath);
        try {
            Files.delete(inputFilePath);
        } catch (IOException e) {
            throw new RuntimeIOException(String.format("Could not remove symbolic link [%s]", inputFilePath.toString()), e);
        }
    }

    private Path getInputDir(String workDir) {
        Path workDirPath = Paths.get(tmpPath.toString(), workDir);
        Path absoluteInputDir = Paths.get(workDirPath.toString(), inputDir);
        absoluteInputDir.toFile().mkdirs();
        return absoluteInputDir;
    }

    private void createSoftLink(Path inputPath, String relativeFilepath) {
        Path owncloudFilePath = Paths.get(srcPath.toString(), relativeFilepath);
        Path inputFilePath = Paths.get(inputPath.toString(), relativeFilepath);
        createFolders(inputPath, relativeFilepath);
        try {
            Files.createSymbolicLink(inputFilePath, owncloudFilePath);
            logger.info(String.format("Created symbolic link for [%s]", inputFilePath.toString()));
        } catch (IOException e) {
            throw new RuntimeIOException(
                    String.format("Could not create symbolic link between owncloud [%s] and input [%s]",
                    owncloudFilePath.toString(), inputFilePath.toString()), e
            );
        }
    }

    private void createFolders(Path inputPath, String relativeFilepath) {
        String relativeFilepathWithoutFilename = FilenameUtils.getPath(relativeFilepath);
        Paths.get(inputPath.toString(), relativeFilepathWithoutFilename).toFile().mkdirs();
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
