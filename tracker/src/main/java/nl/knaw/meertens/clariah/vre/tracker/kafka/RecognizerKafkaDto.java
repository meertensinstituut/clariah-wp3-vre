package nl.knaw.meertens.clariah.vre.tracker.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecognizerKafkaDto {
  public Long objectId;
  public String path;
  public String action;
  public String fitsFormat;
  public String fitsMimetype;
  public String fitsFullResult;
  public String oldPath;
}
