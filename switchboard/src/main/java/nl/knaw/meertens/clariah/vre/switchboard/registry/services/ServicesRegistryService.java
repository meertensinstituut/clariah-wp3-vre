package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import java.util.List;

public interface ServicesRegistryService {
    List<ServiceRecordDto> getServicesByMimetype(String mimetype);
}
