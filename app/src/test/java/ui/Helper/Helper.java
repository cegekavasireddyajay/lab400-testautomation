package ui.Helper;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.testng.Reporter;


public class Helper {

  private static final Logger LOG = LoggerFactory.getLogger(Helper.class);
  public static String printlabelLocation;
  public static String executionId;
  private static final String screenshotLocation;
  private static final String as400Location;
  private static final String videoLocation;
  private static final String htmlReportLocation;

  static {
    Path currentWorkingDir = Paths.get("").toAbsolutePath();
    Path htmlPath = Paths.get(currentWorkingDir.toString(), "app", "test-output", "reports");
    Path screenshotPath = Paths.get(currentWorkingDir.toString(), "app", "test-output",
        "screenshots");
    Path printLabelPath = Paths.get(currentWorkingDir.toString(), "app", "test-output",
        "printlabels");
    Path as400LocationPath = Paths.get(currentWorkingDir.toString(), "app", "test-output", "as400");
    Path videoLocationPath = Paths.get(currentWorkingDir.toString(), "app", "test-output",
        "videos");

    createExecutionId();

    screenshotLocation = screenshotPath.toString();
    as400Location = as400LocationPath.toString();
    videoLocation = videoLocationPath.toString();
    htmlReportLocation = htmlPath.toString();
    printlabelLocation = printLabelPath.toString();
  }

  public Helper() {
  }

  private static void createExecutionId() {
    Timestamp ts = Timestamp.from(Instant.now());
    final SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
    executionId = sdf1.format(ts);
  }

  private static void cleanupOldScreenshots() {
    cleanupFolder(screenshotLocation);
  }

  private static void cleanupOldAS400Logs() {
    cleanupFolder(as400Location);
  }

  private static void cleanupOldVideos() {
    cleanupFolder(videoLocation);
  }

  private static void cleanupOldReports() {
    cleanupFolder(htmlReportLocation);
  }

  private static void cleanupOldPrintlabels() {
    cleanupFolder(printlabelLocation);
  }

  private static void cleanupFolder(String location) {
    File dir = new File(location);
    if (dir.isDirectory()) {
      for (File file : dir.listFiles()) {
        if (!file.isDirectory()) {
          long diff = new Date().getTime() - file.lastModified();
          if (diff > 3 * 24 * 60 * 60 * 1000) {
            file.delete();
          }
        }
      }
    }
  }

  public static void clearAllTestResults() {
    cleanupOldVideos();
    cleanupOldScreenshots();
    cleanupOldPrintlabels();
    cleanupOldReports();
    cleanupOldAS400Logs();
  }

  public static void createOutputFolders() throws IOException {
    Files.createDirectories(Paths.get(screenshotLocation));
    Files.createDirectories(Paths.get(videoLocation));
    Files.createDirectories(Paths.get(htmlReportLocation));
    Files.createDirectories(Paths.get(as400Location));
    Files.createDirectories(Paths.get(printlabelLocation));
  }


  //Sleep function
  public static void sleep(int timer) {
    try {
      Thread.sleep(timer);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  //Get current environment
  public static String getEnvironment() {
    String env = System.getProperty("environment");
    if (env != null) {
      env = env.toUpperCase();
    }
    return env;
  }

  /**
   * Get current browser
   */
  public static String getBrowser() {
    return Reporter.getCurrentTestResult().getTestContext().getCurrentXmlTest()
        .getParameter("browser");
  }

  public static String getTestplan() {
    return Reporter.getCurrentTestResult().getTestContext().getCurrentXmlTest()
        .getParameter("testplan");
  }

  /**
   * Get Jira test execution key where we need to push the test execution results to. Might be
   * empty, as it's not a mandatory parameter
   */
  public static String getJiraTestExec() {
    return System.getProperty("testExec");
  }

  /**
   * Resolve properties which belong to the current environment
   */
  public static Properties resolveEnvironment(String environment) throws IOException {
    if (!environment.equals("DEV") && !environment.equals("TST") && !environment.equals("QAS")
        && !environment.equals("PRD")) {
      LOG.info("Unsupported environment!");
    }
    //LOG.info("Env is: " + environment);
    return PropertiesLoaderUtils.loadProperties(
        new ClassPathResource("/environments/" + environment + ".properties"));
  }

  /**
   * Get specific property for the current environment
   */
  public static String getEnvironmentProperty(String property) {
    String value = null;
    try {
      Properties environmentProperties = resolveEnvironment(getEnvironment());
      //LOG.info(environmentProperties.getProperty(property));
      value = environmentProperties.getProperty(property);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return value;
  }

  public static String getNetworkSpecificProperty(String propertyCegekaNetwork,
      String propertyViollierNetwork) {
    String property = Network.select(propertyCegekaNetwork, propertyViollierNetwork);
    return getEnvironmentProperty(property);
  }

  /**
   * Get property which is environment independent.
   */
  public static String resolveGlobal(String property) throws IOException {
    String value;
    Properties environmentProperties = PropertiesLoaderUtils.loadProperties(
        new ClassPathResource("/environments/GLOBAL.properties"));
    value = environmentProperties.getProperty(property);

    return value;
  }

  /**
   * Get information on paths which is environment independent.
   */
  public static String resolveGlobalPath(String property) throws IOException {
    String value;
    Path currentWorkingDir = Paths.get("").toAbsolutePath();
    Properties environmentProperties = PropertiesLoaderUtils.loadProperties(
        new ClassPathResource("/environments/GLOBAL.properties"));
    value = currentWorkingDir + "/" + environmentProperties.getProperty(property);

    return value;
  }

  /**
   * Get testcase execution ID. This one is generated at random at the beginning of the test.
   */
  public String testCaseExecutionID() {
    return executionId;
  }

}
