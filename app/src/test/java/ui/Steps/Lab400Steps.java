package ui.Steps;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import ui.BaseTestThreading;
import ui.Connectors.AS400;
import ui.Connectors.CAPI;
import ui.Connectors.DB2SQL;
import ui.Helper.Conversion;
import ui.Helper.*;
import ui.Helper.RandomGenerator;

import java.sql.SQLException;
import java.util.*;

public class Lab400Steps {
    private static final Logger LOG = LoggerFactory.getLogger(Lab400Steps.class);

    private AS400 as400;
    private CAPI capi = new CAPI();

    private DB2SQL db2SQL = new DB2SQL();

    public Lab400Steps() {
        as400 = new AS400();
    }

    /**
     * Via SQL, verify if a specific order can be found
     */
    public void validateOrderIDIsOnLab400(int orderIDInt) {
        JSONObject orderDetails = db2SQL.getOrderDetails(orderIDInt);
        String orderIDFromDBString = orderDetails.getString("RQID").trim();

        int orderIDFromDB = Conversion.toInt(orderIDFromDBString);
        Assert.assertEquals(orderIDFromDB, orderIDInt);
    }

    /**
     * Via SQL, verify if a specific order can be found.
     */
    public void validateOrderIDIsOnLab400(String orderID) {
        int orderIDInt = Conversion.toInt(orderID);
        validateOrderIDIsOnLab400(orderIDInt);
    }

    /**
     * Via SQL, create new admission entry if it doesn't exist yet.
     */
    public void createAdmissionIfNotPresentYet(String admission, int patientID, int hospitalID) {
        List entries = db2SQL.getAdmissionEntries(admission, hospitalID);
        if (entries.size() == 0) {
            db2SQL.setAdmissionEntry(admission, patientID, hospitalID);
        }
    }

    /**
     * Via SQL, create new admission entry if it doesn't exist yet.
     */
    public void updateOrCreateAdmission(String admission, int patientID, int hospitalID) {
        List entries = db2SQL.getAdmissionEntries(admission, hospitalID);
        if (entries.size() == 0) {
            db2SQL.setAdmissionEntry(admission, patientID, hospitalID);
        } else {
            db2SQL.updateAdmissionEntry(admission, patientID, hospitalID);
        }
    }

    /***
     * Get random valid patient which has all the needed information (gender, birthdate,..)
     * @param doctorID
     * @return
     */
    public int getRandomPatient(int doctorID) {
        List patients = db2SQL.getAllTestPatients(doctorID);
        return RandomGenerator.randomFromIntList(patients);
    }

    public int getRandomMalePatient(int doctorID) {
        List patients = db2SQL.getAllMalePatient(doctorID);
        return RandomGenerator.randomFromIntList(patients);
    }

    /**
     * Via SQL, get a random valid patient which has an AHV number set
     */
    public int getRandomPatientWithAHVNumber(int doctorID) {
        List patients = db2SQL.getAllPatientsWithValidAHVNumber(doctorID);
        return RandomGenerator.randomFromIntList(patients);
    }

    /**
     * Via SQL, get a random valid patient which has an INS number set
     */
    public int getRandomPatientWithINSNumber(int doctorID) {
        List patients = db2SQL.getAllPatientsWithValidINSNumber(doctorID);
        int patientID = RandomGenerator.randomFromIntList(patients);
        LOG.info("Using random patient ID: " + patientID);
        return patientID;
    }

    /**
     * Via SQL, get a random valid patient which does NOT have an AHV number set
     */
    public int getRandomPatientWithoutAHVorINSNumber(int doctorID) {
        List patients = db2SQL.getAllPatientsWithoutValidAHVOrINSNumber(doctorID);
        return RandomGenerator.randomFromIntList(patients);
    }

    /**
     * Via SQL, get a random patient which doesn't have a birthdate or gender yet
     */
    public int getRandomPatientWithoutBirthDateOrGender(int doctorID) {
        List patients = db2SQL.getAllPatientsWithoutBirthDateOrGender(doctorID);
        return RandomGenerator.randomFromIntList(patients);
    }

    /**
     * Via SQL, get a random patient which does have a birthdate but no gender yet
     */
    public int getRandomPatientWithBirthDateWithoutGender(int doctorID) {
        List patients = db2SQL.getAllPatientsWithBirthDateWithoutGender(doctorID);
        return RandomGenerator.randomFromIntList(patients);
    }

