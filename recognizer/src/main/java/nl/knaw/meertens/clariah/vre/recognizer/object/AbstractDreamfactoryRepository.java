package nl.knaw.meertens.clariah.vre.recognizer.object;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class AbstractDreamfactoryRepository {

  final String objectsDbUrl;
  final String objectsDbKey;
  final ParseContext jsonPath;
  final ObjectMapper mapper;

  AbstractDreamfactoryRepository(String objectsDbUrl, String objectsDbKey, ObjectMapper mapper) {
    this.objectsDbUrl = objectsDbUrl;
    this.objectsDbKey = objectsDbKey;

    this.mapper = mapper;

    var conf = Configuration
      .builder()
      .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
      .options(Option.SUPPRESS_EXCEPTIONS)
      .build();
    jsonPath = JsonPath.using(conf);
  }

  /**
   * Source: technicaladvices.com/2012/02/20/java-encoding-similiar-to-javascript-encodeuricomponent/
   */
  String encodeUriComponent(String filter) {
    try {
      return URLEncoder
        .encode(filter, "UTF-8")
        .replaceAll("\\%28", "(")
        .replaceAll("\\%29", ")")
        .replaceAll("\\+", "%20")
        .replaceAll("\\%27", "'")
        .replaceAll("\\%21", "!")
        .replaceAll("\\%7E", "~");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(String.format(
        "Could not create url from filter [%s]", filter
      ));
    }
  }


}
