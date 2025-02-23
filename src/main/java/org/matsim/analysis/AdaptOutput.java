package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.application.MATSimAppCommand;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import picocli.CommandLine;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.matsim.application.ApplicationUtils.globFile;

@CommandLine.Command(
        name = "adapt-v2-output",
        description = "Adapt run output from a v2 kelheim sim run, such that standard dashboards can be created for the run."
)
public class AdaptOutput implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(AdaptOutput.class);

    @CommandLine.Option(names = "--runDir", description = "Path of V2 run directory with files to adapt.", required = true)
    private String dir;

    public static void main(String[] args) {
        new AdaptOutput().execute(args);
    }

    @Override
    public Integer call() throws Exception {

//		copy original config file
        File inputConfigFile = new File(globFile(Path.of(dir), "*output_config.xml").toString());
        Path targetConfigPath = Path.of(inputConfigFile.getPath().split(".xml")[0] + "_withZonalSystemParams_withTravelTimeCalculatorParam.xml");
        copyFile(targetConfigPath, inputConfigFile);

//		comment out config params which produce errors
        adaptConfigAndWriteXml(inputConfigFile);

        Map<String, List<String>> filePaths = new HashMap<>();
        return 0;
    }

    private static void adaptConfigAndWriteXml(File inputConfigFile) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputConfigFile);
        doc.getDocumentElement().normalize();

        DocumentType doctype = doc.getDoctype();

        NodeList moduleList = doc.getElementsByTagName("module");
        for (int i = 0; i < moduleList.getLength(); i++) {
            Element moduleElement = (Element) moduleList.item(i);

            if (moduleElement.getAttribute("name").equals("travelTimeCalculator")) {
                NodeList paramList = moduleElement.getElementsByTagName("param");
                for (int j = 0; j < paramList.getLength(); j++) {
                    Element paramElement = (Element) paramList.item(j);
                    if (paramElement.getAttribute("name").equals("travelTimeCalculator")) {
                        // Comment out the param element
                        Comment comment = doc.createComment(paramElement.getTextContent());
                        moduleElement.replaceChild(comment, paramElement);
                        break;
                    }
                }
                break;
            }
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(inputConfigFile);
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.transform(source, result);
    }

    private static void copyFile(Path targetPath, File inputFile) {
        if (Files.notExists(targetPath) && inputFile.exists() && inputFile.isFile()) {
            try {
                Files.copy(inputFile.toPath(), targetPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            log.warn("File {} was not copied to target path {}. Please check if file already exists in target dir, the input file exists and the input file is not a directory."
                    , inputFile.getAbsolutePath(), targetPath);
        }
    }
}