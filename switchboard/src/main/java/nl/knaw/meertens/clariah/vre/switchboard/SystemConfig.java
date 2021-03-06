package nl.knaw.meertens.clariah.vre.switchboard;

import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class SystemConfig {

  // Environment variables:
  public static final String DEPLOYMENT_VOLUME = requireNonBlank(getenv("DEPLOYMENT_VOLUME"));
  public static final String KAFKA_HOST_NAME = "kafka:" + requireNonBlank(getenv("KAFKA_PORT"));
  public static final String SWITCHBOARD_TOPIC_NAME = requireNonBlank(getenv("SWITCHBOARD_TOPIC_NAME"));
  public static final String NEXTCLOUD_TOPIC_NAME = requireNonBlank(getenv("NEXTCLOUD_TOPIC_NAME"));
  public static final String NEXTCLOUD_VOLUME = requireNonBlank(getenv("NEXTCLOUD_VOLUME"));
  public static final String SERVICES_DB_KEY = requireNonBlank(getenv("APP_KEY_SERVICES"));
  public static final String OBJECTS_DB_KEY = requireNonBlank(getenv("APP_KEY_OBJECTS"));
  public static final String USER_TO_LOCK_WITH = requireNonBlank(getenv("USER_TO_LOCK_WITH"));
  public static final String USER_TO_UNLOCK_WITH = requireNonBlank(getenv("USER_TO_UNLOCK_WITH"));

  // TODO: use shibboleth:
  public static final String TEST_USER = requireNonBlank(getenv("TEST_USER"));

  public static final String DEPLOYMENT_HOST_NAME = "http://deployment:8080";
  public static final String VRE_DIR = ".vre";
  public static final String OUTPUT_DIR = "output";
  public static final String INPUT_DIR = "input";
  public static final String FILES_DIR = "files";
  public static final String CONFIG_FILE_NAME = "config.json";
  public static final String EDITOR_TMP = "editor.html";
  public static final String EDITOR_OUTPUT = "result";
  public static final String STATUS_FILE_NAME = "switchboard.status.json";
  public static final String OBJECTS_DB_URL = "http://dreamfactory/api/v2/objects";
  public static final String SERVICES_DB_URL = "http://dreamfactory/api/v2/services";
  public static final long DEPLOYMENT_MEMORY_SPAN = 5; // seconds
  public static final int MIN_POLL_INTERVAL = 1; // seconds

  private static String requireNonBlank(String field) {
    if (isBlank(field)) {
      throw new RuntimeException("Environment variable is not set");
    }
    return field;
  }

}
