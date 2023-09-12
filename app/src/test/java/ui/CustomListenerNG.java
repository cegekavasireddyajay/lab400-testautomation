package ui;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class CustomListenerNG extends TestListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(CustomListenerNG.class);

    @Override
    public void onTestFailure(ITestResult tr) {
        LOG.info("FAILED");
    }

    @Override
    public void onTestSkipped(ITestResult tr) {
        LOG.info("SKIPPED");
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        LOG.info("PASSED");
    }


}
