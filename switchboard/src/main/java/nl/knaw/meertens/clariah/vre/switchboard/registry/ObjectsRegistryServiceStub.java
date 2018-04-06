package nl.knaw.meertens.clariah.vre.switchboard.registry;

public class ObjectsRegistryServiceStub implements ObjectsRegistryService {
    private ObjectsRecordDTO testFileRecord;

    @Override
    public ObjectsRecordDTO getObjectById(Long id) {
        return testFileRecord;
    }

    public void setTestFileRecord(ObjectsRecordDTO testFileRecord) {
        this.testFileRecord = testFileRecord;
    }
}