    /**
     * Via SQL, get a random patient which does not have a birthdate but has a gender
     */
    public int getRandomPatientWithoutBirthDateWithGender(int doctorID) {
        List patients = db2SQL.getAllPatientsWithoutBirthDateWithGender(doctorID);
        if(patients.size() ==0){
            LOG.info("Patients are not found with the given query", new IllegalArgumentException("No Patients found"));
        }
        return RandomGenerator.randomFromIntList(patients);
    }

    /**
     * Via SQL, get a random valid patient which does NOT have an email address set
     */
    public int getRandomPatientWithoutEmail(int doctorID) {
        List patients = db2SQL.getAllPatientsWithoutEmailAddress(doctorID);
        return RandomGenerator.randomFromIntList(patients);
    }

    /**
     * Via SQL, get a random valid patient which do have an email address set
     */
    public int getRandomPatientWithEmail(int doctorID) {
        List patients = db2SQL.getAllPatientsWithEmailAddress(doctorID);
        return RandomGenerator.randomFromIntList(patients);
    }

    /***
     * Via SQL, get the names of the sendmaterial for a given orderID. As this is language dependant, a languagekey is needed as well
     * @param orderID
     * @param languageKey
     * @return
     * @throws SQLException
     */
    public List getSendMaterialForOrderID(String orderID, String languageKey) {
        List materials = db2SQL.getSendMaterial(Conversion.toInt(orderID), languageKey);
        return materials;
    }

    public void verifyPA002CodeIsPresent(int analyseID) {
        List codes = db2SQL.getAnalyseCodes(analyseID);
        Boolean PA002Present = codes.contains("PA002");
        if (PA002Present == false) {
            BaseTestThreading.skipTestcase("Skipping testcase as PA002 is absent for the selected analyse.");
        }
    }

    public void setPA002CodeIfNotPresentYet(int analyseID) {
        db2SQL.setCodeForAnalyseIDIfNotPresentYet(analyseID, "PA002");
    }

    public List<Integer> getListOfPOCTDeviceIDs() {

        List<Integer> poctDeviceIDs = new ArrayList<>();
        JSONArray devices = db2SQL.getAvailablePOCTDevicesAndAnalyses();

        for (int i = 0; i < devices.length(); i++) {
            int id = devices.getJSONObject(i).getInt("ID");
            poctDeviceIDs.add(id);
        }

        return poctDeviceIDs;
    }

    /***
     * Get analyses which are available, enabled, and disabled for a given POCT deviceID
     * @param deviceID
     * @param doctorID
     * @return
     */
    public JSONObject getPOCTDeviceAnalyses(int deviceID, int doctorID) {

        //JSONArray devices =  db2SQL.getAvailablePOCTDevicesAndAnalyses();
        List<Integer> availableAnalyses = db2SQL.getAvailableAnalysesForPOCTDevice(deviceID);
        JSONObject poctDetails = new JSONObject();
        poctDetails.put("deviceID", deviceID);
        poctDetails.put("available", availableAnalyses);
        List<Integer> enabledAnalyses = db2SQL.getIncludedAnalysesForPOCTDevice(deviceID, doctorID);
        poctDetails.put("enabled", enabledAnalyses);
        List<Integer> disabled = new ArrayList<>();
        for (int i = 0; i < availableAnalyses.size(); i++) {
            if (!enabledAnalyses.contains(availableAnalyses.get(i))) {
                disabled.add(availableAnalyses.get(i));
            }
        }
        poctDetails.put("disabled", disabled);
        return poctDetails;

    }

    /***
     * Get a random POCT device ID which includes multiple analyses, of which some are enabled and some are disabled. SKIP if none can be found
     * @param doctorID
     * @return
     */
    public JSONObject getPOCTDeviceWithMultipleEnabledAndDisabledAnalyses(int doctorID, int minimumNumberOfAnalyses, int minimumNumberOfAnalysesToAdd, int minimumNumberOfAnalysesEnabled) {

        List<Integer> poctDevices = getListOfPOCTDeviceIDs();

        Collections.shuffle(poctDevices);

        JSONObject deviceDetails = new JSONObject();

        for (int POCTDeviceID : poctDevices) {
            deviceDetails = getPOCTDeviceAnalyses(POCTDeviceID, doctorID);
            if (deviceDetails.getJSONArray("available").length() >= minimumNumberOfAnalyses && deviceDetails.getJSONArray("enabled").length() >= minimumNumberOfAnalysesEnabled && deviceDetails.getJSONArray("disabled").length() >= minimumNumberOfAnalysesToAdd) {
                return deviceDetails;
            }
        }

        //If the for loop didn't find anything, we should skip the testcase.
        BaseTestThreading.skipTestcase("Could not find a POCT device with the applied filters");

        return deviceDetails;
    }

