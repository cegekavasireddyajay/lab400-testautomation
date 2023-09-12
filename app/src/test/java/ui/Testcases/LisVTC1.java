package ui.Testcases;

import org.testng.annotations.Test;
import ui.BaseTestThreading;
import ui.Steps.Lab400Steps;

public class LisVTC1 extends BaseTestThreading {

  //Testcase details
  private static final String TESTCASE_SUMMARY     = "Ordval of the order";
  private static final String TESTCASE_DESCRIPTION = "For any given order id, lab400 steps are executed to do the ORDVAL";


  public LisVTC1() {
    setTestcaseDetails(TESTCASE_SUMMARY, TESTCASE_DESCRIPTION);
  }

  @Test
  public void Testcase() throws Exception {
    Lab400Steps lab400Steps = new Lab400Steps();
    String orderID = "123.456"; //FIXME Should come from API/other lab400 steps
    lab400Steps.ordVal(orderID);
  }
}
