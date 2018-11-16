package nl.knaw.meertens.clariah.vre.tagger.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TagDto {
  public String name;
  public String type;
  public String owner;

  public TagDto(String owner) {
    this.owner = owner;
  }
}
