package nl.knaw.meertens.clariah.vre.recognizer;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static nl.knaw.meertens.clariah.vre.recognizer.Config.FITS_MIMETYPES_RESOURCE;
import static nl.mpi.tla.util.Saxon.buildDocument;
import static nl.mpi.tla.util.Saxon.xpath2boolean;
import static nl.mpi.tla.util.Saxon.xpath2string;
import static nl.mpi.tla.util.Saxon.xpathList;

/**
 * Determine mimetype using:
 * - fits xml report
 * - original file
 * Rules to assert mimetypes are defined in:
 * fits-mimetypes.xml
 */
public class MimetypeService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Map<String, String> namespaces = new LinkedHashMap<>();

  private final List<XdmItem> mimetypes;

  public MimetypeService() {
    this.mimetypes = getMimetypesFromResources();

    namespaces.put("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
    namespaces.put("sx", "java:nl.mpi.tla.saxon");
    namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");

  }

  public String getMimetype(String fits, String originalFile) {
    try {
      var fitsDoc = buildDocument(new StreamSource(new StringReader(fits)));
      var xpathVars = new HashMap<String, XdmValue>();
      xpathVars.put("file", new XdmAtomicValue(FitsPath.of(originalFile).toString()));

      for (var mimetype : mimetypes) {
        var mimetypeValue = xpath2string(mimetype, "normalize-space(@value)");
        xpathVars.put("mime", new XdmAtomicValue(mimetypeValue));
        if (isMimetype(fitsDoc, mimetype, xpathVars)) {
          return mimetypeValue;
        }
      }
      return getMimetypeFoundByFits(fitsDoc);
    } catch (SaxonApiException e) {
      throw new RuntimeException("Could not determine mimetype", e);
    }
  }

  public List<XdmItem> getMimetypesFromResources() {
    var filename = FITS_MIMETYPES_RESOURCE;
    try {
      return xpathList(buildDocument(new StreamSource(Thread
        .currentThread()
        .getContextClassLoader()
        .getResourceAsStream(filename)
      )), "/mimetypes/mimetype", null, namespaces);
    } catch (SaxonApiException e) {
      throw new RuntimeException(format("Could not load [%s]", filename), e);
    }

  }

  public Map<String, String> getNamespaces() {
    return namespaces;
  }

  private String getMimetypeFoundByFits(XdmNode fitsDoc) throws SaxonApiException {
    return xpath2string(fitsDoc, "/fits:fits/fits:identification/fits:identity[1]/@mimetype", null, namespaces);
  }

  private boolean isMimetype(
    XdmNode fitsDoc,
    XdmItem mimetype,
    HashMap<String, XdmValue> xpathVars
  ) throws SaxonApiException {
    if (checkMimetypeXpath(fitsDoc, mimetype, xpathVars)) {
      return false;
    }

    if (!xpath2boolean(mimetype, "exists(assertions)")) {
      return false;
    }

    // mimetype contains multiple <assertions/> which contain multiple <assertion/>
    var assertionListList = xpathList(mimetype, "assertions", null, namespaces);

    for (var assertionList : assertionListList) {
      if (checkAssertions(fitsDoc, assertionList, xpathVars)) {
        return true;
      }
    }

    return false;
  }

  private boolean checkMimetypeXpath(
    XdmNode fitsDoc,
    XdmItem mimetype,
    HashMap<String, XdmValue> xpathVars
  ) throws SaxonApiException {
    var mimetypeXpath = xpath2string(mimetype, "normalize-space(@xpath)");
    if (mimetypeXpath.isEmpty()) {
      throw new RuntimeException(String.format("No xpath available in mimetype [%s]", mimetype));
    }
    return !isValidDoc(fitsDoc, mimetypeXpath, xpathVars);
  }

  private boolean checkAssertions(
    XdmNode fitsDoc,
    XdmItem assertionsListNode,
    HashMap<String, XdmValue> xpathVars
  ) throws SaxonApiException {
    var assertionList = xpathList(assertionsListNode, "assertion", null, namespaces);
    for (var assertion : assertionList) {
      var assertionXpath = xpath2string(assertion, "normalize-space(@xpath)");
      if (assertionXpath.isEmpty()) {
        throw new RuntimeException(format("No xpath available in assertion [%s]", assertion));
      }
      if (!isValidDoc(fitsDoc, assertionXpath, xpathVars)) {
        return false;
      }
    }
    return true;
  }

  private boolean isValidDoc(
    XdmNode doc,
    String assertionXpath,
    HashMap<String, XdmValue> vars
  ) throws SaxonApiException {
    return xpath2boolean(doc, assertionXpath, vars, namespaces);
  }

}
