package nl.knaw.meertens.clariah.vre.switchboard.param;

import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;

public class ParamService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DocumentBuilder builder;
    private ServicesRegistryService servicesRegistryService;

    public ParamService(ServicesRegistryService servicesRegistryService) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Could not create xml document builder during initialization", e);
        }
        this.servicesRegistryService = servicesRegistryService;
    }

    public CmdiDto getCmdi(long serviceId) {
        String semanticsXml = servicesRegistryService.getServiceSemantics(serviceId);
        CmdiDto result = convertCmdiXmlToDto(semanticsXml);
        return result;
    }

    private CmdiDto convertCmdiXmlToDto(String cmdi) {
        logger.info("cmdi xml: " + cmdi);
        try {
            CmdiDto result = new CmdiDto();
            InputSource inputSource = new InputSource(new StringReader(cmdi));
            Document xml = builder.parse(inputSource);

            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "//*[local-name() = 'Input'] / *[local-name() = 'Parameter']";
            NodeList parameters = (NodeList) xPath.compile(expression).evaluate(xml, XPathConstants.NODESET);

            if (parameters == null || parameters.getLength() == 0) {
                logger.warn(String.format("No parametergroups found in cmdi [%s]", cmdi));
            } else {
                result.params.addAll(parseParameters(parameters));
            }

            return result;
        } catch (SAXException | IOException | XPathExpressionException e) {
            return handleException(e, String.format("Could not parse cmdi xml [%s]", cmdi));
        }
    }

    private List<ParamDto> parseParameters(NodeList parameters) {
        List<ParamDto> result = new ArrayList<>();
        for (int i = 0; i < parameters.getLength(); i++) {
            Node xmlParam = parameters.item(i);
            ParamDto param = new ParamDto();
            NodeList xmlValues = xmlParam.getChildNodes();
            for (int k = 0; k < xmlValues.getLength(); k++) {
                Node field = xmlValues.item(k);
                String fieldName = field.getNodeName();
                String fieldValue = field.getTextContent();
                if(isNull(field.getNodeName())) {
                    continue;
                }
                switch (fieldName) {
                    case "cmdp:Name":
                        param.name = fieldValue;
                        break;
                    case "cmdp:Label":
                        param.label = fieldValue;
                        break;
                    case "cmdp:DataType":
                        param.type = ParamType.fromString(fieldValue);
                        break;
                    case "cmdp:Description":
                        param.description = fieldValue;
                        break;
                    case "cmdp:MinimumCardinality":
                        param.minimumCardinality = fieldValue;
                        break;
                    case "cmdp:MaximumCardinality":
                        param.maximumCardinality = fieldValue;
                        break;
                }
            }
            result.add(param);
        }
        return result;
    }

}
