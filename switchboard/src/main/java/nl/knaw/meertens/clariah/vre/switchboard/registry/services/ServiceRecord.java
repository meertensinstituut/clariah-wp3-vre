package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceRecord {
  private Long id;
  private String name;
  private String kind;
  private String semantics;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getSemantics() {
    return semantics;
  }

  public void setSemantics(String semantics) {
    this.semantics = semantics;
  }
}
