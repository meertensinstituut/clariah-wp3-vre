package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import nl.knaw.meertens.clariah.vre.switchboard.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public abstract class AbstractPath {

    /**
     * Deployment dir that contains all workDirs and their temporary files
     */
    final String tmp = Config.DEPLOYMENT_VOLUME;

    /**
     * Owncloud root dir that contains users and their files
     */
    final String nextcloud = Config.NEXTCLOUD_VOLUME;

    /**
     * Hidden folder in user folder that contains vre specific files
     */
    final String vre = Config.VRE_DIR;

    /**
     * Folder of viewer service
     */
    String service;

    /**
     * Dir that contains all files of user
     * Is equal to user name
     */
    String user;

    /**
     * Dir that contains files uploaded by user
     */
    final String files = Config.FILES_DIR;

    /**
     * File and parent dirs as created by user
     */
    String file;

    /**
     * Deployment dir that contains config, input and output of a deployed service
     */
    String workDir;

    /**
     * Deployment dir that contains input files of a deployed service
     */
    final String input = Config.INPUT_DIR;

    /**
     * Deployment dir that contains output files of a deployed service
     */
    final String output = Config.OUTPUT_DIR;

    /**
     * Owncloud dir that contains output files of a finished deployment
     * Contains time stamp
     */
    String outputResult;

    /**
     * @return absolute path
     */
    public abstract Path toPath();

    /**
     * ObjectPath: path as found in object registry
     * @return relative path
     */
    public abstract String toObjectPath();

    /**
     * Object paths constist of:
     * {user}/{files}/{file}
     *
     * @return {file}
     */
    static String getFileFrom(String objectPath) {
        assertIsObjectPath(objectPath);
        Path inputPath = Paths.get(objectPath);
        return inputPath
                .subpath(2, inputPath.getNameCount())
                .toString();
    }

    /**
     * Object path constists of:
     * {user}/{files}/{file}
     * @return {user}
     */
    static String getUserFrom(String objectPath) {
        assertIsObjectPath(objectPath);
        Path inputPath = Paths.get(objectPath);
        return inputPath.subpath(0, 1).toString();
    }

    /**
     * Assert that objectPath has the following structure:
     * {user}/files/{rest/of/file/path.txt}
     * Nb. Validity of user name is not checked
     */
    private static void assertIsObjectPath(String objectPath) {
        String pattern = "(.*)/" + Config.FILES_DIR + "/(.*)";
        boolean match = Pattern
                .compile(pattern)
                .matcher(objectPath)
                .matches();
        if(!match) {
            throw new IllegalArgumentException(String.format(
                    "objectPath [%s] did not match pattern [%s]"
                    , objectPath, pattern
            ));
        }
    }
}
