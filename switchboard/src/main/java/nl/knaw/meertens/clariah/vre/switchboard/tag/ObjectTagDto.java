package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectTagDto {
  public Long id;
  public Long object;
  public Long tag;

  @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  public LocalDateTime timestamp;

}
