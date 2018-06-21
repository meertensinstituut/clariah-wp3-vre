package nl.knaw.meertens.clariah.vre.switchboard.param;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ParamType {
    FILE("file"),
    DATE_TIME("date_time"),
    INTEGER("integer"),
    FLOAT("float"),
    BOOLEAN("boolean"),
    STRING("string");

    private final String key;

    ParamType(String key) {
        this.key = key;
    }

    @JsonCreator
    public static ParamType fromString(String key) {
        return ParamType.valueOf(key.toUpperCase());
    }

    @JsonValue
    public String getKey() {
        return key;
    }
}
