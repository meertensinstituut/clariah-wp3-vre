package nl.knaw.meertens.clariah.vre.tagger;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static nl.knaw.meertens.clariah.vre.tagger.FileAction.CREATE;
import static nl.knaw.meertens.clariah.vre.tagger.FileAction.RENAME;
import static nl.knaw.meertens.clariah.vre.tagger.FileAction.UPDATE;

public class Config {
  public static final String KAFKA_SERVER = "kafka:" + requireNonNull(System.getenv("KAFKA_PORT"));
  public static final String RECOGNIZER_TOPIC_NAME = requireNonNull(System.getenv("RECOGNIZER_TOPIC_NAME"));
  public static final String RECOGNIZER_GROUP_NAME = requireNonNull(System.getenv("RECOGNIZER_GROUP_NAME"));
  public static final String TAGGER_TOPIC_NAME = requireNonNull(System.getenv("TAGGER_TOPIC_NAME"));
  public static final String OBJECTS_DB_URL = "http://dreamfactory/api/v2/objects";
  public static final String OBJECTS_DB_KEY = System.getenv("APP_KEY_OBJECTS");
  public static final String OBJECT_TABLE = "/_table/object";
  public static final List<String> ACTIONS_TO_TAG =
    newArrayList(CREATE.msgValue(), UPDATE.msgValue(), RENAME.msgValue());
  public static final String SYSTEM_TAG_OWNER = "system";
  // TODO: use shibboleth:
  public static final String TEST_USER = requireNonNull(System.getenv("TEST_USER"));
}
