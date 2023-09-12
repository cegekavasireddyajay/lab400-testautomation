package ui;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import ui.Helper.Helper;
import ui.Helper.Passwords;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class XrayReporter {
    private static final Logger LOG = LoggerFactory.getLogger(XrayReporter.class);


    public static void main(String[] args) throws IOException, InterruptedException {
        postResults();
    }

    public static String xrayAuthenticationKey() throws IOException, InterruptedException {
        String client_id = Passwords.readPassword("client_id");
        String client_secret = Passwords.readPassword("client_secret");
        String requestBody = "{ \"client_id\": \"" + client_id + "\",\"client_secret\": \"" + client_secret + "\" }";

        Response response = Request.Post("https://xray.cloud.getxray.app/api/v2/authenticate")
                .addHeader("Content-Type", "application/json")

                .bodyString(requestBody, ContentType.APPLICATION_JSON)
                .execute();

        String auth_key = response.returnContent().toString().replace("\"", "");

        return auth_key;
    }


    public static void postResults() throws IOException, InterruptedException {
        String authKey = xrayAuthenticationKey();

        String reportLocation = "./build/reports/tests/test/testng-results.xml";
        Path filePath = Path.of(reportLocation);
        String TestExecKey = null;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new File(reportLocation));
            NodeList nodeList = document.getElementsByTagName("attribute");
            for (int x = 0, size = nodeList.getLength(); x < size; x++) {
                String attributeName = nodeList.item(x).getAttributes().getNamedItem("name").getNodeValue();
                //LOG.info("Found key in xml testresult: "+ attributeName);
                if (attributeName.equals("testExec")) {
                    TestExecKey = nodeList.item(x).getTextContent().strip();
                }
            }
        } catch (Exception e) {
            LOG.info("Could not get test execution key from XML result output");
        }

        String projectKey = Helper.resolveGlobal("JiraProject");
        String target = "https://xray.cloud.getxray.app/api/v2/import/execution/testng?projectKey=" + projectKey + "&testExecKey=" + TestExecKey;
        String testNGResults = Files.readString(filePath);
        Request request = Request.Post(target)
                .addHeader("Authorization", "Bearer " + authKey)
                .addHeader("Content-Type", "application/xml")
                .bodyString(testNGResults, ContentType.APPLICATION_XML);
        Response response = request.execute();
        LOG.info(response.returnResponse().toString());
    }

}