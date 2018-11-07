package nl.knaw.meertens.clariah.vre.tagger;

import org.apache.commons.io.FileUtils;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FileUtil {

    public static String getTestFileContent(String fileName) {
        try {
            return FileUtils.readFileToString(FileUtils.toFile(
                    Thread.currentThread()
                            .getContextClassLoader()
                            .getResource(fileName)
            ), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
