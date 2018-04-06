package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.databind.JsonNode;

public class ParamDto {
    public String name;
    public ParamType type;
    public String value;
    public JsonNode params;
}
