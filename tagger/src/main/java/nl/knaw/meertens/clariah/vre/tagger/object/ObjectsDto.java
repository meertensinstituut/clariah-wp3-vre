package nl.knaw.meertens.clariah.vre.tagger.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectsDto {

  public Long id;

  @JsonProperty("time_created")
  public String timeCreated;

  @JsonProperty("time_changed")
  public String timeChanged;

  @JsonProperty("user_id")
  public String userId;
  public String type;
  public String mimetype;
  public String format;
  public String fits;
  public String filepath;
  public String filesize;

  @JsonProperty("metadata_id")
  public String metadataId;

  public boolean deleted;
}
