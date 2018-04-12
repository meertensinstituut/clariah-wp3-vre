package nl.knaw.meertens.clariah.vre.switchboard.file;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
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
import static java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class DeploymentFileService implements FileService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Path srcPath;
    private final Path tmpPath;
    private final String outputDir;
    private final String inputDir;

    public DeploymentFileService(String srcPath, String tmpPath, String outputDir, String inputDir) {
        this.srcPath = Paths.get(srcPath);
        this.tmpPath = Paths.get(tmpPath);
        this.outputDir = outputDir;
        this.inputDir = inputDir;
    }

    @Override
    public void stage(String workDir, List<String> inputFiles) {
        lockFiles(inputFiles);
        createSymbolicLinks(inputFiles, workDir);
    }

    @Override
    public List<Path> unstage(String workDir, List<String> inputFiles) {
        if (inputFiles.isEmpty()) {
            throw new IllegalArgumentException("Cannot move output when no input file is provided");
        }
        unlockFiles(inputFiles);
        Path outputFilesDir = moveOutputFiles(workDir, inputFiles.get(0));
        unlockOutputFiles(outputFilesDir);
        return getRelativePathsIn(outputFilesDir);
    }

    @Override
    public Path getSrcPath() {
        return srcPath;
    }

    /**
     * TODO: atm locking with root, use another user to lock file!
     */
    @Override
    public void lock(String fileString) {
        assert (!isBlank(fileString));
        Path file = toSrcPath(fileString);
        Path parent = file.getParent();
        try {
            chown(file, "root");
            setPosixFilePermissions(file, get444());
            chown(parent, "root");
            setPosixFilePermissions(parent, get555());
        } catch (IOException e) {
            handleException(e, "Could not lock file [%s]", file.toString());
        }
    }

    /**
     * TODO: atm locking with root, use another user to lock file!
     */
    @Override
    public void unlock(String fileString) {
        assert (!isBlank(fileString));
        Path file = toSrcPath(fileString);
        try {
            chown(file, "www-data");
            setPosixFilePermissions(file, get644());
            Path parent = file.getParent();
            chown(parent, "www-data");
            setPosixFilePermissions(parent, get755());
            chown(parent.getParent(), "www-data");
            setPosixFilePermissions(parent.getParent(), get755());
        } catch (IOException e) {
            handleException(e, "Could not unlock file [%s]", fileString);
        }
    }

    private Path toSrcPath(String fileString) {
        return new File(srcPath + "/" + fileString).toPath();
    }

    /**
     * Move output files back to src
     *
     * @return output dir
     */
    private Path moveOutputFiles(String workDir, String file) {
        Path deploymentOutput = Paths.get(tmpPath.toString(), workDir, outputDir);
        String pathWithoutFile = FilenameUtils.getPath(file);
        Path outputDir = Paths.get(srcPath.toString(), pathWithoutFile, generateOutputDirName());
        outputDir.getParent().toFile().mkdirs();
        try {
            FileUtils.moveDirectory(deploymentOutput.toFile(), outputDir.toFile());
            logger.info(String.format("Move output files from workdir [%s] to [%s]", deploymentOutput, outputDir));
        } catch (IOException e) {
            handleException(e, "Could not move output folder from deployment [%s] to [%s]", deploymentOutput.toString(), outputDir.toString());
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
                unlockParents(toSrcPath(file), srcPath.getFileName().toString());
            } catch (IOException e) {
                handleException(e, "Could not unlock parents of [%s]", file);
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
        logger.info(String.format("unlocking [%s]", path.toString()));
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
            handleException(e, "Could not remove symbolic link [%s]", inputFilePath.toString());
        }
    }

    private void lockFiles(List<String> files) {
        for (String file : files) {
            lock(file);
            logger.info(String.format("Locked [%s]", file));
        }
    }

    private void createSymbolicLinks(List<String> files, String workDir) {
        Path inputPath = getInputDir(workDir);
        for (String file : files) {
            createSoftLink(inputPath, file);
            logger.info(String.format("Created symbolic link for [%s]", file));
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
        } catch (IOException e) {
            handleException(e, "Could not create symbolic link between owncloud [%s] and input [%s]", owncloudFilePath.toString(), inputFilePath.toString());
        }
    }

    private void createFolders(Path inputPath, String relativeFilepath) {
        String relativeFilepathWithoutFilename = FilenameUtils.getPath(relativeFilepath);
        Paths.get(inputPath.toString(), relativeFilepathWithoutFilename).toFile().mkdirs();
    }

    private void unlockFiles(List<String> files) {
        for (String file : files) {
            unlock(file);
            logger.info(String.format("Unlocked [%s]", file));
        }
    }

    private void chown(Path file, String user) throws IOException {
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        GroupPrincipal group = lookupService.lookupPrincipalByGroupName(user);
        getFileAttributeView(file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);
    }

    private Set<PosixFilePermission> get644() {
        Set<PosixFilePermission> permissions = get444();
        permissions.add(OWNER_WRITE);
        return permissions;
    }

    private Set<PosixFilePermission> get755() {
        Set<PosixFilePermission> permissions = get555();
        permissions.add(OWNER_WRITE);
        return permissions;
    }

    private Set<PosixFilePermission> get555() {
        Set<PosixFilePermission> permissions = get444();
        permissions.add(OWNER_EXECUTE);
        permissions.add(OTHERS_EXECUTE);
        permissions.add(GROUP_EXECUTE);
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
