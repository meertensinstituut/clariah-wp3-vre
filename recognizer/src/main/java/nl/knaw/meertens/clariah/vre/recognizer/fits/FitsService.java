package nl.knaw.meertens.clariah.vre.recognizer.fits;

import nl.knaw.meertens.clariah.vre.recognizer.fits.output.Fits;
import nl.knaw.meertens.clariah.vre.recognizer.fits.output.IdentificationType;
import nl.knaw.meertens.clariah.vre.recognizer.fits.output.ObjectFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

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

    public static String getMimeType(Fits fits) {
        return getIdentity(fits).getMimetype();
    }

    public static IdentificationType.Identity getIdentity(Fits fits) {
        return fits.getIdentification().getIdentity().get(0);
    }

    public FitsResult checkFile(String path) throws IOException, JAXBException {
        Path fitsPath = Paths
                .get(fitsFilesRoot, path);
        logger.info(String.format("FitsService is checking file [%s]", fitsPath));

        // loop that checks file
        if(!fitsPath.toFile().exists()) {
            waitUntilFileIsUploaded(fitsPath);
        }

        ls("/"+fitsPath.subpath(0, fitsPath.getNameCount() - 1).toString());

        String fitsXmlResult = askFits(fitsPath.toString());
        FitsResult fitsResult = new FitsResult();
        fitsResult.setXml(fitsXmlResult);
        fitsResult.setFits(unmarshalFits(fitsXmlResult));
        return fitsResult;
    }

    private void waitUntilFileIsUploaded(Path fitsPath) {
        File parentFolder = fitsPath.getParent().toFile();
        while(true) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException("Waiting for uploading was interrupted");
            }
            // check if temporary file still exists:
            File[] tmpUploadFiles = parentFolder.listFiles((dir, name) ->
                    name.contains(fitsPath.getFileName().toString() + ".ocTransferId")
            );
            if(!isNull(tmpUploadFiles) && tmpUploadFiles.length == 1) {
                logger.info("Owncloud still uploading " + fitsPath);
            } else if(fitsPath.toFile().exists()) {
                logger.info("Owncloud finished uploading " + fitsPath + ". Resume recognizing...");
                break;
            } else {
                throw new IllegalArgumentException("Could not find file " + fitsPath);
            }
        }
    }

    private void ls(String path) {
        logger.info("$ ls -al " + path);
        try {
            StringBuilder output = new StringBuilder();
            Process p = Runtime.getRuntime().exec("ls -al " + path);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }
            logger.info(output.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String askFits(String path) throws IOException {
        String result = "";
        URL url = new URL(fitsUrl, "examine?file=" + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        if (!hasStatusSuccess(con)) {
            throw new RuntimeException(String.format(
                    "Fits request [%s] error: [%s][%s]",
                    url.toString(), con.getResponseCode(), IOUtils.toString(con.getErrorStream(), UTF_8)
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
