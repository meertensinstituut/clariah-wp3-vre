package nl.knaw.meertens.clariah.vre.switchboard.deployment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ParamType {
    FILE("file");

    private final String key;

    ParamType(String key) {
        this.key = key;
    }

    @JsonCreator
    public static ParamType fromString(String key) {
        return key == null
                ? null
                : ParamType.valueOf(key.toUpperCase());
    }

    @JsonValue
    public String getKey() {
        return key;
    }
}
