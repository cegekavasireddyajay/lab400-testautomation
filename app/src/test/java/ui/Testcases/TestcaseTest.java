package ui.Testcases;

import org.testng.annotations.Test;
import ui.*;

public class TestcaseTest extends BaseTestThreading {

    //Testcase details
    private static final String TESTCASE_SUMMARY     = "TESTCASE SUMMARY";
    private static final String TESTCASE_DESCRIPTION = "TESTCASE DESCRIPTION";

    //Test parameters
    private static final String USERTYPE = "TESTDE";
    private static final int ANALYSIS_ID = 24976; //Ferritine
    private static final int AST_GOT_ID = 7230;  //AST (GOT)


    protected TestcaseTest(String browserName) { super(); setTestcaseDetails(TESTCASE_SUMMARY, TESTCASE_DESCRIPTION); }

    @Test
    public void Testcase() throws Exception {
        //login("admin");
    }

}
