package nl.knaw.meertens.clariah.vre.switchboard;

import javax.validation.constraints.NotNull;

public class SystemConfig {

  @NotNull public static final String DEPLOYMENT_VOLUME = System.getenv("DEPLOYMENT_VOLUME");
  @NotNull public static final String DEPLOYMENT_HOST_NAME = "http://deployment:8080";
  @NotNull public static final String KAFKA_HOST_NAME = "kafka:" + System.getenv("KAFKA_PORT");
  @NotNull public static final String SWITCHBOARD_TOPIC_NAME = System.getenv("SWITCHBOARD_TOPIC_NAME");
  @NotNull public static final String NEXTCLOUD_TOPIC_NAME = System.getenv("NEXTCLOUD_TOPIC_NAME");
  @NotNull public static final String NEXTCLOUD_VOLUME = System.getenv("NEXTCLOUD_VOLUME");
  @NotNull public static final String VRE_DIR = ".vre";
  @NotNull public static final String OUTPUT_DIR = "output";
  @NotNull public static final String INPUT_DIR = "input";
  @NotNull public static final String FILES_DIR = "files";
  @NotNull public static final String CONFIG_FILE_NAME = "config.json";
  @NotNull public static final String EDITOR_TMP = "editor.html";
  @NotNull public static final String EDITOR_OUTPUT = "result";
  @NotNull public static final String STATUS_FILE_NAME = "switchboard.status.json";
  @NotNull public static final String OBJECTS_DB_URL = "http://dreamfactory/api/v2/objects";
  @NotNull public static final String OBJECTS_DB_KEY = System.getenv("APP_KEY_OBJECTS");
  @NotNull public static final String SERVICES_DB_URL = "http://dreamfactory/api/v2/services";
  @NotNull public static final String SERVICES_DB_KEY = System.getenv("APP_KEY_SERVICES");
  @NotNull public static final long DEPLOYMENT_MEMORY_SPAN = 5; // seconds
  @NotNull public static final int MIN_POLL_INTERVAL = 1; // seconds
  @NotNull public static final String USER_TO_LOCK_WITH = "locker";
  @NotNull public static final String USER_TO_UNLOCK_WITH = "www-data";
  // TODO: use shibboleth:
  @NotNull public static final String TEST_USER = System.getenv("TEST_USER");

}
