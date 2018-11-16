package nl.knaw.meertens.clariah.vre.tagger.kafka;

public interface KafkaProducerService {

  void send(TaggerKafkaDto kafkaMsg);

}
