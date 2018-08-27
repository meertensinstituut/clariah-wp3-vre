package nl.knaw.meertens.clariah.vre.switchboard.file;

import nl.knaw.meertens.clariah.vre.switchboard.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Path of a user file stored in owncloud has the following structure:
 * `/{owncloud}/{user}/{files}/{file}`
 */
public class OwncloudFilePath {

    static final Path FILES = Paths.get("files");

    /**
     * Root path containing input files
     */
    private static final Path srcPath = Paths.get(Config.OWNCLOUD_VOLUME);

    /**
     * Contains owncloud files
     */
    private Path owncloud;

    /**
     * Contains files of user
     * Is equal to user name
     */
    private Path user;


    /**
     * Stores files uploaded by user
     */
    private Path files;

    /**
     * Path to file as created by user
     */
    private Path file;

    private OwncloudFilePath(Path owncloud, Path user, Path files, Path file) {
        this.owncloud = owncloud;
        this.user = user;
        this.files = files;
        this.file = file;
    }

    public Path getOwncloud() {
        return owncloud;
    }

    public Path getUser() {
        return user;
    }

    public Path getFiles() {
        return files;
    }

    public Path getFile() {
        return file;
    }

    /**
     * Return {user}/{files}/{file}
     * @return
     */
    public Path getInputFile() {
        return Paths.get(
                user.toString(),
                files.toString(),
                file.toString()
        );
    }

    /**
     * Path of input file constists of:
     * {user}/{files}/{file}
     * @return {user}/files
     */
    static Path getPathToUserDir(String inputFile) {
        assertIsInputfile(inputFile);
        Path inputPath = Paths.get(inputFile);
        return inputPath.subpath(0, 2);
    }

    /**
     * Path of input file constists of:
     * {user}/{files}/{file}

     * @return {user}
     */
    private static Path getUserFrom(String inputFile) {
        assertIsInputfile(inputFile);
        Path inputPath = Paths.get(inputFile);
        return inputPath.subpath(0, 1);
    }

    /**
     * Path if input files constist of:
     * {user}/{files}/{file}
     *
     * @return {file}
     */
    public static Path getPathInUserDir(String inputFile) {
        assertIsInputfile(inputFile);
        Path inputPath = Paths.get(inputFile);
        return inputPath.subpath(2, inputPath.getNameCount());
    }

    private static void assertIsInputfile(String inputFile) {
        String pattern = "(.*)/" + FILES + "/(.*)";
        assert Pattern.compile(pattern).matcher(inputFile).matches();
    }

    public static List<OwncloudFilePath> convertToPaths(List<String> inputFiles) {
        return inputFiles
                .stream()
                .map(OwncloudFilePath::convertToPath)
                .collect(Collectors.toList());
    }

    public static OwncloudFilePath convertToPath(String inputFile) {
        return new OwncloudFilePath(
                srcPath,
                getUserFrom(inputFile),
                FILES,
                getPathInUserDir(inputFile)
        );
    }

    public Path toPath() {
        return Paths.get(
                this.owncloud.toString(),
                this.user.toString(),
                this.files.toString(),
                this.file.toString()
        );
    }
}
