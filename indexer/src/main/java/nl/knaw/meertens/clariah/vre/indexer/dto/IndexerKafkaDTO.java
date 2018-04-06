package nl.knaw.meertens.clariah.vre.indexer.dto;

public class IndexerKafkaDTO {

    public String object_id;
    public String action;
    public String file;
    public String user;
    public String created;
    
    @Override
    public String toString() {
        return "IndexerKafkaDTO{" +
                "object_id='" + object_id + "'," +
                "action='" + action + "'," +
                "file='" + file + "'," +
                "user='" + user + "'," +
                "}";
    }
}
