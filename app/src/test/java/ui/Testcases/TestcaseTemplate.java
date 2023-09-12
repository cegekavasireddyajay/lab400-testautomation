package ui.Testcases;

import org.testng.annotations.Test;
import ui.*;
import ui.Helper.Helper;


public class TestcaseTemplate extends BaseTestThreading {

    //Testcase details
    private static final String TESTCASE_SUMMARY     = "TESTCASE SUMMARY";
    private static final String TESTCASE_DESCRIPTION = "TESTCASE DESCRIPTION";

    //Test parameters
    private static final String USERTYPE = "ClientRole";
    private static final int ANALYSIS_ID = 24976; //Ferritine

    protected TestcaseTemplate(String browserName) {
        super();
        setTestcaseDetails(TESTCASE_SUMMARY, TESTCASE_DESCRIPTION);
    }

    @Test
    public void Testcase() throws Exception {

        final int DOCTOR_ID = Integer.parseInt(Helper.getEnvironmentProperty("doctorID"));
        final int PATIENT_ID = lab400Steps.getRandomPatient(DOCTOR_ID);

    }

}
