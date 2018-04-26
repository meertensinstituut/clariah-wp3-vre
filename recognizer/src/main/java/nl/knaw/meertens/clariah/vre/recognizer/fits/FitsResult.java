package nl.knaw.meertens.clariah.vre.recognizer.fits;

import nl.knaw.meertens.clariah.vre.recognizer.fits.output.Fits;

public class FitsResult {
    private String xml;
    private Fits fits;

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getXml() {
        return xml;
    }

    public void setFits(Fits fits) {
        this.fits = fits;
    }

    public Fits getFits() {
        return fits;
    }
}
