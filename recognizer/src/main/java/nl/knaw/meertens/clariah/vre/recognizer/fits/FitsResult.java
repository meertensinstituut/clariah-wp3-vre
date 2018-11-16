package nl.knaw.meertens.clariah.vre.recognizer.fits;

import nl.knaw.meertens.clariah.vre.recognizer.fits.output.Fits;

public class FitsResult {
  private String xml;
  private Fits fits;

  public String getXml() {
    return xml;
  }

  public void setXml(String xml) {
    this.xml = xml;
  }

  public Fits getFits() {
    return fits;
  }

  public void setFits(Fits fits) {
    this.fits = fits;
  }
}
