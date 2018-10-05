package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class ObjectTagDto {

    public Long id;
    public Long object;
    public Long tag;

    @JsonFormat(shape= STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSS")
    public LocalDateTime timestamp;

}