    /**
     * Via SQL, check if the pathology comments are properly set in the ORDHKM table
     */
    public void assertPathologyComments(String orderID, String pathologyDescription, JSONObject recipients) {

        Boolean pathologyCommentFound = false;
        Boolean sampleCommentFound = false;
        JSONArray pathologyComments = db2SQL.getPathologyComments(orderID);

        int offsetCounter = 0;
        for (int j = 0; j < pathologyComments.length(); j++) {

            String commentText = getTextOfCommentLine(pathologyComments, j);

            if (commentText.startsWith("[PATHOLOGY] ")) {
                String pathologyDescriptionAsFoundInTable = commentText.split("\\[PATHOLOGY\\] ")[1].trim();
                Assert.assertEquals(pathologyDescriptionAsFoundInTable, pathologyDescription);
                pathologyCommentFound = true;
                LOG.info("Asserted: " + commentText);

            } else if (commentText.startsWith("[SAMPLE INFO] ")) {
                //Sample info may have multiple entries
                sampleCommentFound = true;

                String researchMaterialComment = recipients.getJSONArray("pathologyRecipients").getJSONObject(offsetCounter).getString("researchMaterialComment");
                String researchMaterial = recipients.getJSONArray("pathologyRecipients").getJSONObject(offsetCounter).getString("researchMaterial");
                String sendMaterial = recipients.getJSONArray("pathologyRecipients").getJSONObject(offsetCounter).getString("sendMaterial");
                offsetCounter++;

                String expectedText = "[SAMPLE INFO] " + sendMaterial + ", " + researchMaterial;

                if (!researchMaterialComment.equals("")) {
                    expectedText = expectedText + ", " + researchMaterialComment;
                }

                //Assert if the commentText matches the (trimmed) expected text
                Assert.assertEquals(commentText.trim(), expectedText, "Expected text '" + expectedText + "' did not match the actual text '" + commentText.trim() + "'. Pathology recipients data: " + recipients.getJSONArray("pathologyRecipients"));
                LOG.info("Asserted: " + expectedText);

            }
        }

        Assert.assertTrue(pathologyCommentFound, "Could not find any pathology comments for order ID " + orderID + ". " + pathologyComments);
        Assert.assertTrue(sampleCommentFound, "Could not find any pathology sample comments for order ID " + orderID + ". " + pathologyComments);
    }

    private static String getTextOfCommentLine(JSONArray pathologyComments, int line) {
        String commentText = pathologyComments.getJSONObject(line).getString("TEXT");

        //Check if the comment is found over multiple lines. This happens with long comments, so we should append this text in case the next entry does NOT start with a bracket
        for (int k = line; k < pathologyComments.length(); k++) {

            if (pathologyComments.length() > k + 1) {
                if (!pathologyComments.getJSONObject(k + 1).getString("TEXT").startsWith("[")) {
                    commentText = commentText + pathologyComments.getJSONObject(k + 1).getString("TEXT");
                } else {
                    k = pathologyComments.length(); //Abort immediately
                }
            }
        }
        return commentText;
    }

    public void assertOrderPrescriberComment(String orderID, String orderPrescriber) {
        JSONArray comments = db2SQL.getPathologyComments(orderID);

        boolean orderPrescriberCommentFound = false;
        for (int j = 0; j < comments.length(); j++) {

            String commentText = getTextOfCommentLine(comments, j);

            if (commentText.startsWith("[ORDER_PRESCRIBER] ")) {
                String description = commentText.split("\\[ORDER_PRESCRIBER\\] ")[1].trim();
                Assert.assertEquals(description, orderPrescriber);
                orderPrescriberCommentFound = true;
                LOG.info("Asserted: " + commentText);

            }
        }

        Assert.assertTrue(orderPrescriberCommentFound, "Could not find any order prescriber comments for order ID " + orderID);
    }


    /***
     * Via SQL, check if the pathology comment flags are properly set in the ORDHKM table
     * @param orderID
     * @param FLG1
     * @param FLG2
     * @param FLG3
     */
    public void assertPathologyCommentFlags(String orderID, String FLG1, String FLG2, String FLG3) {

        JSONArray pathologyComments = db2SQL.getPathologyComments(orderID);
        Assert.assertTrue(pathologyComments.length() >= 1);

        for (int i = 0; i < pathologyComments.length(); i++) {

            Assert.assertEquals(pathologyComments.getJSONObject(i).getString("FLG1"), FLG1);
            Assert.assertEquals(pathologyComments.getJSONObject(i).getString("FLG2"), FLG2);
            Assert.assertEquals(pathologyComments.getJSONObject(i).getString("FLG3"), FLG3);
        }

    }

