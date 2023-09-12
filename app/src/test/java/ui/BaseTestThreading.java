package ui;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.internal.TestResult;
import ui.Connectors.AS400;
import ui.Connectors.DB2SQL;
import ui.Helper.Helper;
import ui.Helper.Reporting;
import ui.Steps.Lab400Steps;

@Listeners(CustomListenerNG.class)
public class BaseTestThreading extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(BaseTestThreading.class);
    private String TESTCASE_SUMMARY;
    private String TESTCASE_DESCRIPTION;

    public static String suiteName;

    //Playwright and threading support
    public Thread thread;

    //Helpers and contexts
    public Helper helper;
    public Reporting reporting = new Reporting();
    public Lab400Steps lab400Steps;

    //Connectors
    public DB2SQL db2SQL = new DB2SQL();
    public AS400 as400;
    public BaseTestThreading() {

    }

    public void setTestcaseDetails(String TESTCASE_SUMMARY, String TESTCASE_DESCRIPTION){
        this.TESTCASE_DESCRIPTION = TESTCASE_DESCRIPTION;
        this.TESTCASE_SUMMARY = TESTCASE_SUMMARY;
    }

    @BeforeSuite
    public void clearPreviousResults(){
        try {
            Helper.createOutputFolders();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Helper.clearAllTestResults();

    }

    @BeforeMethod
    public void initialSetup() throws IOException {

        try {
            thread = new BaseTestThreading();
            thread.start();
        }catch (IllegalThreadStateException e){
            throw new RuntimeException("Failed thread setup. To check!");
        }

        //Continue by setting up a session for the newly started thread
        //RetryUtils.retry(this::setupFunction, new RuntimeException(), "Exception during setupFunction");
        setupFunction();

    }

    @BeforeMethod
    public void jiraReporting(){
        String testcaseID = this.getClass().getSimpleName();
        String TESTCASE_SUMMARY = this.TESTCASE_SUMMARY;
        String TESTCASE_DESCRIPTION = this.TESTCASE_DESCRIPTION;
        reporting.setJira(testcaseID, TESTCASE_SUMMARY, TESTCASE_DESCRIPTION);
    }

    @BeforeSuite
    public void beforeTest(ITestContext ctx){
        suiteName = ctx.getCurrentXmlTest().getSuite().getName();
    }

    public String getTESTCASE_SUMMARY(){
        return this.TESTCASE_SUMMARY;
    }

    public String getTESTCASE_DESCRIPTION(){
        return this.TESTCASE_DESCRIPTION;
    }

    public String getSuiteName(){
        return suiteName;
    }

    public static void skipTestcase(String skipReason){
        throw new SkipException(skipReason);
    }

    /***
     * Should be called when testcase cannot be executed and a report needs to be created
     * @param skipReason
     */
    public void reportTestCaseAsSkipped(String skipReason){
        ITestResult result = TestResult.newEmptyTestResult();
        result.setStatus(TestResult.SKIP);
        Reporter.setCurrentTestResult(result);
        result.setThrowable(new Throwable(skipReason));
        try {
            reporting.generateHTMLReport(this, Reporter.getCurrentTestResult());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new SkipException(skipReason);
    }


    public static void main(String[] args) {
    }

    /**
     * @description Playwright setup function. Is called everytime we're setting up a new session
     */
    public void setupFunction() throws IOException {

        //Disable overload on info messages when using the SQL interface
        System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "WARNING");
        System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");

        Path currentWorkingDir = Paths.get("").toAbsolutePath();

        //Helpers
        helper = new Helper();
        as400 = new AS400();
        lab400Steps = new Lab400Steps();

        //When running on PROD, we'll need to take special care to only run testcases that don't modify any existing properties/customers
        if(Helper.getEnvironment().equalsIgnoreCase("prd")){
            String whiteListedTestcasesAsString = Helper.getEnvironmentProperty("whitelistedTestcases");
            List<String> whiteListedTestcases = new ArrayList<String>(Arrays.asList(whiteListedTestcasesAsString.split(",")));
            String testcaseID = this.getClass().getSimpleName();

            if(!whiteListedTestcases.contains(testcaseID)){
                reportTestCaseAsSkipped("Test should not be executed on production environment!");
            }
        }

    }


    @AfterMethod
    public void terminate(ITestResult result) throws IOException {
       reporting.generateHTMLReport(this, result);
    }
}
