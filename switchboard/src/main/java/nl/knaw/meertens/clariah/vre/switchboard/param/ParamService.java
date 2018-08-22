package nl.knaw.meertens.clariah.vre.switchboard.param;

import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceKind;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServiceRecord;
import nl.knaw.meertens.clariah.vre.switchboard.registry.services.ServicesRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

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

    public Cmdi getParams(long serviceId) {
        ServiceRecord service = servicesRegistryService.getService(serviceId);
        Cmdi params = new Cmdi();
        params.id = serviceId;
        params.name = service.getName();
        params.kind = ServiceKind.fromKind(service.getKind());
        params.params = convertCmdiXmlToParams(service.getSemantics());
        if(params.kind.equals(ServiceKind.VIEWER)) {
            params.params = removeOutputParam(params.params);
        }
        return params;
    }

    /**
     * Remove output param because it will be set by switchboard
     */
    private List<Param> removeOutputParam(List<Param> params) {
        return params.stream()
                .filter(p -> !p.name.equals("output"))
                .collect(Collectors.toList());
    }

    private List<Param> convertCmdiXmlToParams(String cmdi) {

        List<Param> result = new ArrayList<>();

        try {
            InputSource inputSource = new InputSource(new StringReader(cmdi));
            Document xml = builder.parse(inputSource);

            XPath xPath = XPathFactory.newInstance().newXPath();
            String paramExpression = "//*[local-name() = 'Input'] / *[local-name() = 'Parameter']";
            NodeList parameters = (NodeList) xPath.compile(paramExpression).evaluate(xml, XPathConstants.NODESET);
            if (parameters == null || parameters.getLength() == 0) {
                logger.warn(String.format("No parameters found in cmdi [%s]", cmdi));
            } else {
                result.addAll(mapParameters(parameters));
            }

            String groupExpression = "//*[local-name() = 'Input'] / *[local-name() = 'ParameterGroup']";
            NodeList groups = (NodeList) xPath.compile(groupExpression).evaluate(xml, XPathConstants.NODESET);
            if (groups == null || groups.getLength() == 0) {
                logger.warn(String.format("No parameter groups found in cmdi [%s]", cmdi));
            } else {
                result.addAll(mapParameterGroups(groups));
            }

            return result;
        } catch (SAXException | IOException | XPathExpressionException e) {
            throw new RuntimeException(String.format("Could not parse cmdi xml [%s]", cmdi), e);
        }
    }

    private List<ParamGroup> mapParameterGroups(NodeList groups) {
        List<ParamGroup> result = new ArrayList<>();
        for (int i = 0; i < groups.getLength(); i++) {
            Node xmlGroup = groups.item(i);
            ParamGroup paramGroup = new ParamGroup();
            mapParameter(xmlGroup, paramGroup);
            NodeList parameters = ((Element) xmlGroup).getElementsByTagName("cmdp:Parameters").item(0).getChildNodes();
            paramGroup.params.addAll(mapParameters(parameters));
            result.add(paramGroup);
        }
        return result;
    }

    private List<Param> mapParameters(NodeList parameters) {
        List<Param> result = new ArrayList<>();
        for (int i = 0; i < parameters.getLength(); i++) {
            Node xmlParam = parameters.item(i);
            if(!xmlParam.getNodeName().equals("cmdp:Parameter")) {
                continue;
            }
            Param param = new Param();
            mapParameter(xmlParam, param);
            result.add(param);
        }
        return result;
    }

    private <T extends Param> void mapParameter(Node xmlParam, T result) {
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
                    result.name = fieldValue;
                    break;
                case "cmdp:Label":
                    String labelLanguage = node.getAttributes().getNamedItem("xml:lang").getNodeValue();
                    if (labelLanguage.equals(currentLanguage)) {
                        result.label = fieldValue;
                    }
                    break;
                case "cmdp:DataType":
                    result.type = ParamType.fromString(fieldValue);
                    break;
                case "cmdp:MIMEType":
                    result.type = ParamType.FILE;
                    break;
                case "cmdp:Description":
                    result.description = fieldValue;
                    break;
                case "cmdp:MinimumCardinality":
                    result.minimumCardinality = fieldValue;
                    break;
                case "cmdp:MaximumCardinality":
                    result.maximumCardinality = fieldValue;
                    break;
                case "cmdp:Values":
                    result.values = mapParamValues(node);
                    break;
            }

        }
        // Enumeration type is determined by the presence of values
        // and should overwrite DataType:
        if (result.values != null) {
            result.valuesType = result.type;
            result.type = ParamType.ENUMERATION;
        }
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

}
