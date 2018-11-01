package nl.knaw.meertens.clariah.vre.tagger;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static nl.knaw.meertens.clariah.vre.tagger.FileAction.CREATE;

public class Config {
    public static final String KAFKA_SERVER = "kafka:" + System.getenv("KAFKA_PORT");
    public static final String RECOGNIZER_TOPIC_NAME = System.getenv("RECOGNIZER_TOPIC_NAME");
    public static final String RECOGNIZER_GROUP_NAME = System.getenv("RECOGNIZER_GROUP_NAME");
    public static final String TAGGER_TOPIC_NAME = System.getenv("TAGGER_TOPIC_NAME");
    public static final String OBJECTS_DB_URL = "http://dreamfactory/api/v2/objects";
    public static final String OBJECTS_DB_KEY = System.getenv("APP_KEY_OBJECTS");
    public static final String OBJECT_TABLE = "/_table/object";
    public static final List<String> ACTIONS_TO_TAG = newArrayList(CREATE.msgValue());
}
