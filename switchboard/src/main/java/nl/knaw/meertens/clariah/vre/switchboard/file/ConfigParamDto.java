package nl.knaw.meertens.clariah.vre.switchboard.file;

import com.fasterxml.jackson.databind.JsonNode;
import nl.knaw.meertens.clariah.vre.switchboard.deployment.ParamType;

public class ConfigParamDto {
    public String name;
    public ParamType type;
    public String value;
    public JsonNode params;
}
