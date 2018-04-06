package nl.knaw.meertens.clariah.vre.indexer.dto;

public class RecognizerKafkaDTO {
  
  public String objectId;
  public String path;
  public String fitsFormat;
  public String fitsMimetype;
  public String fitsFullResult;

    @Override
    public String toString() {
        return "RecognizerKafkaDTO{" +
                "path='" + path + '\'' +
                '}';
    }

}