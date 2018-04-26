package nl.knaw.meertens.clariah.vre.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OwncloudKafkaDTO {
    public String action;
    public String user;
    public String path;
    public String oldPath;
    public long timestamp;

    @Override
    public String toString() {
        return "OwncloudKafkaDTO{" +
                "action='" + action + '\'' +
                ", user='" + user + '\'' +
                ", path='" + path + '\'' +
                ", oldPath='" + oldPath + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

}