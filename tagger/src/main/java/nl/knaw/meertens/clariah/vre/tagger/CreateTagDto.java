package nl.knaw.meertens.clariah.vre.tagger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTagDto {
    public String name;
    public String type;
    public String owner;
}
