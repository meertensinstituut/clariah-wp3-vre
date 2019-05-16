package nl.knaw.meertens.clariah.vre.recognizer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import nl.knaw.meertens.clariah.vre.recognizer.semantics.SemanticTypePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;
import static nl.mpi.tla.util.Saxon.xpath2string;
import static org.apache.commons.lang3.StringUtils.isBlank;

// TODO:
// - create table for semantic types
// - save semantic type records
// - delete semantic type records on update
// - test creation of semantic types (request)
// - test deletion of semantic types on update(request)

/**
 * Detect semantic types which are more specific then mimetypes
 * since some services have additional requirements
 * that cannot be determined by mimetype alone
 */
public class SemanticTypeService {

  private final HashMap<String, SemanticTypePlugin> semanticTypePlugins;
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public SemanticTypeService(MimetypeService mimetypeService) {
    this.semanticTypePlugins = this.getPlugins(mimetypeService.getMimetypesFromResources());
  }

  public List<String> detectSemanticTypes(String mimetype, Path originalFile) {
    var semanticTypePlugin = semanticTypePlugins
      .get(mimetype);
    if (semanticTypePlugin == null) {
      return new ArrayList<>();
    }
    return semanticTypePlugin
      .detect(originalFile);
  }

  private HashMap<String, SemanticTypePlugin> getPlugins(List<XdmItem> mimetypeNodes) {
    HashMap<String, SemanticTypePlugin> result = new HashMap<>();
    for (var mimetypeNode : mimetypeNodes) {
      logger.info("mimetype: " + mimetypeNode.getStringValue());
      try {
        var mimetype = xpath2string(mimetypeNode, "//mimetype/@value");
        var className = xpath2string(mimetypeNode, "//mimetype/semantics/@class");
        if (!isBlank(className)) {
          var semanticTypePlugin = Class.forName(className).asSubclass(SemanticTypePlugin.class);
          var pluginInstance = intantiateClass(semanticTypePlugin);
          result.put(mimetype, pluginInstance);
        }
      } catch (SaxonApiException | ClassNotFoundException ex) {
        logger.error(format("Cannot determine semantic type plugin for [%s]", mimetypeNode), ex);
      }
    }

    return result;
  }

  private SemanticTypePlugin intantiateClass(Class<? extends SemanticTypePlugin> loadedClass) {
    Class<? extends SemanticTypePlugin> pluginClass = loadedClass.asSubclass(SemanticTypePlugin.class);
    SemanticTypePlugin plugin = null;
    try {
      plugin = pluginClass.getDeclaredConstructor().newInstance();
    } catch (InstantiationException |
      IllegalAccessException |
      InvocationTargetException |
      NoSuchMethodException e
    ) {
      logger.error(String.format("Cannot find constructor of [%s]", loadedClass));
    }
    return plugin;
  }
}
