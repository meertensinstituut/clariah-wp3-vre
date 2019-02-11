package nl.knaw.meertens.clariah.vre.switchboard.registry.objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import nl.knaw.meertens.clariah.vre.switchboard.file.path.ObjectPath;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectsRecordDto {
  public Long id;

  /**
   * Note: Cannot annotate two constructors with JsonCreator,
   * so use ordinary String for filepath:
   */
  public String filepath;

  public String mimetype = "";
}
