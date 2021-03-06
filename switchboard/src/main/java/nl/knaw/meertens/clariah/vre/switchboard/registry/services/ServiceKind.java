package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import com.fasterxml.jackson.annotation.JsonValue;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Types of services
 */
public enum ServiceKind {
  SERVICE("service"),
  VIEWER("viewer"),
  EDITOR("editor");

  private final String kind;

  ServiceKind(String kind) {
    this.kind = kind;
  }

  /**
   * ServiceKind is called 'kind' in services registry
   */
  public static ServiceKind fromString(String kind) {
    if (isBlank(kind)) {
      throw new IllegalArgumentException("Cannot determine ServiceKind when String kind is blank");
    }
    return ServiceKind.valueOf(kind.toUpperCase());
  }

  @JsonValue
  public String getKind() {
    return kind;
  }
}
