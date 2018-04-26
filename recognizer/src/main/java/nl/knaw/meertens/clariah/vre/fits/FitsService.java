package nl.knaw.meertens.clariah.vre.fits;

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
import java.nio.file.Paths;

public class FitsService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String fitsFilesRoot;
    private URL fitsUrl;
    private Unmarshaller unmarshaller;

    public FitsService(String fitsUrl, String fitsFilesRoot) {
        this.fitsFilesRoot = fitsFilesRoot;
        try {
            this.fitsUrl = new URL(fitsUrl);
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (MalformedURLException | JAXBException e) {
            e.printStackTrace();
        }
    }

    public FitsResult checkFile(String path) throws IOException, JAXBException {
        String fitsPath = Paths
                .get(fitsFilesRoot, path)
                .toString();
        logger.info(String.format("FitsService is checking file [%s]", fitsPath));
        String fitsXmlResult = requestFits(fitsPath);
        FitsResult fitsResult = new FitsResult();
        fitsResult.setXml(fitsXmlResult);
        fitsResult.setFits(unmarshalFits(fitsXmlResult));
        return fitsResult;
    }

    private String requestFits(String path) throws IOException {
        String result = "";
        URL url = new URL(fitsUrl, "examine?file=" + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        if (!hasStatusSuccess(con)) {
            throw new RuntimeException(String.format(
                    "Fits request [%s] resulted in http status [%s] and msg [%s]",
                    url.toString(), con.getResponseCode(), con.getErrorStream()
            ));
        }
        result = IOUtils.toString(con.getInputStream(), con.getContentEncoding());
        con.disconnect();
        return result;
    }

    private boolean hasStatusSuccess(HttpURLConnection con) throws IOException {
        return con.getResponseCode() / 100 == 2;
    }

    public Fits unmarshalFits(String fitsXmlResult) throws JAXBException {
        return (Fits) unmarshaller.unmarshal(new StringReader(fitsXmlResult));
    }

}
