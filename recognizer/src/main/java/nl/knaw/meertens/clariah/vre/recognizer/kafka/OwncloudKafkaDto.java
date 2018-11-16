package nl.knaw.meertens.clariah.vre.recognizer.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OwncloudKafkaDto {
  public String action;
  public String user;
  public String path;
  public String oldPath;
  public long timestamp;

  @Override
  public String toString() {
    return "OwncloudKafkaDto{" +
      "action='" + action + '\'' +
      ", user='" + user + '\'' +
      ", path='" + path + '\'' +
      ", oldPath='" + oldPath + '\'' +
      ", timestamp=" + timestamp +
      '}';
  }

}
