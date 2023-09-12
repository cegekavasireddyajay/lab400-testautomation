package ui;

import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;
import ui.Helper.Helper;

import java.util.List;

import static java.lang.Integer.parseInt;

public class AlterSuiteListener implements IAlterSuiteListener {

    @Override
    public void alter(List<XmlSuite> suites) {
        XmlSuite suite = suites.get(0);
        suites.get(0).getTests().get(0).setThreadCount(parseInt(Helper.getEnvironmentProperty("threadCount")));
    }
}
