package nl.knaw.meertens.clariah.vre.switchboard;

public class Config {
    public static final String DEPLOYMENT_VOLUME = System.getenv("DEPLOYMENT_VOLUME");
    public static final String DEPLOYMENT_HOST_NAME = "http://deployment:8080";
    public static final String KAFKA_HOST_NAME = "kafka:" + System.getenv("KAFKA_PORT");
    public static final String SWITCHBOARD_TOPIC_NAME = System.getenv("SWITCHBOARD_TOPIC_NAME");
    public static final String OWNCLOUD_TOPIC_NAME = System.getenv("OWNCLOUD_TOPIC_NAME");
    public static final String OWNCLOUD_VOLUME = System.getenv("OWNCLOUD_VOLUME");
    public static final String OUTPUT_DIR = "output";
    public static final String INPUT_DIR = "input";
    public static final String CONFIG_FILE_NAME = "config.json";
    public static final String STATUS_FILE_NAME = "switchboard.status.json";
    public static final String OBJECTS_DB_URL = "http://dreamfactory/api/v2/objects";
    public static final String OBJECTS_DB_KEY = System.getenv("APP_KEY_OBJECTS");
    public static final String OBJECTS_DB_TOKEN = System.getenv("OBJECTS_TOKEN");
}
