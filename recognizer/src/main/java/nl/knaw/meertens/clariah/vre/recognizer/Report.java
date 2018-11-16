package nl.knaw.meertens.clariah.vre.recognizer;

import nl.knaw.meertens.clariah.vre.recognizer.fits.output.Fits;

public class Report {
  private Fits fits;
  private String xml;
  private String path;
  private String user;
  private Long objectId;
  private String oldPath;
  private String action;

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
    this.path = path;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public Long getObjectId() {
    return objectId;
  }

  public void setObjectId(Long objectId) {
    this.objectId = objectId;
  }

  public String getOldPath() {
    return oldPath;
  }

  public void setOldPath(String oldPath) {
    this.oldPath = oldPath;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }
}
