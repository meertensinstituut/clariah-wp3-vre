package nl.knaw.meertens.clariah.vre.recognizer;

import nl.knaw.meertens.clariah.vre.recognizer.fits.output.Fits;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KafkaProducerServiceTest extends AbstractRecognizerTest {

    private KafkaProducerService underTest;

    @Mock
    private RecognizerKafkaProducer mockRecognizerProducer;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, String>> captor;

    @Mock
    private KafkaProducer<String, String> mockKafkaProducer;

    @Before
    public void setup() {
        underTest = new KafkaProducerService(mockRecognizerProducer, "testtopic");
    }

    @Test
    public void testProduceCreatesCorrectRecognizerKafkaMsg() throws Exception {
        when(mockRecognizerProducer.getKafkaProducer()).thenReturn(mockKafkaProducer);

        Fits fits = fitsService.unmarshalFits(testFitsXml);
        Report report = new Report();
        report.setFits(fits);
        String expectedPath = "/some/path/to/x.txt";
        report.setObjectId(15L);
        report.setPath(expectedPath);
        report.setXml(testFitsXml);

        underTest.produceToRecognizerTopic(report);

        verify(mockKafkaProducer).send(captor.capture());
        ProducerRecord<String, String> result = captor.getValue();
        assertThat(result.value()).isEqualToIgnoringWhitespace(
                "{" +
                "  \"objectId\": 15, " +
                "  \"path\": \"" + expectedPath + "\", " +
                "  \"fitsFormat\": \"Plain text\", " +
                "  \"fitsMimetype\": \"text/plain\", " +
                "  \"fitsFullResult\": \"<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?>\\n<fits xmlns=\\\"http://hul.harvard.edu/ois/xml/ns/fits/fits_output\\\" xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\" xsi:schemaLocation=\\\"http://hul.harvard.edu/ois/xml/ns/fits/fits_output http://hul.harvard.edu/ois/xml/xsd/fits/fits_output.xsd\\\" version=\\\"1.2.0\\\" timestamp=\\\"1/19/18 1:04 PM\\\">\\n  <identification>\\n    <identity format=\\\"Plain text\\\" mimetype=\\\"text/plain\\\" toolname=\\\"FITS\\\" toolversion=\\\"1.2.0\\\">\\n      <tool toolname=\\\"Droid\\\" toolversion=\\\"6.3\\\" />\\n      <tool toolname=\\\"Jhove\\\" toolversion=\\\"1.16\\\" />\\n      <tool toolname=\\\"file utility\\\" toolversion=\\\"5.22\\\" />\\n      <externalIdentifier toolname=\\\"Droid\\\" toolversion=\\\"6.3\\\" type=\\\"puid\\\">x-fmt/111</externalIdentifier>\\n    </identity>\\n  </identification>\\n  <fileinfo>\\n    <size toolname=\\\"Jhove\\\" toolversion=\\\"1.16\\\">10</size>\\n    <filepath toolname=\\\"OIS File Information\\\" toolversion=\\\"0.2\\\" status=\\\"SINGLE_RESULT\\\">/tmp/recognizer/test.txt</filepath>\\n    <filename toolname=\\\"OIS File Information\\\" toolversion=\\\"0.2\\\" status=\\\"SINGLE_RESULT\\\">test.txt</filename>\\n    <md5checksum toolname=\\\"OIS File Information\\\" toolversion=\\\"0.2\\\" status=\\\"SINGLE_RESULT\\\">b05403212c66bdc8ccc597fedf6cd5fe</md5checksum>\\n    <fslastmodified toolname=\\\"OIS File Information\\\" toolversion=\\\"0.2\\\" status=\\\"SINGLE_RESULT\\\">1516357453000</fslastmodified>\\n  </fileinfo>\\n  <filestatus>\\n    <well-formed toolname=\\\"Jhove\\\" toolversion=\\\"1.16\\\" status=\\\"SINGLE_RESULT\\\">true</well-formed>\\n    <valid toolname=\\\"Jhove\\\" toolversion=\\\"1.16\\\" status=\\\"SINGLE_RESULT\\\">true</valid>\\n  </filestatus>\\n  <metadata>\\n    <text>\\n      <linebreak toolname=\\\"Jhove\\\" toolversion=\\\"1.16\\\" status=\\\"SINGLE_RESULT\\\">LF</linebreak>\\n      <charset toolname=\\\"Jhove\\\" toolversion=\\\"1.16\\\">US-ASCII</charset>\\n      <standard>\\n        <textMD:textMD xmlns:textMD=\\\"info:lc/xmlns/textMD-v3\\\">\\n          <textMD:character_info>\\n            <textMD:charset>US-ASCII</textMD:charset>\\n            <textMD:linebreak>LF</textMD:linebreak>\\n          </textMD:character_info>\\n        </textMD:textMD>\\n      </standard>\\n    </text>\\n  </metadata>\\n  <statistics fitsExecutionTime=\\\"128\\\">\\n    <tool toolname=\\\"MediaInfo\\\" toolversion=\\\"0.7.75\\\" status=\\\"did not run\\\" />\\n    <tool toolname=\\\"OIS Audio Information\\\" toolversion=\\\"0.1\\\" status=\\\"did not run\\\" />\\n    <tool toolname=\\\"ADL Tool\\\" toolversion=\\\"0.1\\\" status=\\\"did not run\\\" />\\n    <tool toolname=\\\"VTT Tool\\\" toolversion=\\\"0.1\\\" status=\\\"did not run\\\" />\\n    <tool toolname=\\\"Droid\\\" toolversion=\\\"6.3\\\" executionTime=\\\"8\\\" />\\n    <tool toolname=\\\"Jhove\\\" toolversion=\\\"1.16\\\" executionTime=\\\"109\\\" />\\n    <tool toolname=\\\"file utility\\\" toolversion=\\\"5.22\\\" executionTime=\\\"116\\\" />\\n    <tool toolname=\\\"Exiftool\\\" toolversion=\\\"10.00\\\" status=\\\"did not run\\\" />\\n    <tool toolname=\\\"NLNZ Metadata Extractor\\\" toolversion=\\\"3.6GA\\\" status=\\\"did not run\\\" />\\n    <tool toolname=\\\"OIS File Information\\\" toolversion=\\\"0.2\\\" executionTime=\\\"30\\\" />\\n    <tool toolname=\\\"OIS XML Metadata\\\" toolversion=\\\"0.2\\\" status=\\\"did not run\\\" />\\n    <tool toolname=\\\"ffident\\\" toolversion=\\\"0.2\\\" executionTime=\\\"34\\\" />\\n    <tool toolname=\\\"Tika\\\" toolversion=\\\"1.10\\\" executionTime=\\\"32\\\" />\\n  </statistics>\\n</fits>\\n\"" +
                "}"
        );

    }

}