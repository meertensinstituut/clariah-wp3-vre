package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceRecordDTO {
    public Long id;
    public String name;
}
