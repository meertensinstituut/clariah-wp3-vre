package nl.knaw.meertens.clariah.vre.switchboard.registry;

import static java.util.Objects.isNull;

public class ObjectsRegistryServiceStub implements ObjectsRegistryService {
    private ObjectsRecordDTO testFileRecord;

    @Override
    public ObjectsRecordDTO getObjectById(Long id) {
        if (isNull(testFileRecord)) {
            throw new RuntimeException("No test file set in ObjectsRegistryServiceStub");
        }
        return testFileRecord;
    }

    public void setTestFileRecord(ObjectsRecordDTO testFileRecord) {
        this.testFileRecord = testFileRecord;
    }
}
