package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.meertens.clariah.vre.recognizer.fits.FitsResult;
import nl.knaw.meertens.clariah.vre.recognizer.fits.FitsService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.KafkaConsumerService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.KafkaProducerService;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.OwncloudKafkaDTO;
import nl.knaw.meertens.clariah.vre.recognizer.kafka.RecognizerKafkaProducer;
import nl.knaw.meertens.clariah.vre.recognizer.object.ObjectsRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Paths;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.ACTIONS_TO_PERSIST;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_FILES_ROOT;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_URL;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.KAFKA_SERVER;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.NEXTCLOUD_GROUP_NAME;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.NEXTCLOUD_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_KEY;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECTS_DB_URL;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.OBJECT_TABLE;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.RECOGNIZER_TOPIC_NAME;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.CREATE;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.DELETE;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.RENAME;
import static nl.knaw.meertens.clariah.vre.recognizer.FileAction.UPDATE;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class RecognizerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KafkaConsumerService nextcloudConsumerService = new KafkaConsumerService(
            KAFKA_SERVER,
            NEXTCLOUD_TOPIC_NAME,
            NEXTCLOUD_GROUP_NAME
    );
    private final KafkaProducerService kafkaProducer = new KafkaProducerService(
            new RecognizerKafkaProducer(KAFKA_SERVER),
            RECOGNIZER_TOPIC_NAME
    );
    private final ObjectsRepositoryService objectsRepository = new ObjectsRepositoryService(
            OBJECTS_DB_URL,
            OBJECTS_DB_KEY,
            OBJECT_TABLE
    );
    private final FitsService fitsService = new FitsService(
            FITS_URL,
            FITS_FILES_ROOT
    );

    public void consumeOwncloud() {
        nextcloudConsumerService.consumeWith((String json) -> {
            try {
                OwncloudKafkaDTO msg = objectMapper.readValue(json, OwncloudKafkaDTO.class);
                FileAction action = FileAction.from(msg.action);
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
                Report report = mapToReport(msg);
                handleFileActions(action, report);
                kafkaProducer.produceToRecognizerTopic(report);
            } catch (Exception e) {
                logger.error(String.format("Could not process kafka message [%s]", json), e);
            }
        });
    }

    private Report mapToReport(OwncloudKafkaDTO msg) {
        Report report = new Report();
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
            report.setObjectId(objectsRepository.create(report));
        } else if (action.equals(UPDATE)) {
            requestFileType(report);
            checkFileType(report);
            report.setObjectId(objectsRepository.update(report));
        } else if (action.equals(RENAME)) {
            report.setObjectId(objectsRepository.updatePath(report.getOldPath(), report.getPath()));
        } else if (action.equals(DELETE)) {
            report.setObjectId(objectsRepository.softDelete(report.getPath()));
        }
    }

    private void checkFileType(Report report) throws IllegalArgumentException {
        // TODO: use rule lib of menzo
        if (FitsService.getMimeType(report.getFits()).equals("inode/directory")) {
            throw new IllegalArgumentException("File is a directory");
        }
    }

    private void requestFileType(
            Report report
    ) throws IOException, JAXBException {
        FitsResult fitsResult = fitsService.checkFile(report.getPath());
        report.setXml(fitsResult.getXml());
        report.setFits(fitsResult.getFits());
    }


}
