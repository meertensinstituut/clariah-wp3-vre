package nl.knaw.meertens.clariah.vre.switchboard.kafka;

public class KafkaOwncloudCreateFileDto implements KafkaDto {
    public String action;
    public String user;
    public String userPath;
    public Long timestamp;
}
