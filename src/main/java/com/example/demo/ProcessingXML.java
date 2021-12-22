package com.example.demo;

import com.example.demo.service.TrackService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessingXML {
    private final String fileName;
    private final TrackService trackService;

    public ProcessingXML(String fileName, TrackService trackService) {
        this.fileName = fileName;
        this.trackService=trackService;
    }

    public boolean xmlValidation() {

        String fileExtension, xmlScheme;

        fileExtension = getPartFileName(2);

        if (fileExtension.equals("gpx")) xmlScheme = "http://www.topografix.com/GPX/1/1/gpx.xsd";
        else if (fileExtension.equals("kml")) xmlScheme = "http://schemas.opengis.net/kml/2.2.0/ogckml22.xsd";
        else return false;

        try {
            SchemaFactory factory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(xmlScheme));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(fileName));
            return true;
        } catch (
                org.xml.sax.SAXException | IOException e) {
            System.out.println("Error file validation: " + e.getMessage());
            return false;
        }
    }

    public Set<String> xmlParsing() throws ParserConfigurationException, IOException, SAXException {
        NodeList nList;
        Node nNode;
        Element eElement;
        HashSet<String> listCodeCoordinates = new HashSet<>();

        String fileExtension = getPartFileName(2);
        int trkid = Integer.parseInt(getPartFileName(1)), accuracy = 22;
        double lat = 0, lon = 0;
        String codeCoordinate;

        File fXmlFile = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);

        doc.getDocumentElement().normalize();

        nList = fileExtension.equals("gpx") ? doc.getElementsByTagName("trkpt") : doc.getElementsByTagName("LookAt");

        if (trkid != -1) {
            for (int i = 0; i < nList.getLength(); i++) {
                nNode = nList.item(i);
                eElement = (Element) nNode;
                if (fileExtension.equals("gpx")) {
                    lat = Double.parseDouble(eElement.getAttribute("lat"));
                    lon = Double.parseDouble(eElement.getAttribute("lon"));
                } else if (fileExtension.equals("kml")) {
                    lat = Double.parseDouble(eElement.getElementsByTagName("latitude").item(0).getTextContent());
                    lon = Double.parseDouble(eElement.getElementsByTagName("longitude").item(0).getTextContent());
                }
                codeCoordinate = trackService.transferCoordinates(lon, lat, accuracy);
                listCodeCoordinates.add(codeCoordinate);
            }
        }
        return listCodeCoordinates;
    }

    public String getPartFileName(int idGroup) {
        Pattern pattern = Pattern
                .compile("upload-dir/([0-9]*)?\\.([kml|gpx]*)?");

        Matcher matcher = pattern.matcher(fileName);

        return (matcher.find()) ? matcher.group(idGroup) : "-1";
    }


}
