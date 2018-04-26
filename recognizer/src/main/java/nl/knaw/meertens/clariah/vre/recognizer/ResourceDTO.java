package nl.knaw.meertens.clariah.vre.recognizer;

/**
 * Wrapper needed for dreamfactory
 */
public class ResourceDTO {
    public String[] resource;

    public ResourceDTO(String resource) {
        this.resource = new String[]{resource};
    }
}
