package nl.knaw.meertens.clariah.vre.recognizer.object;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class ObjectsRecordDto {

  @JsonProperty("time_created")
  @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  public LocalDateTime timeCreated = LocalDateTime.MIN;

  @JsonProperty("time_changed")
  @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  public LocalDateTime timeChanged = LocalDateTime.MIN;

  @JsonProperty("user_id")
  public String userId = "";
  public String type = "";
  public String mimetype = "";
  public String format = "";
  public String fits = "";
  public String filepath = "";
  public String filesize = "";
  public String metadataid = "";
  public boolean deleted = false;
  public List<String> semanticTypes = "";
}
