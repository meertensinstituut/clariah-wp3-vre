package nl.knaw.meertens.clariah.vre.recognizer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OwncloudKafkaDTO {
    public String action;
    public String user;
    public String userPath;
    public long timestamp;

    @Override
    public String toString() {
        return "OwncloudKafkaDTO{" +
                "action='" + action + '\'' +
                ", user='" + user + '\'' +
                ", userPath='" + userPath + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

}