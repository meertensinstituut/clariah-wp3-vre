package nl.knaw.meertens.clariah.vre.switchboard.registry.objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectsRecordDto {
  public Long id;
  public String filepath = "";
  public String mimetype = "";
}