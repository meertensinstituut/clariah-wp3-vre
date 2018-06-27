package nl.knaw.meertens.clariah.vre.switchboard.param;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

// TODO: pull cmdi param en form dto apart:
public class ParamDto {
    public String name;

    // TODO: to param dto
    public String label;
    public String description;
    public ParamType type;
    public String minimumCardinality;
    public String maximumCardinality;
    public ParamType valuesType;
    public List<ParamValueDto> values = new ArrayList<>();

    // TODO: to form dto
    public JsonNode params; // TODO: replace with ParamGroupDto
    public String value;
}
