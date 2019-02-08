package nl.knaw.meertens.clariah.vre.switchboard.registry.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;

import static java.util.Objects.isNull;

public class ObjectsRegistryServiceStub implements ObjectsRegistryService {
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private HashMap<Long, ObjectsRecordDto> testFileRecords = new HashMap<>();

  @Override
  public ObjectsRecordDto getObjectById(Long id) {
    logger.info("Using stub");
    if (isNull(testFileRecords)) {
      throw new RuntimeException("No test file set in ObjectsRegistryServiceStub");
    }
    return testFileRecords.get(id);
  }

  public void addTestObject(ObjectsRecordDto testFileRecords) {
    this.testFileRecords.put(testFileRecords.id, testFileRecords);
  }

  public Long getNewId() {
    if (testFileRecords.isEmpty()) {
      return 1L;
    }
    return Collections.max(testFileRecords.keySet()) + 1;
  }

}
