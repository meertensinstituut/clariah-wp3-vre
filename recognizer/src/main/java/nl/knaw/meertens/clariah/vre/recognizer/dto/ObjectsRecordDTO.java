package nl.knaw.meertens.clariah.vre.recognizer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class ObjectsRecordDTO {

    @JsonFormat(shape= STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSS")
    public LocalDateTime timechanged = LocalDateTime.MIN;

    @JsonFormat(shape= STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSS")
    public LocalDateTime timecreated = LocalDateTime.MIN;

    public String user_id = "";
    public String type = "";
    public String mimetype = "";
    public String format = "";
    public String fits = "";
    public String filepath = "";
    public String filesize = "";
    public String metadataid = "";
}
