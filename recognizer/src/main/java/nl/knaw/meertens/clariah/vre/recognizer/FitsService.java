package nl.knaw.meertens.clariah.vre.recognizer;

import nl.knaw.meertens.clariah.vre.recognizer.fits.output.Fits;
import nl.knaw.meertens.clariah.vre.recognizer.fits.output.ObjectFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FitsService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private URL fitsUrl;
    private Unmarshaller unmarshaller;

    FitsService(String fitsUrl) {
        try {
            this.fitsUrl = new URL(fitsUrl);
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (MalformedURLException | JAXBException e) {
            e.printStackTrace();
        }
    }

    public Report checkFile(String recognizerPath) throws IOException, JAXBException {
        logger.info("FitsService is checking file: " + recognizerPath);
        String fitsXmlResult = requestFits(recognizerPath);
        Report report = new Report();
        report.setPath(recognizerPath);
        report.setXml(fitsXmlResult);
        report.setFits(unmarshalFits(fitsXmlResult));
        return report;
    }

    private String requestFits(String path) throws IOException {
        String result = "";
        URL url = new URL(fitsUrl, "examine?file=" + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        if (con.getResponseCode() != 200) {
            throw new RuntimeException("FitsService could not parse request [" + url.toString() + "]: " + con.getResponseMessage());
        }
        result = IOUtils.toString(con.getInputStream(), con.getContentEncoding());
        con.disconnect();
        return result;
    }

    public Fits unmarshalFits(String fitsXmlResult) throws JAXBException {
        return (Fits) unmarshaller.unmarshal(new StringReader(fitsXmlResult));
    }

}
