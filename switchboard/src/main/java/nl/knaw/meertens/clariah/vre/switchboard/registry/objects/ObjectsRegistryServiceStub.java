package nl.knaw.meertens.clariah.vre.switchboard.registry.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;

import static java.util.Objects.isNull;

public class ObjectsRegistryServiceStub implements ObjectsRegistryService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private HashMap<Long, ObjectsRecordDTO> testFileRecords = new HashMap<>();

    @Override
    public ObjectsRecordDTO getObjectById(Long id) {
        logger.info("Using stub");
        if (isNull(testFileRecords)) {
            throw new RuntimeException("No test file set in ObjectsRegistryServiceStub");
        }
        return testFileRecords.get(id);
    }

    public void addTestObject(ObjectsRecordDTO testFileRecords) {
        this.testFileRecords.put(testFileRecords.id, testFileRecords);
    }

    public Long getMaxTestObject() {
        if(testFileRecords.isEmpty()) {
            return 0L;
        }
        return Collections.max(testFileRecords.keySet());
    }

}
