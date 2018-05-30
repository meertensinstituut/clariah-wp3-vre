package nl.knaw.meertens.clariah.vre.switchboard.object;

import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceRecordDTO;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;

import java.util.List;

public class ObjectService {

    private ObjectsRegistryService objectsRegistryService;
    private ServicesRegistryService servicesRegistryService;

    public ObjectService(ObjectsRegistryService objectsRegistryService, ServicesRegistryService servicesRegistryService) {
        this.objectsRegistryService = objectsRegistryService;
        this.servicesRegistryService = servicesRegistryService;
    }

    public List<ServiceRecordDTO> getServicesFor(Long objectId) {
        String mimetype = objectsRegistryService.getObjectById(objectId).mimetype;
        return servicesRegistryService.getServicesByMimetype(mimetype);
    }
}