    /***
     * Via SQL, check if the pathology flags are properly set in the ORDHDR table
     * @param orderID
     * @param SW01
     * @param SW02
     * @param SW04
     */
    public void assertOrderHDRFlags(String orderID, String SW01, String SW02, String SW04) {

        JSONObject orderDetails = db2SQL.getOrderDetails(Conversion.toInt(orderID));
        Assert.assertTrue(orderDetails.length() >= 1);

        Assert.assertEquals(orderDetails.getString("SW01"), SW01);
        Assert.assertEquals(orderDetails.getString("SW02"), SW02);
        Assert.assertEquals(orderDetails.getString("SW04"), SW04);

    }

    /***
     * Given an as400 status JSON object, check if the proper analyses are part of the JSON object
     * @param as400AnalysisStatus
     * @param expectedAnalyses - String list of all analyses as we expect them to find in lab400
     */
    public void assertAnalyses(JSONObject as400AnalysisStatus, List<String> expectedAnalyses) {

        JSONArray analyses = Arrays.asList(as400AnalysisStatus.getJSONArray("analyses")).get(0);
        List listOfAnalyses = Arrays.asList(analyses.toList()).get(0);

        for (String expectedAnalyse : expectedAnalyses) {
            Assert.assertTrue(listOfAnalyses.contains(expectedAnalyse), "The analysis is not found "+expectedAnalyse);
        }


    }

    /***
     * Assert if the lab result values as found on lab400 matches the values that we're expecting it to be
     * @param as400AnalysisStatus - The analysis status as found on lob 400 side
     * @param expectedValues - Hashmap of which values we're expecting it to be
     */
    public void assertValues(JSONObject as400AnalysisStatus, HashMap<String, String> expectedValues) {

        //Loop over the expected values, and verify if these values can be found in the as400AnalysisStatus
        for (Map.Entry<String, String> expectedEntry : expectedValues.entrySet()) {
            String key = expectedEntry.getKey();
            String expectingValue = expectedEntry.getValue();
            String resultAsFoundInLab400 = as400AnalysisStatus.getJSONObject("analysesDetails").getJSONObject(key).getString("result");
            Assert.assertEquals(resultAsFoundInLab400, expectingValue);
        }
    }

    /***
     * Assert if the entrymethod as found on lab400 matches the entrymethod that we're expecting it to be
     * @param as400AnalysisStatus - The analysis status as found on lob 400 side
     * @param expectedEntryMethods - Hashmap of which entrymethod we're expecting it to be
     */
    public void assertEntryMethods(JSONObject as400AnalysisStatus, HashMap<String, String> expectedEntryMethods) {

        //Loop over the expected entry methods, and verify if these values can be found in the as400AnalysisStatus
        for (Map.Entry<String, String> expectedEntry : expectedEntryMethods.entrySet()) {
            String key = expectedEntry.getKey();
            String expectingValue = expectedEntry.getValue();
            String entryMethodAsFoundInLab400 = as400AnalysisStatus.getJSONObject("analysesDetails").getJSONObject(key).getString("entryMethod");
            Assert.assertEquals(entryMethodAsFoundInLab400, expectingValue);
        }
    }

    /***
     * Assert if the benutzers as found on lab400 matches the benutzers that we're expecting it to be
     * @param as400AnalysisStatus - The analysis status as found on lob 400 side
     * @param expectedBenutzers - Hashmap of which benutzers we're expecting it to be
     */
    public void asserBenutzers(JSONObject as400AnalysisStatus, HashMap<String, String> expectedBenutzers) {

        //Loop over the expected values, and verify if these values can be found in the as400AnalysisStatus
        for (Map.Entry<String, String> expectedEntry : expectedBenutzers.entrySet()) {
            String key = expectedEntry.getKey();
            String expectingValue = expectedEntry.getValue();
            String benutzerAsFoundInLab400 = as400AnalysisStatus.getJSONObject("analysesDetails").getJSONObject(key).getString("benutzer");
            Assert.assertEquals(benutzerAsFoundInLab400, expectingValue);
        }

    }

