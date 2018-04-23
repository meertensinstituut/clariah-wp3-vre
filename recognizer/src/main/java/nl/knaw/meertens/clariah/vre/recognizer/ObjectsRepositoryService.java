package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.JsonPath;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.RequestBodyEntity;
import nl.knaw.meertens.clariah.vre.recognizer.dto.ObjectsRecordDTO;
import nl.knaw.meertens.clariah.vre.recognizer.dto.ResourceDTO;
import nl.knaw.meertens.clariah.vre.recognizer.fits.output.IdentificationType.Identity;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ObjectsRepositoryService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String objectsDbUrl;
    private final String objectsDbKey;

    ObjectsRepositoryService(String objectsDbUrl, String objectsDbKey) {
        this.objectsDbUrl = objectsDbUrl;
        this.objectsDbKey = objectsDbKey;
    }

    /**
     * Create new object in object registry
     * @return ID
     */
    public Long perist(Report report) throws UnirestException, IOException {
        String recordJson = createJson(report);
        String persistResult = addNewRecord(recordJson);
        return getId(persistResult);
    }

    /**
     * Get id of persisted object by json path: $.resource[0].id
     */
    private Long getId(String object) {
        Integer read = JsonPath.parse(object).read("$.resource[0].id");
        logger.debug(String.format("persisted result [%s] with id [%s]", object, read));
        return read.longValue();
    }

    public String createJson(Report report) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(WRITE_DATES_AS_TIMESTAMPS);

        ObjectsRecordDTO record = fillObjectsRecordDto(report);
        return mapper.writeValueAsString(new ResourceDTO(record));
    }

    private String addNewRecord(String recordJson) throws UnirestException, IOException {
        if(!hasAllObjectsDbDetails()) {
            return null;
        }
        String objectTable = "/_table/object";
        RequestBodyEntity request = Unirest
                .post(objectsDbUrl + objectTable)
                .header("Content-Type", "application/json")
                .header("X-DreamFactory-Api-Key", objectsDbKey)
                .body(recordJson);

        HttpResponse<String> response = request.asString();
        logger.info("Objects registry responded with: " + response.getStatus() + " - " + response.getStatusText());

        if(response.getStatus() == 200) {
            logger.info("Added new record to objects registry");
        } else {
            logger.info("Failed to add record to objects registry: " + recordJson);
            logger.info("Failed request: " + IOUtils.toString(request.getEntity().getContent(), UTF_8));
        }
        return request.asString().getBody();
    }

    private ObjectsRecordDTO fillObjectsRecordDto(Report report) {
        ObjectsRecordDTO msg = new ObjectsRecordDTO();
        msg.filepath = report.getPath();
        Identity identity = report.getFits().getIdentification().getIdentity().get(0);
        msg.fits = report.getXml();
        msg.format = identity.getFormat();
        msg.mimetype = identity.getMimetype();
        msg.timechanged = LocalDateTime.now();
        msg.timecreated = LocalDateTime.now();
        msg.user_id = report.getUser();
        msg.type = "object";
        return msg;
    }

    private boolean hasAllObjectsDbDetails() {
        if(isBlank(objectsDbKey)) {
            logger.warn("Key of object registry is not set");
            return false;
        }
        if(isBlank(objectsDbUrl)) {
            logger.warn("Url of object registry is not set");
            return false;
        }
        return true;
    }

}
