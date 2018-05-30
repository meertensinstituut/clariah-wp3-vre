package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import java.util.List;

public interface ServicesRegistryService {
    List<ServiceRecordDTO> getServicesByMimetype(String mimetype);
}
