package nl.knaw.meertens.clariah.vre.switchboard.object;

import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceRecordDto;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;

import java.util.List;

public class ObjectService {

    private ObjectsRegistryService objectsRegistryService;
    private ServicesRegistryService servicesRegistryService;

    public ObjectService(ObjectsRegistryService objectsRegistryService, ServicesRegistryService servicesRegistryService) {
        this.objectsRegistryService = objectsRegistryService;
        this.servicesRegistryService = servicesRegistryService;
    }

    public List<ServiceRecordDto> getServicesFor(Long objectId) {
        String mimetype = objectsRegistryService.getObjectById(objectId).mimetype;
        return servicesRegistryService.getServicesByMimetype(mimetype);
    }
}
