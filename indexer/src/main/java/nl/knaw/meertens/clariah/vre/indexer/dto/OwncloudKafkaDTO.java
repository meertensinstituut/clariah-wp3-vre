package nl.knaw.meertens.clariah.vre.indexer.dto;

public class OwncloudKafkaDTO {
    public String action;
    public String user;
    public String path;
    public String oldPath;
    public String newPath;
    public String userPath;
    public long timestamp;

    @Override
    public String toString() {
        return "OwncloudKafkaDTO{" +
                "action='" + action + '\'' +
                ", user='" + user + '\'' +
                ", path='" + path + '\'' +
                ", oldPath='" + oldPath + '\'' +
                ", newPath='" + newPath + '\'' +
                ", userPath='" + userPath + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

}