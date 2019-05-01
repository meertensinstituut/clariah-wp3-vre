package nl.knaw.meertens.clariah.vre.tagger.kafka;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;

public class TaggerKafkaDto {
  public String msg;
  public String owner;
  public Long object;
  public Long tag;
  public String dateTime = ofPattern("yyyy-MM-dd HH:mm").format(now());
}
