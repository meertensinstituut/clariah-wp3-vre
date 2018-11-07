package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectsDto {

    public Long id;

    @JsonProperty("time_created")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS+00")
    public LocalDateTime timeCreated;

    @JsonProperty("time_changed")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSSSSS+00")
    public LocalDateTime timeChanged;

    public String user_id;
    public String type;
    public String mimetype;
    public String format;
    public String fits;
    public String filepath;
    public String filesize;
    public String metadata_id;
    public boolean deleted;
}
