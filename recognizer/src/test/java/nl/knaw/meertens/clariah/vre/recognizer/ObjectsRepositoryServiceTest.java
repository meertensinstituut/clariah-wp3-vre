package nl.knaw.meertens.clariah.vre.recognizer;

import nl.knaw.meertens.clariah.vre.recognizer.fits.output.Fits;
import nl.knaw.meertens.clariah.vre.recognizer.object.ObjectsRepositoryService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static java.time.LocalDateTime.*;
import static java.time.format.DateTimeFormatter.ofPattern;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.core.StringContains.*;


@RunWith(MockitoJUnitRunner.class)
public class ObjectsRepositoryServiceTest extends AbstractRecognizerTest {

    private ObjectsRepositoryService objectsRepositoryService;

    @Before
    public void setup() {
        objectsRepositoryService = new ObjectsRepositoryService("", "", Config.OBJECT_TABLE);
    }

    @Test
    public void testCreateJsonCreatesJson() throws Exception {
        Fits fits = fitsService.unmarshalFits(testFitsXml);
        Report report = new Report();
        report.setFits(fits);
        String expectedPath = "/some/path.txt";
        report.setPath(expectedPath);
        String expectedUser = "some-user";
        report.setUser(expectedUser);
        report.setXml(testFitsXml);

        String result = objectsRepositoryService.createObjectRecordJson(report);

        assertThatJson(result).node("timecreated").matches(
                containsString(now().format(ofPattern("yyyy-MM-dd'T'HH"))));
        assertThatJson(result).node("timechanged").matches(
                containsString(now().format(ofPattern("yyyy-MM-dd'T'HH"))));
        assertThatJson(result).node("user_id").isEqualTo(expectedUser);
        assertThatJson(result).node("filepath").isEqualTo(expectedPath);
        assertThatJson(result).node("mimetype").isEqualTo("text/plain");
        assertThatJson(result).node("format").isEqualTo("Plain text");
    }

}