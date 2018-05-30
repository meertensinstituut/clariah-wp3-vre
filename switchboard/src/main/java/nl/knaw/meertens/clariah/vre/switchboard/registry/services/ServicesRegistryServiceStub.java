package nl.knaw.meertens.clariah.vre.switchboard.registry.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ServicesRegistryServiceStub implements ServicesRegistryService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ServiceRecordDTO stubService;

    @Override
    public List<ServiceRecordDTO> getServicesByMimetype(String mimetype) {
        logger.info("Using stub");
        return newArrayList(stubService);
    }

    public void setStubService(ServiceRecordDTO stubService) {
        this.stubService = stubService;
    }
}
