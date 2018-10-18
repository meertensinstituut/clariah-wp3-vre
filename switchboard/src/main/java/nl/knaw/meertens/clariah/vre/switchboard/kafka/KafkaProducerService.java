package nl.knaw.meertens.clariah.vre.switchboard.kafka;

public interface KafkaProducerService {

    void send(KafkaDto kafkaMsg);

}

