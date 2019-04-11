package nl.knaw.meertens.clariah.vre.recognizer.fits;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * - custom checks on the original file
 */
public class FitsMimetypeService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Map<String, String> namespaces = new LinkedHashMap<>();
  private final XdmNode mimeTypes;

  FitsMimetypeService() {
    this.mimeTypes = readFitsMimetypesResource();

    namespaces.put("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
    namespaces.put("sx", "java:nl.mpi.tla.saxon");
    namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");

  }

  String determineFitsMimeType(String fits, Path originalFile) {
    try {
      var fitsDoc = buildDocument(new StreamSource(new StringReader(fits)));
      var mimetypes = xpathList(this.mimeTypes, "/mimetypes/mimetype", null, namespaces);

      var xpathVars = new HashMap<String, XdmValue>();
      xpathVars.put("file", new XdmAtomicValue(originalFile.toString()));

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

  private String getMimetypeFoundByFits(XdmNode fitsDoc) throws SaxonApiException {
    return xpath2string(fitsDoc, "/fits/identification/identity/@mimetype");
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
      if (checkAssertions(fitsDoc, assertionList)) {
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
    if (mimetypeXpath.equals("")) {
      throw new RuntimeException("No xpath available in mimetype");
    }
    return !xpath2boolean(fitsDoc, mimetypeXpath, xpathVars, namespaces);
  }

  private boolean checkAssertions(
    XdmNode fitsDoc,
    XdmItem assertionsListNode
  ) throws SaxonApiException {
    var assertionList = xpathList(assertionsListNode, "assert", null, namespaces);
    for (var assertion : assertionList) {
      var assertionXpath = xpath2string(assertion, "normalize-space(@xpath)");
      if (assertionXpath.isEmpty()) {
        throw new RuntimeException("mismatch between fits report and mimetype rules");
      }
      if (!xpath2boolean(fitsDoc, assertionXpath, null, namespaces)) {
        return false;
      }
    }
    return true;
  }

  private XdmNode readFitsMimetypesResource() {
    var filename = FITS_MIMETYPES_RESOURCE;
    try {
      return buildDocument(new StreamSource(Thread
        .currentThread()
        .getContextClassLoader()
        .getResourceAsStream(filename)
      ));
    } catch (SaxonApiException e) {
      throw new RuntimeException(format("Could not load [%s]", filename), e);
    }

  }
}
