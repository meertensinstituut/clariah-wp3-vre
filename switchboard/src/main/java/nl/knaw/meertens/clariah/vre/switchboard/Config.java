package nl.knaw.meertens.clariah.vre.switchboard;

public class Config {

  public static final String DEPLOYMENT_VOLUME = System.getenv("DEPLOYMENT_VOLUME");
  public static final String DEPLOYMENT_HOST_NAME = "http://deployment:8080";
  public static final String KAFKA_HOST_NAME = "kafka:" + System.getenv("KAFKA_PORT");
  public static final String SWITCHBOARD_TOPIC_NAME = System.getenv("SWITCHBOARD_TOPIC_NAME");
  public static final String NEXTCLOUD_TOPIC_NAME = System.getenv("NEXTCLOUD_TOPIC_NAME");
  public static final String NEXTCLOUD_VOLUME = System.getenv("NEXTCLOUD_VOLUME");
  public static final String VRE_DIR = ".vre";
  public static final String OUTPUT_DIR = "output";
  public static final String INPUT_DIR = "input";
  public static final String FILES_DIR = "files";
  public static final String CONFIG_FILE_NAME = "config.json";
  public static final String STATUS_FILE_NAME = "switchboard.status.json";
  public static final String OBJECTS_DB_URL = "http://dreamfactory/api/v2/objects";
  public static final String OBJECTS_DB_KEY = System.getenv("APP_KEY_OBJECTS");
  public static final String SERVICES_DB_URL = "http://dreamfactory/api/v2/services";
  public static final String SERVICES_DB_KEY = System.getenv("APP_KEY_SERVICES");
  public static final long DEPLOYMENT_MEMORY_SPAN = 5; // seconds
  public static final int MIN_POLL_INTERVAL = 1; // seconds
  public static final String USER_TO_LOCK_WITH = "locker";
  public static final String USER_TO_UNLOCK_WITH = "www-data";

  // TODO: use shibboleth:
  public static final String TEST_USER = System.getenv("TEST_USER");

}
