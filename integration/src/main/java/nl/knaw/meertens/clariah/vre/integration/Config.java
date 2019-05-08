package nl.knaw.meertens.clariah.vre.integration;

public class Config {
  public static final String KAFKA_ENDPOINT = "kafka:" + System.getenv("KAFKA_PORT");
  public static final String NEXTCLOUD_ENDPOINT = "http://nextcloud:80/remote.php/webdav/";
  public static final String LAMACHINE_ENDPOINT = "http://lamachine/";
  public static final String NEXTCLOUD_ADMIN_NAME = System.getenv("NEXTCLOUD_ADMIN_NAME");
  public static final String NEXTCLOUD_ADMIN_PASSWORD = System.getenv("NEXTCLOUD_ADMIN_PASSWORD");
  public static final String NEXTCLOUD_TOPIC_NAME = System.getenv("NEXTCLOUD_TOPIC_NAME");
  public static final String RECOGNIZER_TOPIC_NAME = System.getenv("RECOGNIZER_TOPIC_NAME");
  public static final String TAGGER_TOPIC_NAME = System.getenv("TAGGER_TOPIC_NAME");
  public static final String DB_OBJECTS_USER = System.getenv("DB_OBJECTS_USER");
  public static final String DB_OBJECTS_PASSWORD = System.getenv("DB_OBJECTS_PASSWORD");
  public static final String DB_OBJECTS_DATABASE = System.getenv("DB_OBJECTS_DATABASE");
  public static final String SWITCHBOARD_ENDPOINT = "http://switchboard:8080/switchboard/rest";
  public static final int MAX_POLLING_PERIOD = 30;
}
