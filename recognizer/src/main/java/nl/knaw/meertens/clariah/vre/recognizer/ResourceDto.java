package nl.knaw.meertens.clariah.vre.recognizer;

/**
 * Wrapper needed for dreamfactory
 */
public class ResourceDto {
  public String[] resource;

  public ResourceDto(String resource) {
    this.resource = new String[]{resource};
  }
}
