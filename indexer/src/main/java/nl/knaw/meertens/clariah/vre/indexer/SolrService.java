package nl.knaw.meertens.clariah.vre.indexer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import nl.knaw.meertens.clariah.vre.indexer.dto.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.indexer.dto.SolrRecordDTO;
import com.mashape.unirest.request.body.RequestBodyEntity;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class SolrService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper mapper = new ObjectMapper();
    private String solrUrl;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    

    SolrService(String solrUrl) {
        mapper.setTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"));
        mapper.setDateFormat(new SimpleDateFormat("dd-MM-yyyy hh:mm"));
        this.solrUrl = solrUrl;
    }

    public void createDocument(ObjectsRecordDTO record) throws UnirestException, IOException {
     if(record.id!=null) {    
        String json = createJson(record);
        logger.info("JSON: "+json);
        RequestBodyEntity request = Unirest
            .post(solrUrl+"update?commit=true")
            .header("Content-Type", "application/json")
            .body(json);                
        HttpResponse<String> response = request.asString();
        if(response.getStatus() == 200) {
          logger.info("Added record "+record.id+" to Solr");        
        } else {
          logger.info("Failed to add record "+record.id+" to Solr:" +response.getBody());
        }
      }          
    }
    
    private String createJson(ObjectsRecordDTO record) throws com.fasterxml.jackson.core.JsonProcessingException {
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(Include.NON_NULL);
      SolrRecordDTO document = new SolrRecordDTO(record);
      return "["+mapper.writeValueAsString(document)+"]";
  }

   
}
