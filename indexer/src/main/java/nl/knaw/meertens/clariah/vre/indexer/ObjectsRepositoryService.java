package nl.knaw.meertens.clariah.vre.indexer;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import nl.knaw.meertens.clariah.vre.indexer.dto.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.indexer.dto.ObjectsRecordListDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class ObjectsRepositoryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper mapper = new ObjectMapper();
    private final String objectTable = "/_table/object";
    private final String objectsDbUrl;
    private final String objectsDbKey;
    private final String objectsDbToken;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    

    ObjectsRepositoryService(String objectsDbUrl, String objectsDbKey, String objectsDbToken) {
        mapper.setTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"));
        mapper.setDateFormat(new SimpleDateFormat("dd-MM-yyyy hh:mm"));
        this.objectsDbUrl = objectsDbUrl;
        this.objectsDbKey = objectsDbKey;
        this.objectsDbToken = objectsDbToken;
    }

    public ObjectsRecordDTO getRecordByPath(String path) throws UnirestException, IOException {
     if(path!=null) {        
        GetRequest request = Unirest
            .get(objectsDbUrl + objectTable+"?limit=1&order=id%20DESC&filter=filepath="+path)
            .header("Content-Type", "application/json")
            .header("X-DreamFactory-Api-Key", objectsDbKey)
            .header("X-DreamFactory-Session-Token", objectsDbToken);
        HttpResponse<String> response = request.asString();
        logger.info("Objects registry responded on request for report "+path+" with: " + response.getStatus() + " - " + response.getStatusText());
        if(response.getStatus() == 200) {
            logger.info("Retrieved record from objects registry for report "+path);
            ObjectsRecordDTO record = fillObjectsRecordListDto(response.getBody());
            logger.info("record "+record.id);
            return record;
        } else {
            logger.info("Failed to get record from objects registry for report "+path);
            return null;
        }
      } else {
        logger.info("Failed to get record from objects registry, no path");
        return null;
      }           
    }
    
    public ObjectsRecordDTO getRecordById(String id) throws UnirestException, IOException {
      if(id!=null) {           
         GetRequest request = Unirest
             .get(objectsDbUrl + objectTable+"/"+id)
             .header("Content-Type", "application/json")
             .header("X-DreamFactory-Api-Key", objectsDbKey)
             .header("X-DreamFactory-Session-Token", objectsDbToken);
         HttpResponse<String> response = request.asString();
         logger.info("Objects registry responded on request for report "+id+" with: " + response.getStatus() + " - " + response.getStatusText());
         if(response.getStatus() == 200) {
             logger.info("Retrieved record from objects registry for report "+id);            
             return fillObjectsRecordDto(response.getBody());          
         } else {
             logger.info("Failed to get record from objects registryfor report "+id);
             return null;
         }
       } else {
         logger.info("Failed to get record from objects registry, no id");
         return null;
       }           
     }

    private ObjectsRecordDTO fillObjectsRecordDto(String json) throws JsonParseException, JsonMappingException, IOException {
      ObjectsRecordDTO msg = objectMapper.readValue(json, ObjectsRecordDTO.class);
      return msg;
    }

    private ObjectsRecordDTO fillObjectsRecordListDto(String json) throws JsonParseException, JsonMappingException, IOException {
      ObjectsRecordListDTO msg = objectMapper.readValue(json, ObjectsRecordListDTO.class);
      return msg.resource.get(0);
    }
    
    

}
