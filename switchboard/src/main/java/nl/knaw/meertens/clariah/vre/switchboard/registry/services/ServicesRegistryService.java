package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import java.util.List;

public interface ServicesRegistryService {

  /**
   * String containing CMDI-xml of service
   */
  ServiceRecord getService(Long id);

  ServiceRecord getServiceByName(String name);

  List<ServiceRecord> getServicesByMimetype(String mimetype);

  List<ServiceRecord> getServicesByMimetypeAndKind(String mimetype, ServiceKind kind);
}
