package nl.knaw.meertens.clariah.vre.recognizer.fits;

import nl.knaw.meertens.clariah.vre.recognizer.generated.fits.output.Fits;
import nl.knaw.meertens.clariah.vre.recognizer.generated.fits.output.IdentificationType;
import nl.knaw.meertens.clariah.vre.recognizer.generated.fits.output.ObjectFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBContextFactory;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
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
      var jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
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
    var fitsPath = Paths.get(fitsFilesRoot, path);

    if (!fitsPath.toFile().exists()) {
      waitUntilFileIsUploaded(fitsPath);
    }

    var fitsXmlResult = askFits(fitsPath.toString());
    var fitsResult = new FitsResult();
    fitsResult.setXml(fitsXmlResult);
    fitsResult.setFits(unmarshalFits(fitsXmlResult));
    return fitsResult;
  }

  private void waitUntilFileIsUploaded(Path fitsPath) {
    var parentFolder = fitsPath.getParent().toFile();
    while (true) {
      try {
        TimeUnit.MILLISECONDS.sleep(250);
      } catch (InterruptedException e) {
        throw new RuntimeException("Waiting for file upload was interrupted");
      }
      var fileIsBeingUploaded = parentFolder.listFiles((dir, name) ->
        name.contains(fitsPath.getFileName().toString() + ".ocTransferId")
      );
      if (!isNull(fileIsBeingUploaded) && fileIsBeingUploaded.length == 1) {
        logger.info("Nextcloud is still uploading " + fitsPath);
      } else if (fitsPath.toFile().exists()) {
        logger.info("Nextcloud finished uploading " + fitsPath + ". Resume recognizing...");
        break;
      } else {
        throw new IllegalArgumentException("Could not find file " + fitsPath);
      }
    }
  }

  private String askFits(String path) throws IOException {
    var result = "";
    var url = new URL(fitsUrl, "examine?file=" + path);
    var con = (HttpURLConnection) url.openConnection();
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
    var reader = new StringReader(fitsXmlResult);
    System.out.print("unmarshaller:" + unmarshaller);
    var unmarshal = unmarshaller.unmarshal(reader);
    return (Fits) unmarshal;
  }

}
