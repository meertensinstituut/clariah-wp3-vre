package nl.knaw.meertens.clariah.vre.indexer.dto;

/**
 * Wrapper needed for dreamfactory
 */
public class ResourceDTO {
    public ObjectsRecordDTO[] resource;

    public ResourceDTO(ObjectsRecordDTO resource) {
        this.resource = new ObjectsRecordDTO[]{resource};
    }
}
