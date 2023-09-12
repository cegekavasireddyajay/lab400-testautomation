package ui.Testcases;

import org.testng.annotations.Test;
import ui.BaseTestThreading;
import ui.Steps.Lab400Steps;

public class LisVTC1 extends BaseTestThreading {


  @Test
  public void Testcase() throws Exception {
    Lab400Steps lab400Steps = new Lab400Steps();
    String orderID ="123.456"; //FIXME Should come from API/other lab400 steps
    lab400Steps.ordVal(orderID);
  }
}
