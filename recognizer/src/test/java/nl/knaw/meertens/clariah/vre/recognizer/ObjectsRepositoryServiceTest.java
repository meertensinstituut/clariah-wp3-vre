package nl.knaw.meertens.clariah.vre.recognizer;

import nl.knaw.meertens.clariah.vre.recognizer.fits.output.Fits;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.LocalDateTime.*;
import static java.time.format.DateTimeFormatter.ofPattern;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.core.StringContains.*;


@RunWith(MockitoJUnitRunner.class)
public class ObjectsRepositoryServiceTest extends AbstractRecognizerTest {

    private ObjectsRepositoryService objectsRepositoryService;

    @Before
    public void setup() {
        objectsRepositoryService = new ObjectsRepositoryService("", "");
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

        String result = objectsRepositoryService.createJson(report);

        assertThatJson(result).node("resource").isPresent();
        assertThatJson(result).node("resource[0].timecreated").matches(
                containsString(now().format(ofPattern("yyyy-MM-dd'T'HH"))));
        assertThatJson(result).node("resource[0].timechanged").matches(
                containsString(now().format(ofPattern("yyyy-MM-dd'T'HH"))));
        assertThatJson(result).node("resource[0].user_id").isEqualTo(expectedUser);
        assertThatJson(result).node("resource[0].filepath").isEqualTo(expectedPath);
        assertThatJson(result).node("resource[0].mimetype").isEqualTo("text/plain");
        assertThatJson(result).node("resource[0].format").isEqualTo("Plain text");
    }

}