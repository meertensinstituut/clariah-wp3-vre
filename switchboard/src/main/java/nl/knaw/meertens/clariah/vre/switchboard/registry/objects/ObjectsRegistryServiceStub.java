package nl.knaw.meertens.clariah.vre.switchboard.registry.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.isNull;

public class ObjectsRegistryServiceStub implements ObjectsRegistryService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ObjectsRecordDTO testFileRecord;

    @Override
    public ObjectsRecordDTO getObjectById(Long id) {
        logger.info("Using stub");
        if (isNull(testFileRecord)) {
            throw new RuntimeException("No test file set in ObjectsRegistryServiceStub");
        }
        return testFileRecord;
    }

    public void setTestFileRecord(ObjectsRecordDTO testFileRecord) {
        this.testFileRecord = testFileRecord;
    }
}
