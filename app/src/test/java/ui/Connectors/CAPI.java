package ui.Connectors;


import java.io.IOException;
import java.time.Duration;
import org.apache.http.client.fluent.Request;
import org.awaitility.Awaitility;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import ui.Helper.Conversion;
import ui.Helper.Helper;


public class CAPI {
    // Connect to CAPI
    private static final Logger LOG = LoggerFactory.getLogger(CAPI.class);

    private String labCluster = Helper.getNetworkSpecificProperty("labCluster_ingress" ,"labCluster");

    /**
     * Get orderID details. Could return an empty object in case the orderID isn't there yet.
     * @param orderID
     * @return
     * @throws IOException
     */
    public JSONObject getOrderID(String orderID) throws IOException {

        orderID = Conversion.padWithLeadingZeros(orderID);

        String CAPIURI = "/capi/internal/admin/orders/" + orderID;
        String response = getResponseAsString(CAPIURI);

        String tmp = null;
        try {
            tmp = response.substring(1, response.length() - 1); //Remove encapsulation of outer brackets
        } catch (Exception e) {
            //Could not remove encapsulation, return empty JSON object
            return new JSONObject();
        }

        if(tmp.length() == 0){
            return new JSONObject();
        }
        return new JSONObject(tmp);

    }

    public void waitForOrderIDToExist(String orderID) {
        waitForOrderIDToExist(orderID, 60000);
    }

    private void waitForOrderIDToExist(String orderID, int timeoutMs) {
        //Can be either DRAFT, ORDERED, APPROVED
        long startTime = System.currentTimeMillis();
        long currentTime = startTime;
        boolean orderFound = false;

        JSONObject orderState = null;

        while (currentTime < startTime + timeoutMs && !orderFound) {
            try {
                orderState = getOrderID(orderID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (orderState.length() > 0) {
                //LOG.info("Confirmed order found on CAPI side");
                orderFound = true;
            }
            currentTime = System.currentTimeMillis();
        }

    }

    private void waitForOrderIDToBeInState(String orderID, String expectedState, int timeoutMs) {
        //Can be either DRAFT, ORDERED, APPROVED

        Awaitility.await().pollInterval(Duration.ofSeconds(15))
            .timeout(Duration.ofMillis(timeoutMs))
            .untilAsserted(
                ()-> Assert
                    .assertEquals(getOrderID(orderID)
                        .getJSONObject("order")
                        .getString("status")
                        .toUpperCase(),
                        expectedState,
                        "State '" + expectedState + "' could not be reached within " + timeoutMs + "ms for order ID " + orderID)
            );
    }

    public void waitForOrderIDToBeInState(String orderID, String expectedState) throws Exception {
        int timeout = 120000;
        if(expectedState.equalsIgnoreCase("AUTO_VALIDATED")) timeout = timeout + 60000;
        waitForOrderIDToBeInState(orderID, expectedState, timeout);
    }


    private void waitForLabResults(String orderID, int analyseID, float expectedLabResult, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        long currentTime = startTime;
        boolean stateCorrect = false;
        String labValueString = "?";
        float actualLabResult = 0;

        JSONObject orderState;

        while (currentTime < startTime + timeoutMs && !stateCorrect) {
            orderState = getOrderID(orderID);

            for (int i = 0; i < orderState.length(); i++) {
                if(orderState.getJSONObject("order").getJSONArray("samples").length() > 0){
                    int analysisNumberOnCapi = 0;
                    try {
                        analysisNumberOnCapi = orderState.getJSONObject("order").getJSONArray("samples").getJSONObject(0).getJSONArray("analysisResults").getJSONObject(i).getInt("analysisNumber");
                    }catch (JSONException e){
                        //Analysis date might not be present yet. Try again later
                    }

                    if (Integer.compare(analysisNumberOnCapi, analyseID) == 0) {
                        labValueString = orderState.getJSONObject("order").getJSONArray("samples").getJSONObject(0).getJSONArray("analysisResults").getJSONObject(0).getString("value");
                        i = orderState.length() + 1; //Abort immediately
                    }
                }

            }

            if (!labValueString.equals("?")) actualLabResult = Float.parseFloat(labValueString);

            if (Float.compare(expectedLabResult, actualLabResult) == 0) {
                stateCorrect = true;
                LOG.info("Confirmed lab result on CAPI: " + actualLabResult);
            }else {
                Helper.sleep(1000); //Don't spam the CAPI server by delaying next request for 1 second.
            }

        currentTime = System.currentTimeMillis();
        }

        if (!stateCorrect) {
            throw new Exception("Expected lab result state could not be reached within " + timeoutMs + "ms for order ID " + orderID);
        }
    }

    public void waitForLabAnalyse(String orderID, int analyseID, int timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        long currentTime = startTime;
        boolean analysisFound = false;
        String labValueString = "?";
        float actualLabResult = 0;

        JSONObject orderState;

        while (currentTime < startTime + timeoutMs && !analysisFound) {
            orderState = getOrderID(orderID);

            for (int i = 0; i < orderState.length(); i++) {
                if(orderState.getJSONObject("order").getJSONArray("samples").length() > 0){
                    int analysisNumberOnCapi = 0;
                    try {
                        analysisNumberOnCapi = orderState.getJSONObject("order").getJSONArray("samples").getJSONObject(0).getJSONArray("analysisResults").getJSONObject(i).getInt("analysisNumber");
                        analysisFound = true;
                    }catch (JSONException e){
                        //Analysis date might not be present yet. Try again later
                    }

                }

            }

            currentTime = System.currentTimeMillis();
        }

        if (!analysisFound) {
            throw new Exception("Expected analysis could not be found within  " + timeoutMs + "ms for order ID " + orderID);
        }
    }

    public void waitForLabResults(String orderID, int analyseID, float expectedLabResult) throws Exception {
        waitForLabResults(orderID, analyseID, expectedLabResult, 120000);
    }

    private String getResponseAsString(String endpoint) throws IOException {

        //First, figure out if we can reach the component directly or if we should fall back on its ingress endpoint
        String port = Helper.getEnvironmentProperty("CAPIPort");
        String URI = labCluster + ":" + port + endpoint ;

        return Request.Get(URI)
                .connectTimeout(10000)
                .addHeader("Authorization", Helper.getEnvironmentProperty("APIAuthBasic"))
                .execute().returnContent().asString();
    }
}