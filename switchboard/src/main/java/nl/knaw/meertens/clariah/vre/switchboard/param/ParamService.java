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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static nl.knaw.meertens.clariah.vre.switchboard.exception.ExceptionHandler.handleException;

public class ParamService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DocumentBuilder builder;
    private ServicesRegistryService servicesRegistryService;

    private String currentLanguage;

    public ParamService(ServicesRegistryService servicesRegistryService) {
        currentLanguage = "en";

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
        return convertCmdiXmlToDto(semanticsXml);
    }

    private CmdiDto convertCmdiXmlToDto(String cmdi) {
        try {
            CmdiDto result = new CmdiDto();
            InputSource inputSource = new InputSource(new StringReader(cmdi));
            Document xml = builder.parse(inputSource);

            XPath xPath = XPathFactory.newInstance().newXPath();
            String paramExpression = "//*[local-name() = 'Input'] / *[local-name() = 'Parameter']";
            NodeList parameters = (NodeList) xPath.compile(paramExpression).evaluate(xml, XPathConstants.NODESET);
            if (parameters == null || parameters.getLength() == 0) {
                logger.warn(String.format("No parameters found in cmdi [%s]", cmdi));
            } else {
                result.params.addAll(mapParameters(parameters));
            }

            String groupExpression = "//*[local-name() = 'Input'] / *[local-name() = 'ParameterGroup']";
            NodeList groups = (NodeList) xPath.compile(groupExpression).evaluate(xml, XPathConstants.NODESET);
            if (groups == null || groups.getLength() == 0) {
                logger.warn(String.format("No parameter groups found in cmdi [%s]", cmdi));
            } else {
                result.paramGroups.addAll(mapParameterGroups(groups));
            }

            return result;
        } catch (SAXException | IOException | XPathExpressionException e) {
            return handleException(e, String.format("Could not parse cmdi xml [%s]", cmdi));
        }
    }

    private List<ParamGroupDto> mapParameterGroups(NodeList groups) {
        List<ParamGroupDto> result = new ArrayList<>();
        for (int i = 0; i < groups.getLength(); i++) {

        }
        return result;

    }

    private List<ParamDto> mapParameters(NodeList parameters) {
        List<ParamDto> result = new ArrayList<>();
        for (int i = 0; i < parameters.getLength(); i++) {
            Node xmlParam = parameters.item(i);
            ParamDto param = mapParameter(xmlParam);
            result.add(param);
        }
        return result;
    }

    private ParamDto mapParameter(Node xmlParam) {
        ParamDto param = new ParamDto();
        NodeList xmlValues = xmlParam.getChildNodes();
        for (int k = 0; k < xmlValues.getLength(); k++) {
            Node node = xmlValues.item(k);
            String fieldName = node.getNodeName();
            if (isNull(fieldName)) {
                continue;
            }
            String fieldValue = node.getTextContent();
            switch (fieldName) {
                case "cmdp:Name":
                    param.name = fieldValue;
                    break;
                case "cmdp:Label":
                    String labelLanguage = node.getAttributes().getNamedItem("xml:lang").getNodeValue();
                    if (labelLanguage.equals(currentLanguage)) {
                        param.label = fieldValue;
                    }
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
                case "cmdp:Values":
                    param.values.addAll(mapParamValues(node));
                    break;
            }

            // Enumeration type is determined by the presence of values
            // and should overwrite DataType:
            if (!param.values.isEmpty()) {
                param.valuesType = param.type;
                param.type = ParamType.ENUMERATION;
            }
        }
        return param;
    }

    private List<ParamValueDto> mapParamValues(Node values) {
        List<ParamValueDto> result = new ArrayList<>();
        for (int i = 0; i < values.getChildNodes().getLength(); i++) {
            Node child = values.getChildNodes().item(i);
            if (child.getNodeName().equals("cmdp:ParameterValue")) {
                result.add(mapParamValue(child));
            }
        }
        return result;
    }

    private ParamValueDto mapParamValue(Node paramValue) {
        ParamValueDto result = new ParamValueDto();
        for (int i = 0; i < paramValue.getChildNodes().getLength(); i++) {
            Node value = paramValue.getChildNodes().item(i);
            String name = value.getNodeName();
            String textValue = value.getTextContent();
            switch (name) {
                case "cmdp:Label":
                    String labelLanguage = value.getAttributes().getNamedItem("xml:lang").getNodeValue();
                    if (labelLanguage.equals(currentLanguage)) {
                        result.label = textValue;
                    }
                    break;
                case "cmdp:Description":
                    result.description = textValue;
                    break;
                case "cmdp:Value":
                    result.value = textValue;
                    break;
            }
        }
        return result;
    }

    private static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException e) {
            throw new RuntimeException("could not print node", e);
        }
        return sw.toString();
    }

}
