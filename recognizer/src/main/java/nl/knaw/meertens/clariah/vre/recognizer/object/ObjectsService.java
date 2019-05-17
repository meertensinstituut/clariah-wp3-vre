package nl.knaw.meertens.clariah.vre.recognizer.object;

import nl.knaw.meertens.clariah.vre.recognizer.MimetypeService;
import nl.knaw.meertens.clariah.vre.recognizer.Report;
import nl.knaw.meertens.clariah.vre.recognizer.SemanticTypeService;
import nl.knaw.meertens.clariah.vre.recognizer.fits.FitsService;

import java.nio.file.Paths;
import java.time.LocalDateTime;

public class ObjectsService {

  private final MimetypeService mimetypeService;
  private final SemanticTypeService semanticTypeService;
  private final ObjectRepository objectRepository;
  private final ObjectSemanticTypeRepository objectSemanticTypeRepository;

  public ObjectsService(
    MimetypeService mimetypeService,
    SemanticTypeService semanticTypeService,
    ObjectRepository objectRepository,
    ObjectSemanticTypeRepository objectSemanticTypeRepository
  ) {

    this.mimetypeService = mimetypeService;
    this.semanticTypeService = semanticTypeService;

    this.objectRepository = objectRepository;

    this.objectSemanticTypeRepository = objectSemanticTypeRepository;
  }

  /**
   * Create new object in object registry
   *
   * @return objectId
   */
  public Long create(Report report) {
    var objectRecord = createObjectRecordDto(report);

    var objectRecordId = objectRepository.persistRecord(objectRecord, null);

    var semanticTypes = semanticTypeService.detectSemanticTypes(
      objectRecord.mimetype,
      Paths.get(report.getPath())
    );
    objectSemanticTypeRepository.createSemanticTypes(objectRecordId, semanticTypes);

    return objectRecordId;
  }

  /**
   * Update object by path
   *
   * @return objectId
   */
  public Long update(Report report) {
    var id = objectRepository.getObjectIdByPath(report.getPath());
    var objectRecord = createObjectRecordDto(report);
    var objectRecordId = objectRepository.persistRecord(objectRecord, id);
    var semanticTypes = semanticTypeService.detectSemanticTypes(
      objectRecord.mimetype,
      Paths.get(report.getPath())
    );
    objectSemanticTypeRepository.deleteSemanticTypes(objectRecordId);
    objectSemanticTypeRepository.createSemanticTypes(objectRecordId, semanticTypes);

    return objectRecordId;
  }

  public ObjectsRecordDto createObjectRecordDto(Report report) {
    var objectRecord = new ObjectsRecordDto();
    objectRecord.filepath = report.getPath();
    objectRecord.fits = report.getXml();

    objectRecord.format = FitsService.getIdentity(report.getFits()).getFormat();

    objectRecord.timeChanged = LocalDateTime.now();
    objectRecord.timeCreated = LocalDateTime.now();
    objectRecord.userId = report.getUser();
    objectRecord.type = "object";
    objectRecord.deleted = false;

    objectRecord.mimetype = mimetypeService.getMimetype(
      report.getXml(),
      Paths.get(report.getPath())
    );

    return objectRecord;
  }

  public Long updatePath(String oldPath, String path) {
    return objectRepository.updatePath(oldPath, path);
  }

  public Long softDelete(String path) {
    return objectRepository.softDelete(path);
  }
}
