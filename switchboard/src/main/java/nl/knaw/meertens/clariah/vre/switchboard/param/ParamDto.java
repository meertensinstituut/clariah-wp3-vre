package nl.knaw.meertens.clariah.vre.switchboard.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParamDto {
    public String name;
    public String label;
    public String description;
    public ParamType type;
    public String value;
    public String minimumCardinality;
    public String maximumCardinality;
    public JsonNode params; // TODO: replace with ParamGroupDto
}
