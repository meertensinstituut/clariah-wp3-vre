package nl.knaw.meertens.clariah.vre.tracker;

import static java.util.Objects.requireNonNull;

public class Config {

  // Environment variables:
  public static final String KAFKA_SERVER = "kafka:" + requireNonNull(System.getenv("KAFKA_PORT"));
  public static final String RECOGNIZER_TOPIC_NAME = requireNonNull(System.getenv("RECOGNIZER_TOPIC_NAME"));
  public static final String RECOGNIZER_GROUP_NAME = requireNonNull(System.getenv("RECOGNIZER_GROUP_NAME"));
  public static final String TAGGER_TOPIC_NAME = requireNonNull(System.getenv("TAGGER_TOPIC_NAME"));

}
