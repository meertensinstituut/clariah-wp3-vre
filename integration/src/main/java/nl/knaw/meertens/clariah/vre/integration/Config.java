package nl.knaw.meertens.clariah.vre.integration;

public class Config {
    public final static String KAFKA_ENDPOINT = "kafka:" + System.getenv("KAFKA_PORT");
    public final static String NEXTCLOUD_ENDPOINT = "http://nextcloud:80/remote.php/webdav/";
    public final static String LAMACHINE_ENDPOINT = "http://lamachine/";
    public final static String NEXTCLOUD_ADMIN_NAME = System.getenv("NEXTCLOUD_ADMIN_NAME");
    public final static String NEXTCLOUD_ADMIN_PASSWORD = System.getenv("NEXTCLOUD_ADMIN_PASSWORD");
    public final static String NEXTCLOUD_TOPIC_NAME = System.getenv("NEXTCLOUD_TOPIC_NAME");
    public final static String RECOGNIZER_TOPIC_NAME = System.getenv("RECOGNIZER_TOPIC_NAME");
    public final static String TAGGER_TOPIC_NAME = System.getenv("TAGGER_TOPIC_NAME");
    public final static String DB_OBJECTS_USER = System.getenv("DB_OBJECTS_USER");
    public final static String DB_OBJECTS_PASSWORD = System.getenv("DB_OBJECTS_PASSWORD");
    public final static String DB_OBJECTS_DATABASE = System.getenv("DB_OBJECTS_DATABASE");
    public final static String SWITCHBOARD_ENDPOINT = "http://switchboard:8080/switchboard/rest";
    public final static int MAX_POLLING_PERIOD = 30;
}
