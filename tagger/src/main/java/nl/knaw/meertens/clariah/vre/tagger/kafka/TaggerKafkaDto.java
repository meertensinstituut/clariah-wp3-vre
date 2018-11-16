package nl.knaw.meertens.clariah.vre.tagger.kafka;

import org.joda.time.LocalDateTime;

public class TaggerKafkaDto {
  public String msg;
  public String owner;
  public Long object;
  public Long tag;
  public String dateTime = LocalDateTime.now().toString("yyyy-MM-dd HH:mm");
}
