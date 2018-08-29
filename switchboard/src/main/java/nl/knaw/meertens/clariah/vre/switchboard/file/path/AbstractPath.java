package nl.knaw.meertens.clariah.vre.switchboard.file.path;

import nl.knaw.meertens.clariah.vre.switchboard.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

abstract class AbstractPath {

    /**
     * Deployment dir that contains all workDirs and their temporary files
     */
    String tmp;

    /**
     * Owncloud root dir that contains users and their files
     */
    String owncloud;

    /**
     * Hidden folder in user folder that contains vre specific files
     */
    String vre;

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
    String files;

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
    String input;

    /**
     * Deployment dir that contains output files of a deployed service
     */
    String output;

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
        assertIsInputfile(objectPath);
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
        assertIsInputfile(objectPath);
        Path inputPath = Paths.get(objectPath);
        return inputPath.subpath(0, 1).toString();
    }

    private static void assertIsInputfile(String objectPath) {
        String pattern = "(.*)/" + Config.FILES_DIR + "/(.*)";
        boolean match = Pattern.compile(pattern).matcher(objectPath).matches();
        if(!match) {
            throw new IllegalArgumentException(String.format("objectPath [%s] did not match pattern [%s]", objectPath, pattern));
        }
    }
}
