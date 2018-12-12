package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import java.util.List;

public interface ServicesRegistryService {

  /**
   * String containing CMDI-xml of service
   */
  ServiceRecord getService(Long id);

  ServiceRecord getServiceByName(String name);

  /**
   * Gets available services from the services registry
   * and checks validity of CMDI contained by semantics field
   */
  List<ServiceRecord> getServicesByMimetypeAndKind(String mimetype, ServiceKind kind);
}
