package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.recognizer.fits.FitsService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.OwncloudKafkaDto;
import nl.knaw.meertens.clariah.vre.recognizer.object.ObjectsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.ACTIONS_TO_PERSIST;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.CREATE;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.DELETE;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.RENAME;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.UPDATE;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class RecognizerService {

  private final ObjectMapper objectMapper;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private KafkaConsumerService nextcloudConsumerService;
  private ObjectsService objectsService;
  private MimetypeService mimetypeService;
  private FitsService fitsService;
  private KafkaProducerService kafkaProducerService;

  public RecognizerService(
    ObjectMapper objectMapper,
    KafkaConsumerService nextcloudConsumerService,
    ObjectsService objectsService,
    MimetypeService mimetypeService,
    FitsService fitsService,
    KafkaProducerService kafkaProducerService
  ) {
    this.objectMapper = objectMapper;
    this.nextcloudConsumerService = nextcloudConsumerService;
    this.objectsService = objectsService;
    this.mimetypeService = mimetypeService;
    this.fitsService = fitsService;
    this.kafkaProducerService = kafkaProducerService;
  }

  public void consumeOwncloud() {
    nextcloudConsumerService.consumeWith((String json) -> {
      try {
        var msg = objectMapper.readValue(json, OwncloudKafkaDto.class);
        var action = FileAction.from(msg.action);
        if (!ACTIONS_TO_PERSIST.contains(msg.action)) {
          logger.info(format(
            "Ignored message about file [%s] with action [%s]",
            msg.path, msg.action
          ));
          return;
        }
        if (isBlank(msg.path)) {
          throw new IllegalArgumentException(String.format(
            "No field path in nextcloud msg [%s]", json
          ));
        }
        var report = mapToReport(msg);
        handleFileActions(action, report);
        kafkaProducerService.produceToRecognizerTopic(report);
      } catch (Exception e) {
        logger.error(String.format("Could not process kafka message [%s]", json), e);
      }
    });
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  private Report mapToReport(OwncloudKafkaDto msg) {
    var report = new Report();
    report.setAction(msg.action);
    report.setUser(msg.user);
    report.setPath(Paths.get(msg.path).normalize().toString());
    report.setOldPath(isNull(msg.oldPath) ? null : Paths.get(msg.oldPath).normalize().toString());
    return report;
  }

  private void handleFileActions(
    FileAction action,
    Report report
  ) throws IOException, JAXBException {
    if (action.equals(CREATE)) {
      requestFileType(report);
      checkFileType(report);
      report.setObjectId(objectsService.create(report));
    } else if (action.equals(UPDATE)) {
      requestFileType(report);
      checkFileType(report);
      report.setObjectId(objectsService.update(report));
    } else if (action.equals(RENAME)) {
      report.setObjectId(objectsService.updatePath(report.getOldPath(), report.getPath()));
    } else if (action.equals(DELETE)) {
      report.setObjectId(objectsService.softDelete(report.getPath()));
    }
  }

  private void checkFileType(Report report) throws IllegalArgumentException {
    if (mimetypeService.getMimetype(report.getXml(), Path.of(report.getPath())).equals("inode/directory")) {
      throw new IllegalArgumentException("File is a directory");
    }
  }

  private void requestFileType(
    Report report
  ) throws IOException, JAXBException {
    var fitsResult = fitsService.checkFile(report.getPath());
    report.setXml(fitsResult.getXml());
    report.setFits(fitsResult.getFits());
  }

}
