package nl.knaw.meertens.clariah.vre.switchboard.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TagDto {
    public Long id;
    public String name;
    public String type;
    public String owner;
}
