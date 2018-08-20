package nl.knaw.meertens.clariah.vre.switchboard.param;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import static org.apache.commons.lang3.StringUtils.isBlank;

public enum ParamType {

    FILE("file"),

    DATE_TIME("date_time"),

    INTEGER("integer"),

    FLOAT("float"),

    BOOLEAN("boolean"),

    STRING("string"),

    /**
     * When type is enumeration, a param should have values:
     */
    ENUMERATION("enumeration");

    private final String key;

    ParamType(String key) {
        this.key = key;
    }

    @JsonCreator
    public static ParamType fromString(String key) {
        if(isBlank(key)) {
            throw new IllegalArgumentException("Cannot determine ParamType when key is blank");
        }
        return ParamType.valueOf(key.toUpperCase());
    }

    @JsonValue
    public String getKey() {
        return key;
    }
}
