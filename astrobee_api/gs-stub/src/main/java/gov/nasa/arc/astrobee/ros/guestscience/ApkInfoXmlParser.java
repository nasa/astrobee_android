package gov.nasa.arc.astrobee.ros.guestscience;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApkInfoXmlParser {
    private static final Log logger = LogFactory.getLog(ApkInfoXmlParser.class);

        public static ApkInfo parseFile(String filePath) {
            File xmlFile = new File(filePath);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            try {
                dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(xmlFile);
                doc.getDocumentElement().normalize();
                NodeList nodeList = doc.getElementsByTagName("apkInfo");
                // we expect information for only one APK
                return getApkInfo(nodeList.item(0));
            } catch (SAXException | ParserConfigurationException | IOException e1) {
                e1.printStackTrace();
            }
            return null;
        }

        private static ApkInfo getApkInfo(Node node) {
            //XMLReaderDOM domReader = new XMLReaderDOM();
            ApkInfo apkInfo = new ApkInfo();
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                    apkInfo.setShortName(getTagValue("shortName", element));
                    apkInfo.setPrimary(Boolean.parseBoolean(getTagValue("primary", element)));
                try {
                    apkInfo.setFullName(getTagValue("fullName", element));
                } catch (NullPointerException npe) {
                    logger.error("No full APK name listed in xml file");
                    apkInfo.setFullName("none");
                }

                NodeList cmdList = element.getElementsByTagName("commands").item(0).getChildNodes();
                List<Command> commandList = new ArrayList<>();
                for (int i = 0; i < cmdList.getLength(); i++) {
                    Command c = getCommand(cmdList.item(i));
                    if(c != null) {
                        commandList.add(c);
                    }
                }
                apkInfo.setCommands(commandList);
            }
            return apkInfo;
        }

        private static Command getCommand(Node n) {
            if(n.getAttributes() == null) {
                return null;
            }
            String name = n.getAttributes().getNamedItem("name").getNodeValue();
            String syntax = n.getAttributes().getNamedItem("syntax").getNodeValue();

            Command c = new Command();
            c.setName(name);
            c.setSyntax(syntax);
            return c;
        }

        private static String getTagValue(String tag, Element element) throws NullPointerException {
            NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
            Node node = (Node) nodeList.item(0);
            return node.getNodeValue();
        }

}
