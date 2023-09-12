package ui.Helper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.Reporter;
import ui.BaseTestThreading;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Reporting {
	private static final Logger LOG = LoggerFactory.getLogger(Reporting.class);
	public void setJira(String testcaseID, String TESTCASE_SUMMARY, String TESTCASE_DESCRIPTION){

		String jiraProject = null;
		String jiraKey = null;
		try {
			jiraProject = Helper.resolveGlobal("JiraProject");
		} catch (IOException e) {
			e.printStackTrace();
		}

		try{
			jiraKey = testcaseID.split(jiraProject)[1];
		}catch (Exception e){
			jiraKey = "00000";
		}

		String fullTestCaseID = jiraProject + "-" + jiraKey;
		LOG.info("Reporting for testcase ID : " + fullTestCaseID);

		ITestResult result = Reporter.getCurrentTestResult();

		result.setAttribute("test", fullTestCaseID);
		//result.setAttribute("testplan", Helper.getTestplan());
		result.setAttribute("testExec", Helper.getJiraTestExec());
		String tml = Helper.getJiraTestExec();
		result.setAttribute("summary", TESTCASE_SUMMARY);
		result.setAttribute("description", TESTCASE_DESCRIPTION);

	}


	public void generateHTMLReport(BaseTestThreading baseTestThreading, ITestResult result) throws IOException {
		//Generate HTML report with the screenshots and AS400 logs as input.
		Path currentWorkingDir = Paths.get("").toAbsolutePath();
		Path htmlPath = Paths.get(currentWorkingDir.toString(), "app", "test-output", "reports");
		String htmlReportLocation  = htmlPath.toString();

		String testcaseExecutionID = baseTestThreading.helper.testCaseExecutionID();
		//LOG.info("Generating report for " + testcaseExecutionID);
		String content = "<html>" +
				"<style>\n" +
				"body {\n" +
				"  background-color: white;\n" +
				"}\n" +
				"tt {\n" +
				"  white-space: pre;\n" +
				"  color: grey;\n" +
				"}\n" +
				"</style>" +
				"" +
				"<body><H1>Testcase execution report</H1>" +
				"<table cellspacing=\"0\" cellpadding=\"8\" bordercolor=\"black\" border=\"1\">" +
				"  <tr>\n" +
				"    <th>Description</th>\n" +
				"    <th>Value</th>\n" +
				"  </tr>";

		String testcaseID = baseTestThreading.getClass().getSimpleName();
		String TESTCASE_SUMMARY = baseTestThreading.getTESTCASE_SUMMARY();
		String TESTCASE_DESCRIPTION = baseTestThreading.getTESTCASE_DESCRIPTION();
		//String testcaseResult = baseTestThreading.getTestCaseResult();
		String environment = Helper.getEnvironment();


		String testcaseResult = "";

		if(result.getStatus() == ITestResult.SUCCESS){
			//LOG.info("Test case execution status is SUCCESS");
			testcaseResult = "PASSED";
		}
		else if(result.getStatus() == ITestResult.FAILURE){
			//LOG.info("Test case execution status is FAILURE");
			testcaseResult = "FAILED";
		}
		else if(result.getStatus() == ITestResult.SKIP ){
			//LOG.info("Test case execution status is SKIP");
			testcaseResult = "SKIPPED";
		}

		content = addToTable("Testcase ID", testcaseID, content);
		content = addToTable("Testcase result", testcaseResult, content);
		content = addToTable("Environment", Helper.getEnvironment() , content);
		content = addToTable("Testcase summary", TESTCASE_SUMMARY, content);
		content = addToTable("Testcase description", TESTCASE_DESCRIPTION, content);
		String datetimeStart = DateTime.generateDateTime( result.getStartMillis(), "yyyy-MM-dd HH:mm");
		String datetimeEnd = DateTime.generateDateTime( result.getEndMillis(), "HH:mm");
		content = addToTable("Time of execution", datetimeStart + " until " + datetimeEnd, content);

		content = content + "</table></br>";

//		content = addScreenshots(testcaseExecutionID, content);

//		content = addLabels(testcaseExecutionID, content);
//
//		content = addVideolink(testcaseExecutionID, content);

		content = addStackTrace(result, content);

		content = addAS400Trace(testcaseExecutionID, content);

		content = content + "</body></html>"; //HTML closure

		try {
			String reportFilename = testcaseID + "_"  + testcaseExecutionID + ".html";
			//String videoFileName = testcaseExecutionID + ".webm"; //Disabled, as video is integrated in HTML report
			FileWriter myWriter = new FileWriter(Path.of(htmlReportLocation , reportFilename).toString());
			FileWriter lastReport = new FileWriter(Path.of(htmlReportLocation , "lastExecution.html").toString());

			myWriter.write(content);
			myWriter.close();
			lastReport.write(content);
			lastReport.close();

			String stackMessage = null;
			if(result.getThrowable() != null){
				String classname = result.getThrowable().getClass().getCanonicalName();
				stackMessage = classname + ": " + result.getThrowable().getMessage();
			}
			pushToElastic(testcaseID, result.getStartMillis(), result.getEndMillis(), testcaseResult, environment, reportFilename,  stackMessage, baseTestThreading.getSuiteName() );

			//Anything different from status PASSED should be pushed to the external server
			if(result.getStatus() != ITestResult.SUCCESS){
				pushToExternalServer("reports", reportFilename);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Add a new title to the report
	 * @param description
	 * @param base
	 * @return
	 */
	private static String addTitleContent(String description, String base){
		String contentAdded = base + "<h2>" + description + ":</h2></br>";
		return contentAdded;
	}

	/**
	 * Add the lab400 traces to the test report
	 * @param description
	 * @param content
	 * @param base
	 * @return
	 */
	private static String addCodeContent(String description, String content, String base){
		String contentAdded = base + "<h2>" + description + ":</h2><tt><xmp>" + content + "</xmp></tt></br>";
		return contentAdded;
	}

	private static String addToTable(String description, String value, String base){

		String contentAdded = base + "<tr>\n" +
				"    <td>" + description + "</td>\n" +
				"    <td>" + value + "</td>\n" +
				"  </tr>";

		return contentAdded;

	}

	/***
	 * Add the lab400 traces to the test report
	 * @param testCaseExecutionID testCaseExecutionID in full string format.
	 * @param base The report as it's formed before calling this method
	 * @return
	 */
	private static String addAS400Trace(String testCaseExecutionID, String base){
		String as400traceLocation  = null;
		try {
			as400traceLocation = Helper.resolveGlobalPath("as400TraceLocation");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String contentAdded = base;

		//Loop through the full list of files. If that contains the orderID, add it to the report.
		File dir = new File(as400traceLocation);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File path : directoryListing) {
				if(path.getName().contains(testCaseExecutionID)){
					//File content should be added to our report
					try {
						String filecontent = FileUtils.readFileToString(path);

						//Replace special chars, as these cannot be properly displayed with the monospaced chars.
						filecontent = filecontent.replace("ö", "o");
						filecontent = filecontent.replace("ü", "u");
						filecontent = filecontent.replace("ä", "a");
						filecontent = filecontent.replace("Â", "");
						filecontent = filecontent.replace("Ã", "A");
						filecontent = filecontent.replace("©", "C");

						contentAdded = addCodeContent("AS400 logs" , filecontent , base);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return contentAdded;
	}

	/**
	 * Add image contents to the report.
	 * @param filePath
	 * @param base
	 * @return
	 * @throws IOException
	 */
	private static String addImageContent(String filePath, String base) throws IOException {

		byte[] fileContent = FileUtils.readFileToByteArray(new File(filePath));
		String encodedString = Base64.getEncoder().encodeToString(fileContent);

		base = base + "</br><img src='data:image/png;charset=utf-8;base64," +encodedString + "' /><hr>";
		return base;
	}

	/**
	 * Add screenshots to the report
	 * @param testCaseExecutionID
	 * @param base
	 * @return
	 */
	private static String addScreenshots(String testCaseExecutionID, String base){

		String screenshotLocation  = null;
		try {
			screenshotLocation = Helper.resolveGlobalPath("screenshotLocation");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String contentAdded = addTitleContent("Screenshots", base);

		//Loop through the full list of files. If that contains the ID of the testcase we've executed, add it to the report.
		File dir = new File(screenshotLocation);
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null) {
			for (File path : directoryListing) {
				if(path.getName().contains(testCaseExecutionID)){
						//File should be added to our report
						try {
							String descriptionFromFilename = path.getName().split("_")[path.getName().split("_").length - 1];
							descriptionFromFilename = descriptionFromFilename.split("\\.")[0];
							contentAdded = contentAdded + "</br>" + descriptionFromFilename + ":</br>";
							contentAdded = addImageContent(screenshotLocation + path.getName(), contentAdded);
							//Push to external server. Disabled for now as screenshots are also included in the html page source. Might change later
							//postToExternalServer("screenshots", fileName);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
			}
		}

		return contentAdded;
	}

	/**
	 * Add labels (as they can be found on the PDF output) to the report
	 * @param testCaseExecutionID
	 * @param base
	 * @return
	 */
	private static String addLabels(String testCaseExecutionID, String base){

		String labelLocation  = null;
		try {
			labelLocation = Helper.resolveGlobalPath("printlabelLocation");
		} catch (IOException e) {
			e.printStackTrace();
		}
		String contentAdded = base;
		//Loop through the list of files. If that contains the ID of the testcase we've executed, add it to the report.
		File dir = new File(labelLocation);
		File[] directoryListing = dir.listFiles();
		Boolean titleAdded = false;
		if (directoryListing != null) {
			for (File path : directoryListing) {
				if(path.getName().contains(testCaseExecutionID)){
					//File should be added to our report
					if(titleAdded == false){
						contentAdded = addTitleContent("Labels", base);
						titleAdded = true;
					}
					try {
						contentAdded = addImageContent(labelLocation + path.getName(), contentAdded);
						//Push to external server. Disabled for now as screenshots are also included in the html page source. Might change later
						//postToExternalServer("screenshots", fileName);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return contentAdded;
	}

	/**
	 * Add video content of the testcase execution to the report
	 * @param testCaseExecutionID
	 * @param base
	 * @return
	 * @throws IOException
	 */
	private static String addVideolink(String testCaseExecutionID, String base) throws IOException {


		String contentAdded = addTitleContent("Video", base);

		Path currentWorkingDir = Paths.get("").toAbsolutePath();
		Path videolocationPath = Paths.get(currentWorkingDir.toString(), "app","test-output", "videos");
		String filepath = Path.of(videolocationPath.toString(), testCaseExecutionID + ".webm").toString();
		String encodedString = "";

		boolean exists = new File(filepath).exists();
		if(exists){
			byte[] fileContent = FileUtils.readFileToByteArray(new File(filepath));
			encodedString = Base64.getEncoder().encodeToString(fileContent);
			contentAdded = contentAdded + "<video controls>\n" +
					"\t<source type=\"video/webm\" src=\"data:video/webm;base64," + encodedString + "\">\n" +
					"</video>";
		}
		else{
			contentAdded = contentAdded + "No video was created for this testcase run.";
		}



		//base = base + "<video src='data:video/webm;charset=utf-8;base64," +encodedString + "' /></br>";



		return contentAdded;

	}

	private static String addStackTrace(ITestResult result, String base){

		if(result.getThrowable() != null){

			StringWriter sw = new StringWriter();
			result.getThrowable().printStackTrace(new PrintWriter(sw));
			String stackTrace = sw.toString();
			
			return addCodeContent("StackTrace", stackTrace, base);
		}
		else {
			return base; //Nothing needs to be added.
		}

	}

	/**
	 * After report is generated, we can push it to an external server.
	 * @param type
	 * @param filename
	 * @throws IOException
	 */
	private static void pushToExternalServer(String type, String filename) throws IOException {

		String command = null;
		String externalReportsLocation  = Helper.resolveGlobal("externalReportsLocation");
		switch (type) {
			case "screenshots":
				String screenshotLocation = Helper.resolveGlobalPath("screenshotLocation");
				command = "curl -X PUT --insecure -T " + screenshotLocation + filename + " " + externalReportsLocation + type + "/" + filename;
				break;
			case "reports":
				String htmlReportLocation = Helper.resolveGlobalPath("htmlReportLocation");
				command = "curl -X PUT --insecure -T " + htmlReportLocation + filename + " " + externalReportsLocation + type + "/" + filename;
				break;
			case "videos":
				String videoLocation = Helper.resolveGlobalPath("videoLocation");
				command = "curl -X PUT --insecure -T " + videoLocation + filename + " " + externalReportsLocation + type + "/" + filename;
				break;
			default:
				LOG.info("Invalid file type selected to push to external server!");
				break;
		}

		executeCurl(command);

	}

	/**
	 * Curl is used to push the reports.
	 * @param curlCommand
	 * @throws IOException
	 */
	private static void executeCurl(String curlCommand) throws IOException {

		Process process = Runtime.getRuntime().exec(curlCommand);
		process.getInputStream();

		ProcessBuilder processBuilder = new ProcessBuilder(curlCommand.split(" "));
		Process p;
		try
		{
			p = processBuilder.start();
			BufferedReader reader =  new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ( (line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String result = builder.toString();
			//LOG.info("Pushed report to external server");
			//System.out.print(result);

		}
		catch (IOException e)
		{   //System.out.print("error");
			e.printStackTrace();
		}

		process.destroy();
	}


	/***
	 * After test execution, we can push the results to Elastic. If index not present yet for the current day, we'll create one.
	 * @param testcaseID - Testcase ID that has just been executed
	 * @param epochTestTimeStart - Start time at which the testcase was executed
	 * @param epochTestTimeEnd - End time at which the testcase was finished
	 * @param executionResult - Test execution result. PASSED, FAILED, SKIPPED
	 * @param environment - Environment as used. DEV, TST, QAS, PRD
	 * @param filename - Filename of the report as it can be found on the external server
	 * @param stackMessage - Optionally, the stacktrace message as involved in the testcase
	 * @throws IOException
	 */
	private static void pushToElastic(String testcaseID, long epochTestTimeStart, long epochTestTimeEnd, String executionResult, String environment, String filename, String stackMessage, String suiteName) throws IOException {

		final String elasticUser = Passwords.readPassword("elasticUser");
		final String elasticPassword = Passwords.readPassword("elasticPassword");
		final String elasticServer =  Helper.resolveGlobal("elasticServer");
		final int portNumber = 9200;
		final String scheme = "https";

		final CredentialsProvider credentialsProvider =
				new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY,
				new UsernamePasswordCredentials(elasticUser, elasticPassword));

		RestClient restClient = RestClient.builder(
						new HttpHost(elasticServer, portNumber, scheme))
				.setHttpClientConfigCallback(httpClientBuilder -> {

					httpClientBuilder.setSSLHostnameVerifier(new NoopHostnameVerifier());

					return httpClientBuilder
							.setDefaultCredentialsProvider(credentialsProvider);
				}).build();

		long testcaseDuration = 0;
		if(epochTestTimeStart == 0) {
			epochTestTimeStart = System.currentTimeMillis(); //Fallback mechanism when time is not set. Could happen for skipped cases
		}
		else {
			testcaseDuration = epochTestTimeEnd - epochTestTimeStart;
		}


		String reportLink = Helper.resolveGlobal("externalReportsLocation") + "reports/" + filename;

		String currentDate = DateTime.generateCurrentDateTime("yyyy.MM.dd");
		Request request = new Request(
				"POST",
				"/vconsult-testresults-" + currentDate + "/_doc/");

		String body = "{\n" +
				"\"testcaseID\": \""      + testcaseID + "\" ,\n" +
				"\"@timestamp\": \""        + DateTime.generateDateTime(epochTestTimeStart, "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")    + "\" ,\n" +
				"\"executionResult\": \"" + executionResult + "\" ,\n" +
				"\"environment\": \""     + environment.toLowerCase() + "\" ,\n";
				if(stackMessage != null){

					stackMessage = stackMessage.replaceAll("[^A-Za-z0-9(),=:'. \n {}]","");

					stackMessage = stackMessage.replace("\n", "\\n");
					body = body +
							"\"failedReason\": \""      + stackMessage + "\" ,\n";
				}
				body = body +
				"\"reportLink\": \""      + reportLink + "\" ,\n" +
				"\"suite\": \""           + suiteName + "\" ,\n";

				if(testcaseDuration!=0){
					body = body + "\"executionDuration\": \""      + testcaseDuration + "\" ,\n";
				}

		body = body.substring(0, body.length() - 2) + "\n}"; //Strip off the last comma

		request.setJsonEntity(body);

		Response response = null;
		try	{
			response = restClient.performRequest(request);
			Assert.assertEquals(response.getStatusLine().getStatusCode(),201);
		}catch (Exception e){
			LOG.info("Failed pushing test execution result to Elastic.");
		}
	}
}
