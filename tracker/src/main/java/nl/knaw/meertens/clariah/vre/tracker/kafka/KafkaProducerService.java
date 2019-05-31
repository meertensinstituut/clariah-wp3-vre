package nl.knaw.meertens.clariah.vre.tracker.kafka;

public interface KafkaProducerService {


  void send(TaggerKafkaDto kafkaMsg);

}
