package nl.knaw.meertens.clariah.vre.recognizer;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.CREATE;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.UPDATE;

public class Config {
    public static final String KAFKA_SERVER = "kafka:" + System.getenv("KAFKA_PORT");
    public static final String OWNCLOUD_TOPIC_NAME = System.getenv("OWNCLOUD_TOPIC_NAME");
    public static final String OWNCLOUD_GROUP_NAME = System.getenv("OWNCLOUD_GROUP_NAME");
    public static final String RECOGNIZER_TOPIC_NAME = System.getenv("RECOGNIZER_TOPIC_NAME");
    public static final String FITS_FILES_ROOT = System.getenv("FITS_FILES_ROOT");
    public static final String FITS_URL = "http://fits:8080/fits/";
    public static final String OBJECTS_DB_URL = "http://dreamfactory/api/v2/objects";
    public static final String OBJECTS_DB_KEY = System.getenv("APP_KEY_OBJECTS");
    public static final String OBJECT_TABLE = "/_table/object";
    public static final List<String> ACTIONS_TO_PERSIST = newArrayList("create", "update", "rename", "delete");
    public static final String ACTION_RENAME = "update";
}
