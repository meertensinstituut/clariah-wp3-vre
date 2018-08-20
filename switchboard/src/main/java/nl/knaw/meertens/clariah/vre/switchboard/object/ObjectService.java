package nl.knaw.meertens.clariah.vre.switchboard.object;

import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceRecord;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;

import java.util.List;

import static nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind.SERVICE;

public class ObjectService {

    private ObjectsRegistryService objectsRegistryService;
    private ServicesRegistryService servicesRegistryService;

    public ObjectService(ObjectsRegistryService objectsRegistryService, ServicesRegistryService servicesRegistryService) {
        this.objectsRegistryService = objectsRegistryService;
        this.servicesRegistryService = servicesRegistryService;
    }

    public List<ServiceRecord> getServicesOfKindServiceFor(Long objectId) {
        String mimetype = objectsRegistryService.getObjectById(objectId).mimetype;
        return servicesRegistryService.getServicesByMimetypeAndKind(mimetype, SERVICE);
    }
}
