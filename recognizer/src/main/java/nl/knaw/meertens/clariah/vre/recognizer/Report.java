package nl.knaw.meertens.clariah.vre.recognizer;

import nl.knaw.meertens.clariah.vre.recognizer.fits.output.Fits;

import java.nio.file.Paths;

public class Report {
    private Fits fits;
    private String xml;
    private String path;
    private String user;
    private Long objectId;

    public Fits getFits() {
        return fits;
    }

    public void setFits(Fits fits) {
        this.fits = fits;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = Paths.get(path).normalize().toString();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public Long getObjectId() {
        return objectId;
    }
}
