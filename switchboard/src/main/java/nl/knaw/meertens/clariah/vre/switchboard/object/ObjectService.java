package nl.knaw.meertens.clariah.vre.switchboard.object;

import nl.knaw.meertens.clariah.vre.switchboard.registry.objects.ObjectsRegistryService;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceRecord;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;

import java.util.List;

import static nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind.EDITOR;
import static nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind.SERVICE;
import static nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind.VIEWER;

public class ObjectService {

    private ObjectsRegistryService objectsRegistryService;
    private ServicesRegistryService servicesRegistryService;

    public ObjectService(ObjectsRegistryService objectsRegistryService, ServicesRegistryService servicesRegistryService) {
        this.objectsRegistryService = objectsRegistryService;
        this.servicesRegistryService = servicesRegistryService;
    }

    /**
     * Services of kind 'viewer'
     */
    public List<ServiceRecord> getViewersFor(long objectId) {
        var mimetype = objectsRegistryService.getObjectById(objectId).mimetype;
        return servicesRegistryService.getServicesByMimetypeAndKind(mimetype, VIEWER);
    }

    /**
     * Services of kind 'editor'
     */
    public List<ServiceRecord> getEditorsFor(long objectId) {
        var mimetype = objectsRegistryService.getObjectById(objectId).mimetype;
        return servicesRegistryService.getServicesByMimetypeAndKind(mimetype, EDITOR);
    }

    /**
     * Services of kind 'service' >:)
     */
    public List<ServiceRecord> getServiceServicesFor(Long objectId) {
        var mimetype = objectsRegistryService.getObjectById(objectId).mimetype;
        return servicesRegistryService.getServicesByMimetypeAndKind(mimetype, SERVICE);
    }
}
