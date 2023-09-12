package ui;

import org.testng.*;
import org.testng.reporters.*;
import org.testng.xml.XmlSuite;

import java.util.*;

public class CustomXMLReporter extends XMLReporter {

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        getConfig().setGenerateTestResultAttributes(true);
        super.generateReport(xmlSuites, suites, outputDirectory);
    }
}