    /**
     * Connect to the AS400 via Telnet and perform ORDVAL
     */
    public void ordVal(String orderID) throws Exception {
        int orderIDInt = Conversion.toInt(orderID);
        as400.ordVal(orderIDInt);
    }

    /**
     * Connect to the AS400 via Telnet and perform the invoice action. Once invoiced, we'll wait for the orderID to appear in the INVOICED_REQUEST table to make sure it was successfully invoiced.
     */
    public void invoice(String orderID) throws Exception {
        int orderIDInt = Conversion.toInt(orderID);
        as400.invoice(orderIDInt);

        //Verify if the order has been added to the invoice request on LABOWEB.INVOICED_REQUEST
        String orderIDPadded = Conversion.padWithLeadingZeros(orderID);
        JSONObject orderState = capi.getOrderID(orderIDPadded);
        Boolean isInvoiced = orderState.getJSONObject("order").getBoolean("invoiced");

        long startTime = System.currentTimeMillis();
        long currentTime = startTime;

        while (currentTime < startTime + 120000 && isInvoiced == false) {
            currentTime = System.currentTimeMillis();
            orderState =  capi.getOrderID(orderIDPadded);
            isInvoiced = orderState.getJSONObject("order").getBoolean("invoiced");
        }

        if(isInvoiced==false){
            throw new RuntimeException("Failed invoicing the order");
        }

    }

    /**
     * Send results via mail, HL7, and to the output folder
     */
    public void sendResults(String orderID, String institute, String mailAddress) throws Exception {
        int orderIDInt = Conversion.toInt(orderID);
        as400.sendResults(orderIDInt, institute, mailAddress);
    }

    /**
     * Connect to the AS400 via Telnet and get patient details
     */
    public JSONObject getPatientDetails(String orderID, int patientID) throws Exception {
        JSONObject details = as400.getPatientDetails(orderID, patientID);
        return details;
    }

    /***
     * For a given patient ID, delete specific properties like AHV, AHO, INS,..
     * @param orderID
     * @param patientID
     * @throws Exception
     */
    public void deletePropertiesForPatient(String orderID, int patientID, List<String> propertiesToDelete) throws Exception {
        int orderIDInt = Conversion.toInt(orderID);
        as400.deletePropertiesForPatient(orderIDInt, patientID, propertiesToDelete);
    }

    /***
     * For a given patient ID, enter specific properties directly into lab400 (Like i.e. AHV number)
     * @param orderID
     * @param patientID
     * @param propertiesToEnter
     * @throws Exception
     */
    public void enterPropertiesForPatient(String orderID, int patientID, HashMap<String, String> propertiesToEnter) throws Exception {
        int orderIDInt = Conversion.toInt(orderID);
        as400.enterPropertiesForPatient(orderIDInt, patientID, propertiesToEnter);
    }


    /**
     * Connect to the AS400 via Telnet and perform a partial ORDVAL for only the given list of materials
     */
    public void partialOrdVal(String orderID, List<String> materials) throws Exception {
        int orderIDInt = Conversion.toInt(orderID);
        as400.partialOrdVal(orderIDInt, materials);
    }

    public String getRandomValidResult(int analyseID) {
        final JSONObject analyseDetails = db2SQL.getAnalyseDetails(analyseID);
        float lowerBoundary = analyseDetails.getFloat("lowerBoundary");
        float upperBoundary = analyseDetails.getFloat("upperBoundary");

        //20 is defined rather arbitrary. Might need additional logic on whether to return an integer or a float.
        if (upperBoundary - lowerBoundary > 20) {
            int randomValidResult = RandomGenerator.randomInt((int) lowerBoundary, (int) upperBoundary);
            return Integer.toString(randomValidResult);
        } else {
            float randomValidResult = RandomGenerator.randomFloat(lowerBoundary, upperBoundary);
            return Float.toString(randomValidResult);
        }

    }

    public int getChildAnalysisIDFromPOCTAnalysis(int POCT_ANALYSIS_ID, String childnameToLookFor) {
        //Loop over the available childs and filter out the one which matches the childname we're looking for.

        JSONArray POCTChilds = db2SQL.getUnderlyingPOCTAnalyses(POCT_ANALYSIS_ID);
        int analysisID = 0;
        Boolean childFound = false;
        for (Object child : POCTChilds) {
            JSONObject childJSON = (JSONObject) child;
            String analysisName = childJSON.getString("de_translation");
            if (analysisName.equalsIgnoreCase(childnameToLookFor)) {
                childFound = true;
                analysisID = childJSON.getInt("analyseID");
            }
        }

        Assert.assertTrue(childFound, "Could not find the child " + childnameToLookFor + " within the given analysis ID");
        return analysisID;
    }


