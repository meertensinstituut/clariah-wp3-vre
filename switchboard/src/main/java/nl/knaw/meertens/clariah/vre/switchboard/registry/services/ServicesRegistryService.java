package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import java.util.List;

public interface ServicesRegistryService {

    /**
     * String containing CMDI-xml of service
     */
    ServiceRecordDto getService(Long id);

    List<ServiceRecordDto> getServices(String mimetype);
}