    /**
     * Connect to the AS400 via Telnet and enter lab results
     */
    public void enterLabResults(int orderIDInt, int analyseID, String result) throws Exception {
        as400.enterLabResults(orderIDInt, analyseID, result);
    }

    /**
     * Connect to the AS400 via Telnet and enter lab results
     */
    public void enterLabResults(String orderID, int analyseID, String result) throws Exception {
        int orderIDInt = Conversion.toInt(orderID);
        as400.enterLabResults(orderIDInt, analyseID, result);
    }

    /**
     * Connect to the AS400 via Telnet and get lab results for a specific orderID
     */
    public JSONObject getLabResults(String orderID) throws Exception {
        return as400.getLabResults(orderID);
    }

    public JSONObject getOrderAdministration(String orderID) throws Exception {
        return as400.getOrderAdministration(orderID);
    }

    /**
     * Connect to the AS400 via Telnet and get mismatch list
     */
    public JSONObject getMismatchList(String orderID) throws Exception {
        return as400.getMismatchList(orderID);
    }

    /**
     * Connect to the AS400 via Telnet and perform 'put sample in serothek' sequence to the given the analyse name
     */
    public void putSampleInSerothek(String orderID, String analyseToPutInSerothek) throws Exception {
        as400.putSampleInSerothek(orderID, analyseToPutInSerothek);
    }

    /**
     * Connect to the AS400 via Telnet and enter lab results
     */
    public void changeSampleDate(String orderID, String newSampleDate) throws Exception {
        as400.changeSampleDate(orderID, newSampleDate);
    }

    /**
     * Connect to the AS400 via Telnet and set the PDF flag for a specific doctor
     */
    public void setPDFFlag(Integer doctorID, String flagValue) throws Exception {
        as400.setPDFFlag(doctorID, flagValue);
    }

    public void changePathologyProfileToPreviousImageVersion(int pathologyProfileID) {
        //First, get the current pathology profile sample which relates to the provided pathology profile ID
        JSONObject pathologyProfileSample;

        int failSafeCounter = 10;
        do {
            pathologyProfileSample = db2SQL.getPathologyProfileSample(pathologyProfileID);
            failSafeCounter--;
        } while (db2SQL.getPathologyProfileSample(pathologyProfileID) == null && failSafeCounter > 10);

        int imageVersionID = pathologyProfileSample.getInt("IMAGE_VERSION_ID");
        JSONObject imageVersion = db2SQL.getPathologyImageVersion(imageVersionID);
        String imageNameToGet = imageVersion.getString("IMAGE");
        int versionToGet = imageVersion.getInt("VERSION") - 1;
        int idOfPreviousVersion = db2SQL.getIDOfImageVersion(imageNameToGet, versionToGet);
        db2SQL.setImageVersionOfPathologyProfileSample(pathologyProfileID, idOfPreviousVersion);
        System.out.println("Image version downgraded for pathology profile ID " + pathologyProfileID);
        LOG.info("Image version downgraded for pathology profile ID " + pathologyProfileID);
    }


    public void createUpdateDeleteDoctor(String doctorNumber, String name, Map<String, String> updatedData, Runnable afterCreate, Runnable afterUpdate, Runnable afterDelete) throws Exception {
        as400.createUpdateDeleteDoctor(doctorNumber, name, updatedData, afterCreate, afterUpdate, afterDelete);
    }

    public void createNewDoctor(String doctorNumber, String name) throws Exception {
        as400.createNewDoctor(doctorNumber, name);
    }

    public void deleteADoctor(String doctorNumber) throws Exception {
        as400.deleteADoctor(doctorNumber);
    }

    public static final int DOCTOR_NUMBER_RANGE_START = 600000;
    public static final int DOCTOR_NUMBER_RANGE_END = 700000;

    public int findNewDoctorNumber() {
        for (int i = 0; i < 1000; i++) {
            int maybeDoctor = RandomGenerator.randomInt(DOCTOR_NUMBER_RANGE_START, DOCTOR_NUMBER_RANGE_END);
            if (db2SQL.getDoctorDetails(maybeDoctor) == null) {
                return maybeDoctor;
            }
        }

        throw new RuntimeException("No doctor number available");
    }

    public void updateActiveFlagOfAnalysis(int analysisId, boolean active){
        db2SQL.updateActiveFlagOfAnalysis(analysisId, active);
    }
}




