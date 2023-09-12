package ui.Connectors;


import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.json.JSONArray;
import org.json.JSONObject;
import ui.Helper.Conversion;
import ui.Helper.DateTime;
import ui.Helper.Helper;
import ui.Helper.Passwords;


public class DB2SQL {
    // Connect to your database.

    private Connection connection;


    private ComboPooledDataSource getDataSource() throws PropertyVetoException, IOException {


        String environment = Helper.getEnvironment();
        ;
        Properties environmentProperties = Helper.resolveEnvironment(environment);
        String url = environmentProperties.getProperty("url");
        String username = environmentProperties.getProperty("username");
        String password = Passwords.readPassword("AS400");
        String driver = environmentProperties.getProperty("driver");

        ComboPooledDataSource cpds = new ComboPooledDataSource();
        cpds.setJdbcUrl(url);
        cpds.setUser(username);
        cpds.setPassword(password);
        cpds.setDriverClass(driver);

        return cpds;
    }

    /***
     * Execute SQL statement
     * @param sql
     * @return resultSet as retrieved from SQL response
     */
    private ResultSet getResultSet(String sql) {

        PreparedStatement pstmt;
        ResultSet resultSet = null;

        try {
            if (connection == null) {
                ComboPooledDataSource dataSource = getDataSource();
                connection = dataSource.getConnection();
            }
            pstmt = connection.prepareStatement(sql);
            resultSet = pstmt.executeQuery();
        } catch (PropertyVetoException | IOException | SQLException e) {
            e.printStackTrace();
        }

        return resultSet;

    }

    /***
     * Insert SQL statement
     * @param sql
     */
    private void insertSQL(String sql) {

        PreparedStatement pstmt;

        try {
            if (connection == null) {
                ComboPooledDataSource dataSource = getDataSource();
                connection = dataSource.getConnection();
            }
            pstmt = connection.prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }


    }

    /***
     * Change doctor configuration setting. WARNING, this affects the entire order process flow!
     * @param doctorID
     * @param bestimmungRechn
     */
    public void setDoctorBestimmungRechn(int doctorID, String bestimmungRechn) {

        if (!bestimmungRechn.equals("A") && !bestimmungRechn.equals("P") && !bestimmungRechn.equals("G") && !bestimmungRechn.equals("X")) {
            throw new RuntimeException("Invalid bestimmingRechn provided! " + bestimmungRechn + " is not a valid state");
        }

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "UPDATE " + lab400scheme + ".DOKTER SET MTKD05 = '" + bestimmungRechn + "' WHERE GHNR05 = '" + doctorID + "'";

        insertSQL(SQL);
    }


    /***
     * Given a doctorID, get the full details of the doctor
     * @param doctorID
     * @return
     */
    public JSONObject getDoctorDetails(int doctorID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT GHNM05, GHVN05, GHAD05, MTKD05 FROM " + lab400scheme + ".DOKTER WHERE GHNR05=" + doctorID;

        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);

        if (results == null) {
            return null;
        }

        //Convert meaningless column names
        results.put("name", results.getString("GHNM05").trim());
        results.put("firstName", results.getString("GHVN05").trim());
        results.put("Address", results.getString("GHAD05").trim());
        results.put("Destination", results.getString("MTKD05").trim());

        return results;
    }

    /***
     * Given a patientID, get the full details of the patient
     * @param patientID
     * @return
     */
    public JSONObject getPatientDetails(int patientID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT * FROM " + lab400scheme + ".PATIEN WHERE \"NANR01\"='" + patientID + "'";

        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);

        enrichPatientData(results);

        return results;
    }

    /***
     * Given a HIS ID, get the full details of the underlying patient
     * @param HISID
     * @return
     */
    public JSONObject getHISPatientData(String HISID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT A.* FROM " + lab400scheme + ".PATIEN A WHERE A.NANR01 IN (select NANR02 from " + lab400scheme + ".OPNAME B WHERE B.OPNR02 = '" + HISID + "' LIMIT 1)";

        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);

        results = enrichPatientData(results);

        return results;

    }

    /***
     * Get setting on the babyprocessing flag
     * @param hospitalID
     * @return True or False (Baby processing enabled or disabled)
     */
    public Boolean getBabyFlagSetting(int hospitalID) {

        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT HOSPITAL_ID FROM " + vconsultscheme + ".HOSPITAL_ENABLED_FEATURE WHERE FEATURE = 'BABY_PROCESS' AND HOSPITAL_ID = '" + hospitalID + "'";

        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);

        if (results == null) {
            return false;
        } else {
            return true;
        }

    }

    /***
     * Enter new admission record entry via SQL.
     * @param admission
     * @param patientID
     * @param hospitalID
     */
    public void setAdmissionEntry(String admission, int patientID, int hospitalID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "INSERT INTO " + lab400scheme + ".OPNAME (\"OPNR02\", \"NANR02\", \"INST02\") VALUES ('" + admission + "', '" + patientID + "', '" + hospitalID + "')";

        insertSQL(SQL);
    }

    /***
     * Update existing admission record entry via SQL.
     * @param admission
     * @param patientID
     * @param hospitalID
     */
    public void updateAdmissionEntry(String admission, int patientID, int hospitalID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "UPDATE " + lab400scheme + ".OPNAME SET NANR02 = '" + patientID + "'  WHERE  OPNR02 = '" + admission + "' AND INST02 = '" + hospitalID + "'";
        insertSQL(SQL);
    }

    /***
     * Get STRING list of admission recordsL
     * @param admission
     * @param hospitalID
     * @return list of patientIDs
     */
    public List getAdmissionEntries(String admission, int hospitalID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT NANR02 FROM " + lab400scheme + ".OPNAME  WHERE \"OPNR02\" = '" + admission + "' AND \"INST02\" = '" + hospitalID + "'";

        ResultSet resultSet = getResultSet(SQL);

        List patients = resultsetToIntegerList(resultSet);

        return patients;
    }

    /***
     * Get STRING list of all analyse codes as they belong to a specific analysis ID
     * @param analyseID
     * @return
     */
    public List getAnalyseCodes(int analyseID) {

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT DIVC FROM " + lab400scheme + ".ANLDVC WHERE TENR = '" + analyseID + "'";

        ResultSet resultSet = getResultSet(SQL);
        return resultsetToStringList(resultSet);
    }

    /***
     * Get STRING list of all analyses which have a specific analysis code set.
     * @param code
     * @return
     */
    public List getAnalysesWithCode(String code) {

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT TENR FROM " + lab400scheme + ".ANLDVC WHERE DIVC = '" + code + "'";

        ResultSet resultSet = getResultSet(SQL);
        return resultsetToIntegerList(resultSet);
    }

    /***
     * Enter new analyse code for specific analyse via SQL.
     * @param analyseID
     * @param code
     */
    public void setCodeForAnalyseIDIfNotPresentYet(int analyseID, String code) {
        List analyses = getAnalysesWithCode(code);
        if (!analyses.contains(analyseID)) {
            String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
            String SQL = "INSERT INTO " + lab400scheme + ".ANLDVC (\"TENR\", \"DIVC\") VALUES ('" + analyseID + "', '" + code + "')";
            insertSQL(SQL);
        }
    }


    /***
     * SQL query to search for all patients which have an AHV number set
     * @return
     */
    public List getAllTestPatients(int doctorID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");

        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.PTVN01 != '' and A.PTAR01 != '' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' AND A.GBDT01 != 0 AND A.GESL01 != '9' AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";

        ResultSet resultSet = getResultSet(SQL);
        return resultsetToIntegerList(resultSet);
    }

    /***
     * SQL query to search for all patients without a birthdate or gender.
     * @return
     * @throws SQLException
     */
    public List getAllPatientsWithoutBirthDateOrGender(int doctorID) {
        //Also filter out any patients that have an AHV number set, as this is not realistic for patients without a birthdate

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.NANR01 not in (select ADNR01 from " + lab400scheme + ".PATXID B WHERE B.ADNR01 = C.ADNR01 and B.ASFA01 = 'AHV')\n" +
                "  AND A.PTVN01 != '' and A.PTAR01 != '' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' and A.GBDT01 = 0 and A.GESL01 = '9'    AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";

        ResultSet resultSet = getResultSet(SQL);

        List patients = resultsetToIntegerList(resultSet);

        return patients;
    }

    /***
     * SQL query to search for all patients with a birthdate but without any gender.
     * @return
     */
    public List getAllPatientsWithBirthDateWithoutGender(int doctorID) {
        //Also filter out any patients that have an AHV number set, as this is not realistic for patients without a birthdate
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.NANR01 not in (select ADNR01 from " + lab400scheme + ".PATXID B WHERE B.ADNR01 = C.ADNR01 and B.ASFA01 = 'AHV' )\n" +
                "  AND A.PTVN01 != '' and A.PTAR01 != '' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' and A.GBDT01 != 0 and A.GESL01 != '9'    AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";

        ResultSet resultSet = getResultSet(SQL);

        List patients = resultsetToIntegerList(resultSet);

        return patients;
    }

    /***
     * SQL query to search for all patients with genre male.
     * @return
     */
    public List getAllMalePatient(int doctorID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");

        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.PTVN01 != '' and A.PTAR01 != '' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' AND A.GBDT01 != 0 AND A.GESL01 = '1' AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";

        ResultSet resultSet = getResultSet(SQL);
        return resultsetToIntegerList(resultSet);
    }

    /***
     * SQL query to search for all patients without a birthdate but with a gender.
     * @return
     */
    public List getAllPatientsWithoutBirthDateWithGender(int doctorID) {
        //Also filter out any patients that have an AHV number set, as this is not realistic for patients without a birthdate

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");

        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.NANR01 not in (select ADNR01 from " + lab400scheme + ".PATXID B WHERE B.ADNR01 = C.ADNR01 and B.ASFA01 = 'AHV' )\n" +
                "  AND A.PTVN01 != '' and A.PTAR01 != '' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' and A.GBDT01 = 0 and A.GESL01 != '9'    AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";

        ResultSet resultSet = getResultSet(SQL);

        List patients = resultsetToIntegerList(resultSet);

        return patients;
    }

    /***
     * SQL query to search for all patients which have an AHV number set
     * @return
     */
    public List getAllPatientsWithValidAHVNumber(int doctorID) {
        //Filter out patients which are good to go to be used in HIS linking
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");

        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.NANR01 in (select ADNR01 from " + lab400scheme + ".PATXID B WHERE B.ADNR01 = C.ADNR01 and B.ASFA01 = 'AHV' )\n" +
                "  AND A.PTVN01 != '' and A.PTAR01 != '' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' AND A.GESL01 != '9' AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";


        ResultSet resultSet = getResultSet(SQL);
        return resultsetToIntegerList(resultSet);
    }

    /***
     * SQL query to search for all patients which have an INS number set
     * @return
     */
    public List getAllPatientsWithValidINSNumber(int doctorID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.NANR01 in (select ADNR01 from " + lab400scheme + ".PATXID B WHERE B.ADNR01 = C.ADNR01 and B.ASFA01 = 'INS' AND B.XPID01 like '80%' )\n" +
                "  AND A.PTVN01 != '' and A.PTAR01 != '' AND A.GBDT01 != 0 and A.GESL01 != '9' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";

        ResultSet resultSet = getResultSet(SQL);
        return resultsetToIntegerList(resultSet);
    }

    /***
     * SQL query to search for all patients which don't have an AHV or SSN number set
     * @return
     */
    public List getAllPatientsWithoutValidAHVOrINSNumber(int doctorID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.NANR01 not in (select ADNR01 from " + lab400scheme + ".PATXID B WHERE B.ADNR01 = C.ADNR01 AND (B.ASFA01 = 'AHV' OR B.ASFA01 = 'INS' OR B.ASFA01 = 'SSN' ))\n" +
                "  AND A.PTVN01 != '' and A.PTAR01 != '' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' AND A.GBDT01 != 0 AND A.GESL01 != '9' AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";

        ResultSet resultSet = getResultSet(SQL);
        return resultsetToIntegerList(resultSet);
    }

    /***
     * SQL query to search for all patients without any email address set
     * @return
     */
    public List getAllPatientsWithoutEmailAddress(int doctorID) {

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.PTVN01 != '' and A.PTAR01 != '' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' and A.GBDT01 != 0 and A.GESL01 != '9' and A.MAIL01 = ''    AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";

        ResultSet resultSet = getResultSet(SQL);

        List patients = resultsetToIntegerList(resultSet);

        return patients;
    }

    /***
     * SQL query to search for all patients with an email address set
     * @return
     */
    public List getAllPatientsWithEmailAddress(int doctorID) {

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT A.NANR01\n" +
                "FROM " + lab400scheme + ".PATIEN A\n" +
                "         inner join " + lab400scheme + ".PATCTX C on A.NANR01 = C.NANR01\n" +
                "         inner join " + lab400scheme + ".DOKPAT D on A.NANR01 = D.NANR\n" +
                "WHERE A.PTVN01 != '' and A.PTAR01 != '' AND A.PTAD01 != '' AND A.POST01 != '' AND A.PTWP01 != '' and A.GBDT01 != 0 and A.GESL01 != '9'  and A.MAIL01 != ''    AND GHNR = '" + doctorID + "' AND A.PTNM01 LIKE '%AUTOTESTPATIENT%'";

        ResultSet resultSet = getResultSet(SQL);

        List patients = resultsetToIntegerList(resultSet);

        return patients;
    }

    /***
     * Enrich the returned results of a patient with meaningful columns.
     * @param results
     * @return
     */
    private JSONObject enrichPatientData(JSONObject results) {

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");

        //Convert meaningless column names
        results.put("Name", results.getString("PTNB01").trim());
        results.put("FirstName", results.getString("PTVB01").trim());
        results.put("VioNr", Integer.parseInt(results.getString("NANR01").trim()));
        results.put("Address", results.getString("PTAD01").trim());
        results.put("ZIP", results.getString("POST01").trim());
        results.put("City", results.getString("PTWP01").trim());
        results.put("Birthdate", results.getInt("GBDT01"));
        results.put("CareOf", results.getString("NMPB01").trim());
        results.put("Country", results.getString("LDKD01").trim());
        results.put("MobileNr", results.getString("GSMN01").trim());
        results.put("Email", results.getString("MAIL01").trim());

        if (results.getInt("GESL01") == 1) results.put("Gender", "M");
        if (results.getInt("GESL01") == 0) results.put("Gender", "F");
        if (results.getInt("GESL01") == 9) results.put("Gender", "Unknown");

        String SQL = "SELECT * FROM " + lab400scheme + ".PATXID WHERE \"ADNR01\"='" + results.getString("NANR01").trim() + "' ";

        ResultSet resultSet = getResultSet(SQL);
        JSONArray json;
        json = resultsetToJSONArray(resultSet);

        long previousEpoch = 0;
        for (int i = 0; i < json.length(); i++) {
            String type = json.getJSONObject(i).getString("ASFA01").trim();
            String value = json.getJSONObject(i).getString("XPID01").trim();
            String date = json.getJSONObject(i).getBigDecimal("UPDT01").toString();
            String time = json.getJSONObject(i).getBigDecimal("UPTM01").toString();
            //Make sure time string consists of 4 digits.
            for (int padder = time.length(); padder < 4; padder++) {
                time = "0" + time;
            }
            Long epochValue = DateTime.dateTimeToEpoch(date + time, "yyyyMMddHHmm");


            //Should only keep the latest entered INS nr. There might be multiple in it, but we should store the most recent date only
            if (type.equals("INS")) {
                if (previousEpoch < epochValue) {
                    results.put(type, value);
                    previousEpoch = epochValue;
                }
            } else {
                results.put(type, value);
            }


        }

        return results;
    }

    /***
     * Get order details
     * @param orderID
     * @return
     */
    public JSONObject getOrderDetails(int orderID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT * FROM " + lab400scheme + ".ORDHDR as ORDERS INNER JOIN " + lab400scheme + ".ORDRCP AS REL ON REL.RQID = ORDERS.RQID WHERE \"ORDERS\".RQID='" + Conversion.padWithLeadingZeros(orderID) + "'";
        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);
        return results;
    }

    /***
     * Get STRING list of all material that should be included for a given orderID
     * @param orderID
     * @return
     */
    public List getSendMaterial(int orderID, String languageKey) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String singleLetterKey = "";
        switch (languageKey.toLowerCase()) {
            case "de":
                singleLetterKey = "D";
                break;
            case "fr":
                singleLetterKey = "F";
                break;
            case "it":
                singleLetterKey = "I";
                break;
            case "en":
                singleLetterKey = "E";
                break;
            default:
                throw new RuntimeException(languageKey + " is not a valid languagekey");
        }

        String SQL = "SELECT A.OMSC52 FROM " + lab400scheme + ".RECEPIOMS A WHERE A.TAKD52 = '" + singleLetterKey + "' AND A.RCKD52 in (SELECT B.RCKD FROM " + lab400scheme + ".ORDRCP B WHERE B.RQID = '" + Conversion.padWithLeadingZeros(orderID) + "' ) ";

        ResultSet resultSet = getResultSet(SQL);

        List materials = resultsetToStringList(resultSet);

        return materials;
    }

    /***
     * Get STRING list of all material that should be included for a given orderID
     * @param orderID
     * @return
     */
    public List getSendMaterial(String orderID, String languageKey) {
        return getSendMaterial(Conversion.toInt(orderID), languageKey);
    }

    /***
     * Get details belonging to a specific analysisID
     * @param analyseID
     * @return JSONObject
     */
    public JSONObject getAnalyseDetails(int analyseID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT * FROM " + lab400scheme + ".ANLDTA WHERE \"TENR07\"='" + analyseID + "'";

        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);

        //Convert meaningless column names

        if (results.getFloat("SOLM07") != 0 || results.getFloat("SBLM07") != 0) {
            results.put("lowerBoundary", results.getFloat("SOLM07"));
            results.put("upperBoundary", results.getFloat("SBLM07"));
        }

        if (results.getFloat("ONLM07") != 0 || results.getFloat("BVLM07") != 0) {
            results.put("lowerBoundary", results.getFloat("ONLM07"));
            results.put("upperBoundary", results.getFloat("BVLM07"));
        }

        results.put("de_translation", results.getString("OMS001").trim());
        results.put("fr_translation", results.getString("OMS002").trim());
        results.put("it_translation", results.getString("OMS003").trim());
        results.put("en_translation", results.getString("OMS001").trim());
        results.put("analyseID", results.getBigDecimal("TENR07").intValue());
        results.put("analyseCode", results.getString("NMAF07").trim());

        results.put("units", results.getString("EENH07").trim());
        results.put("range", results.getString("LIM001").trim());

        return results;
    }

    /***
     * Get details on a MB template
     * @param templateID
     * @return
     */
    public JSONObject getMicroBiologyTemplateDetails(int templateID) {

        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT A.* FROM " + vconsultscheme + ".MB_TRANSLATION A WHERE A.ID IN (select B.NAME_TRANSLATION_ID FROM " + vconsultscheme + ".MB_TEMPLATE B WHERE B.ID = '" + templateID + "')";

        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);

        results.put("de_translation", results.getString("DE").trim());
        results.put("fr_translation", results.getString("FR").trim());
        results.put("it_translation", results.getString("IT").trim());
        results.put("en_translation", results.getString("EN").trim());

        return results;
    }


    /***
     * Convert a resultset to JSON
     * @param resultSet
     * @return
     */
    private JSONArray resultsetToJSONArray(ResultSet resultSet) {

        JSONArray json = new JSONArray();
        try {

            ResultSetMetaData rsmd = resultSet.getMetaData();
            while (resultSet.next()) {
                int numColumns = rsmd.getColumnCount();
                JSONObject obj = new JSONObject();
                for (int i = 1; i <= numColumns; i++) {
                    String column_name = rsmd.getColumnName(i);
                    Object value = resultSet.getObject(column_name);
                    obj.put(column_name, value);
                }
                json.put(obj);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return json;
    }


    /***
     * Convert a received resultset to a list of integers
     * @param resultSet
     * @return List of integers
     */
    private List resultsetToIntegerList(ResultSet resultSet) {

        List<Integer> result = new ArrayList<>();
        try {
            while (resultSet.next()) {
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    result.add(Integer.parseInt(resultSet.getObject(i).toString().trim()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /***
     * Convert a received resultset to a single integer. If none can be found, return -1
     * @param resultSet
     * @return List of integers
     */
    private int resultsetToInteger(ResultSet resultSet) {

        try {
            while (resultSet.next()) {
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    return Integer.parseInt(resultSet.getObject(i).toString().trim());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;

    }

    /***
     * Convert a received resultset to a list of strings
     * @param resultSet
     * @return List of strings
     */
    private List resultsetToStringList(ResultSet resultSet) {

        List<String> result = new ArrayList<>();

        try {
            while (resultSet.next()) {
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    result.add(resultSet.getObject(i).toString().trim());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /***
     * Convert a received resultset to a JSON object
     * @param resultSet
     * @return List of strings
     */
    private JSONObject resultsetToJSONObject(ResultSet resultSet) {

        JSONArray json;
        json = resultsetToJSONArray(resultSet);

        JSONObject results = null;
        if (json.length() > 0) {
            results = json.getJSONObject(0);
        }

        return results;
    }

    /***
     * Get an INT list of all enabled analyses IDs for a given MB template
     * @param microBiologyTemplateID
     * @return
     */
    public List getDefaultEnabledAnalysesForMBTemplate(int microBiologyTemplateID) {

        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT A.TENR FROM " + vconsultscheme + ".MB_ANALYSIS A WHERE A.ID IN (select B.ANALYSIS_ID from " + vconsultscheme + ".MB_TEMPLATE_ANALYSIS B WHERE B.TEMPLATE_ID = " + microBiologyTemplateID + ")";

        ResultSet resultSet = getResultSet(SQL);

        List<Integer> result = resultsetToIntegerList(resultSet);

        return result;
    }

    /***
     * Get an INT list of all available analyses IDs for a given MB template
     * @param microBiologyTemplateID
     * @return
     */
    public List getAvailableAnalysesForMBTemplate(int microBiologyTemplateID) {

        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT A.TENR FROM " + vconsultscheme + ".MB_ANALYSIS A WHERE A.MATERIAL_ID IN (select B.MATERIAL_ID from " + vconsultscheme + ".MB_TEMPLATE B WHERE B.ID = " + microBiologyTemplateID + ") ORDER BY SORTORDER ASC\n";

        ResultSet resultSet = getResultSet(SQL);

        List<Integer> result = resultsetToIntegerList(resultSet);

        return result;
    }

    /***
     * Get an INT list of all available analyses IDs for a given pathology image
     * @param pathologyImageName
     * @return
     */
    public List getAvailableAnalysesForPathologyImage(String pathologyImageName) {

        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT B.ID FROM " + vconsultscheme + ".PATHOLOGY_MATERIAL B WHERE B.DEPRECATED = 'N' AND PATHOLOGY_DOMAIN = 'PATHOLOGY' AND B.ID IN (SELECT C.PATHOLOGY_MATERIAL_ID FROM " + vconsultscheme + ".PATHOLOGY_IMAGE_MAPPING C WHERE C.PATHOLOGY_IMAGE_ID IN (SELECT D.ID FROM " + vconsultscheme + ".PATHOLOGY_IMAGE D WHERE D.IMAGE = '" + pathologyImageName + "') )";

        ResultSet resultSet = getResultSet(SQL);

        List<Integer> result = resultsetToIntegerList(resultSet);

        return result;
    }

    /***
     * Get an INT list of all child analyses IDs belonging to a given POCT
     * @param poctID
     * @return
     */
    public JSONArray getUnderlyingPOCTAnalyses(int poctID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "SELECT TENR08 FROM " + lab400scheme + ".ANLBTA WHERE \"BTNR08\"='" + poctID + "'";

        JSONArray analyses = new JSONArray();

        ResultSet resultSet = getResultSet(SQL);

        List<Integer> list = resultsetToIntegerList(resultSet);

        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                JSONObject analyseDetails;
                analyseDetails = getAnalyseDetails(list.get(i));
                analyses.put(analyseDetails);
            }
        } else if (list.size() == 0) {
            //No childs available? Then the poct ID itself is the analysis we should check
            JSONObject analyseDetails;
            analyseDetails = getAnalyseDetails(poctID);
            analyses.put(analyseDetails);
        }

        return analyses;
    }


    /***
     * Get an INT list of all child analyses IDs belonging to a given profile
     * @param profileID
     * @return
     */
    public List getProfileRoutineAnalyses(int profileID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT A.TENR_REF FROM " + vconsultscheme + ".PROFILE_CONTENT A WHERE A.PROFILE_ID IN (select B.ID from " + vconsultscheme + ".PROFILE B WHERE B.ID = '" + profileID + "')";

        ResultSet resultSet = getResultSet(SQL);

        List<Integer> list = resultsetToIntegerList(resultSet);

        return list;
    }

    /***
     * Get an INT list of all child microbiology template IDs belonging to a given profile
     * @param profileID
     * @return
     */
    public List getProfileMicroBiologyTemplates(int profileID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT A.MB_TEMPLATE_ID FROM " + vconsultscheme + ".PROFILE_CONTENT_TEMPLATE A WHERE A.PROFILE_ID IN (select B.ID from " + vconsultscheme + ".PROFILE B WHERE B.ID = '" + profileID + "')";

        ResultSet resultSet = getResultSet(SQL);

        List<Integer> list = resultsetToIntegerList(resultSet);

        return list;
    }


    /***
     * Get a JSON array of all available profiles for a given doctor ID
     * @param doctorID
     * @return
     */
    public JSONArray getAvailableProfilesForDoctorID(int doctorID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT * FROM " + vconsultscheme + ".PROFILE WHERE KEY = '" + doctorID + "' ORDER BY SORTORDER ASC";

        ResultSet resultSet = getResultSet(SQL);

        JSONArray list;
        list = resultsetToJSONArray(resultSet);

        return list;
    }

    /***
     * Get a JSON array of all available profiles for a given doctor ID
     * @param doctorID
     * @return
     */
    public JSONArray getAvailablePOCTProfilesForDoctorID(int doctorID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT * FROM " + vconsultscheme + ".PROFILE WHERE KEY = '" + doctorID + "' and DISCRIMINATOR = 'INTERNAL' ORDER BY SORTORDER ASC";

        ResultSet resultSet = getResultSet(SQL);

        JSONArray list;
        list = resultsetToJSONArray(resultSet);

        return list;
    }

    /***
     * Given the ID of a POCT profile, find the related profile name
     * @param profileID
     * @param doctorID
     * @return
     */
    public String getProfileNameOfPOCTProfileID(int profileID, int doctorID) {
        JSONArray availablePOCTProfilesOnLab400 = getAvailablePOCTProfilesForDoctorID(doctorID);
        String profileName = null;
        for (int i = 0; i < availablePOCTProfilesOnLab400.length(); i++) {
            int id = availablePOCTProfilesOnLab400.getJSONObject(i).getInt("ID");

            if (id == profileID) {
                profileName = availablePOCTProfilesOnLab400.getJSONObject(i).getString("NAME");
                i = availablePOCTProfilesOnLab400.length();
            }
        }
        return profileName;
    }

    /***
     * Given the Name of a POCT profile, find the related profile ID
     * @param profileName
     * @param doctorID
     * @return
     */
    public int getProfileIDOfPOCTProfileName(String profileName, int doctorID) {
        JSONArray availablePOCTProfilesOnLab400 = getAvailablePOCTProfilesForDoctorID(doctorID);
        int profileID = 0;
        for (int i = 0; i < availablePOCTProfilesOnLab400.length(); i++) {
            int id = availablePOCTProfilesOnLab400.getJSONObject(i).getInt("ID");
            String profileNameLab400 = availablePOCTProfilesOnLab400.getJSONObject(i).getString("NAME");
            if (profileNameLab400.equals(profileName)) {
                profileID = availablePOCTProfilesOnLab400.getJSONObject(i).getInt("ID");
                i = availablePOCTProfilesOnLab400.length();
            }
        }
        return profileID;
    }


    /***
     * Get all translations for a given language key (DE, EN, FR or IT)
     * @param languageKey
     * @return
     */
    public HashMap getTranslations(String languageKey) {

        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "select KEY,VALUE from (" +
                "                SELECT RESOURCE_KEY AS KEY, '" + languageKey + "' AS LOCALE, " + languageKey + "_VALUE AS VALUE\n" +
                "                FROM " + vconsultscheme + ".LABELS_VIEW)";

        ResultSet resultSet = getResultSet(SQL);
        HashMap mapping = new HashMap<>();

        try {
            while (resultSet.next()) {
                mapping.put(resultSet.getString(1), resultSet.getString(2));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed fetching translations for language key '" + languageKey + "'");
        }

        return mapping;
    }


    public HashMap<String, String> getTranslationForSpecificKeys(String languageKey, List<String> keys) {

        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "select KEY,VALUE from (" +
                "                SELECT RESOURCE_KEY AS KEY, '" + languageKey + "' AS LOCALE, " + languageKey + "_VALUE AS VALUE\n" +
                "                FROM " + vconsultscheme + ".LABELS_VIEW) WHERE ";

        for (int i = 0; i < keys.size(); i++) {
            SQL = SQL + " KEY = " + "'" + keys.get(i) + "'";
            if (i + 1 < keys.size()) SQL = SQL + " OR ";
        }


        ResultSet resultSet = getResultSet(SQL);
        HashMap<String, String> mapping = new HashMap<>();

        try {
            while (resultSet.next()) {
                mapping.put(resultSet.getString(1), resultSet.getString(2));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed fetching translations for language key '" + languageKey + "'");
        }

        return mapping;
    }


    //Medical maps

    public List getAvailableMedicalMapsKeywords(String languageKey) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT KEYWORD_NAME_" + languageKey + " FROM " + vconsultscheme + ".BO_MEDICAL_MAP_KEYWORDS";

        ResultSet resultSet = getResultSet(SQL);

        List<Integer> list = resultsetToStringList(resultSet);

        return list;
    }

    /***
     * Get medical mapping ID via SQL. In case it cannot be found, return -1;
     * @param languageKey
     * @param keyword
     * @return
     */
    public int getMedicalMappingID(String languageKey, String keyword) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT MAP_ID FROM " + vconsultscheme + ".BO_MEDICAL_MAPS WHERE MAP_NAME_" + languageKey.toUpperCase() + " = '" + keyword + languageKey.toUpperCase() + "' ORDER BY 'MAP_ID' DESC LIMIT 1";

        ResultSet resultSet = getResultSet(SQL);

        int mappingID = resultsetToInteger(resultSet);

        return mappingID;
    }

    /***
     * Get medical mapping details via SQL. In case it cannot be found, return -1;
     * @return
     */
    public JSONObject getMedicalMappingDetails(int medicalMappingID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        //String SQLold = "SELECT * FROM " + vconsultscheme + ".BO_MEDICAL_MAPS WHERE MAP_ID = '" + medicalMappingID + "' LIMIT 1";
        String SQL = "SELECT * FROM " + vconsultscheme + ".BO_MEDICAL_MAP_KEYWORDS INNER JOIN " + vconsultscheme + ".BO_MEDICAL_MAPS ON " + vconsultscheme + ".BO_MEDICAL_MAP_KEYWORDS.MAP_ID = " + vconsultscheme + ".BO_MEDICAL_MAPS.MAP_ID WHERE " + vconsultscheme + ".BO_MEDICAL_MAPS.MAP_ID ='" + medicalMappingID + "'";
        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);

        return results;
    }

    /***
     * Get medical mapping keyword details via SQL. In case it cannot be found, return -1;
     * @return
     */
    public JSONObject getMedicalMappingKeywordDetails(int medicalMappingID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT * FROM " + vconsultscheme + ".BO_MEDICAL_MAP_KEYWORDS WHERE MAP_ID = '" + medicalMappingID + "' LIMIT 1";

        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);

        return results;
    }

    /***
     * Get list of all analyses which are currently enabled for a given POCT device
     * @param poctProfileID
     * @param doctorID
     * @return
     */
    public List<Integer> getIncludedAnalysesForPOCTDevice(int poctProfileID, int doctorID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT ANALYSIS_ID FROM " + vconsultscheme + ".INTERNAL_ANALYSIS WHERE CONTAINER_REFERENCE = '" + poctProfileID + "' and KEY = '" + doctorID + "'";
        ResultSet resultSet = getResultSet(SQL);
        List<Integer> list = resultsetToIntegerList(resultSet);
        return list;
    }

    /***
     * Get list of all analyses which are AVAILABLE for a given POCT device
     * @param poctProfileID
     * @return
     */
    public List<Integer> getAvailableAnalysesForPOCTDevice(int poctProfileID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT ANALYSES FROM " + vconsultscheme + ".CONTAINER WHERE ID = '" + poctProfileID + "'";
        ResultSet resultSet = getResultSet(SQL);

        String analyseString = "";
        List stringList = resultsetToStringList(resultSet);
        if (stringList.get(0) != null) {
            analyseString = stringList.get(0).toString();
        }
        List<String> allAnalyses = List.of(analyseString.split(","));

        List<Integer> listToReturn = new ArrayList<>();

        for (String s : allAnalyses) {
            if (!s.equals("")) listToReturn.add(Integer.valueOf(s));
        }

        return listToReturn;
    }

    /***
     * Get list of all available POCT containers (devices)
     * @return
     */
    public JSONArray getAvailablePOCTDevicesAndAnalyses() {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT * FROM " + vconsultscheme + ".CONTAINER WHERE POCT = 'Y'";
        ResultSet resultSet = getResultSet(SQL);
        JSONArray poctDevices = resultsetToJSONArray(resultSet);
        return poctDevices;
    }

    /***
     * Get list of all available POCT containers (devices)
     * @return
     */
    public String getPOCTDeviceName(int deviceID, String languageKey) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT " + languageKey.toUpperCase() + " FROM " + vconsultscheme + ".CONTAINER WHERE ID = '" + deviceID + "'";
        ResultSet resultSet = getResultSet(SQL);
        String deviceName = resultsetToStringList(resultSet).get(0).toString();
        return deviceName;
    }


    //// Settings per doctor
    public String getStringDoctorSetting(int doctorID, String settingType) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT SETTING_VALUE FROM " + vconsultscheme + ".SETTING WHERE DOCTOR_ID = '" + doctorID + "' and SETTING_TYPE = '" + settingType + "'";
        ResultSet resultSet = getResultSet(SQL);
        String settings = resultsetToStringList(resultSet).get(0).toString();
        return settings;
    }

    public void markOrderForCreationOfNewPatient(String orderID) {
        orderID = Conversion.padWithLeadingZeros(orderID);
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "UPDATE " + lab400scheme + ".ORDHDR SET FLG6 = 'Y' WHERE RQID='" + orderID + "'";

        insertSQL(SQL);

    }

    /***
     * When a new patient is created, we could use this method to get its related patient/vio number
     * @param patientName
     * @param patientFirstName
     * @param patientBirthdate
     * @return
     */
    public int getPatientIDOfNewPatient(String patientName, String patientFirstName, String patientBirthdate) {

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");

        patientBirthdate = DateTime.reformatDate(patientBirthdate, "dd.MM.yyyy", "yyyyMMdd");

        String SQL = "SELECT NANR01 FROM " + lab400scheme + ".PATIEN WHERE PTNM01 = '" + patientName + "' and PTVN01 = '" + patientFirstName + "' and GBDT01 = '" + patientBirthdate + "' ORDER BY UPDT01 DESC LIMIT 1";
        ResultSet resultSet = getResultSet(SQL);
        int patientID = resultsetToInteger(resultSet);

        return patientID;
    }

    public Boolean checkIfOrderIDIsInvoiced(String orderID) {

        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");

        String SQL = "SELECT COUNT(REQUEST_ID) from " + vconsultscheme + ".INVOICED_REQUEST WHERE REQUEST_ID = '" + orderID + "'";
        ;
        ResultSet resultSet = getResultSet(SQL);
        int count = resultsetToInteger(resultSet);

        if (count == 1) {
            return true;
        } else {
            return false;
        }
    }

    public JSONArray getPathologyComments(String orderID) {
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");

        String SQL = "SELECT * FROM " + lab400scheme + ".ORDHKM WHERE " + lab400scheme + ".ORDHKM.RQID ='" + Conversion.padWithLeadingZeros(orderID) + "' ORDER BY LNNR ASC";
        ResultSet resultSet = getResultSet(SQL);


        JSONArray results = resultsetToJSONArray(resultSet);
        return results;
    }

    public JSONArray getPathologyVolumes() {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT * from " + vconsultscheme + ".PATHOLOGY_MATERIAL_RECIPIENT_VOLUME ORDER BY PATHOLOGY_MATERIAL_ID ASC";
        ResultSet resultSet = getResultSet(SQL);
        JSONArray results = resultsetToJSONArray(resultSet);
        return results;
    }

    public List<String> getImagesWhichBelongToMaterialID(int materialID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT IMAGE " +
                "FROM " + vconsultscheme + ".PATHOLOGY_IMAGE_MAPPING " +
                " INNER JOIN " + vconsultscheme + ".PATHOLOGY_IMAGE ON " + vconsultscheme + ".PATHOLOGY_IMAGE_MAPPING.PATHOLOGY_IMAGE_ID = " + vconsultscheme + ".PATHOLOGY_IMAGE.ID " +
                " WHERE " + vconsultscheme + ".PATHOLOGY_IMAGE_MAPPING.PATHOLOGY_MATERIAL_ID = '" + materialID + "'";
        ResultSet resultSet = getResultSet(SQL);
        List<String> results = resultsetToStringList(resultSet);


        return results;
    }

    public JSONObject getPathologyProfileSample(int pathologyProfileID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT * FROM " + vconsultscheme + ".PATHOLOGY_PROFILE_SAMPLE WHERE PATHOLOGY_PROFILE_MODEL_ID = '" + pathologyProfileID + "' LIMIT 1";
        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);
        return results;
    }

    public JSONObject getPathologyImageVersion(int versionID) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT * FROM " + vconsultscheme + ".PATHOLOGY_IMAGE_VERSION WHERE ID = '" + versionID + "' LIMIT 1";
        ResultSet resultSet = getResultSet(SQL);
        JSONObject results = resultsetToJSONObject(resultSet);
        return results;
    }

    public int getIDOfImageVersion(String imageName, int version) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "SELECT ID FROM " + vconsultscheme + ".PATHOLOGY_IMAGE_VERSION WHERE IMAGE = '" + imageName + "' AND VERSION = '" + version + "' LIMIT 1";
        ResultSet resultSet = getResultSet(SQL);
        int id = resultsetToInteger(resultSet);

        return id;
    }

    public void setImageVersionOfPathologyProfileSample(int pathologyProfileID, int idToSet) {
        String vconsultscheme = Helper.getEnvironmentProperty("vconsultscheme");
        String SQL = "UPDATE " + vconsultscheme + ".PATHOLOGY_PROFILE_SAMPLE SET IMAGE_VERSION_ID = '" + idToSet + "' WHERE PATHOLOGY_PROFILE_MODEL_ID = '" + pathologyProfileID + "'";
        insertSQL(SQL);
    }

    public void updateActiveFlagOfAnalysis(int analysisID, boolean active) {
        String activeFlag = active ? "Y" : "N";
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "UPDATE " + lab400scheme + ".ANLDTA SET ACTI07 = '" + activeFlag + "' WHERE TENR07='" + analysisID + "'";

        insertSQL(SQL);
    }


    public boolean getReportFlagForAnalysis(int analysisNumber) throws SQLException {

        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "select ad.GRKD07 from " + lab400scheme + ".ANLDTA ad where ad.TENR07 = " + analysisNumber;
        ResultSet resultSet = getResultSet(SQL);
        resultSet.next();
        return resultSet.getString(1).equals("Y");
    }

    public void changeReportFlagForAnalysis(int analysisNumber, boolean currentReportFlagValue) {
        String newValue = currentReportFlagValue ? "N" : "Y";//invert the value
        String lab400scheme = Helper.getEnvironmentProperty("lab400scheme");
        String SQL = "UPDATE " + lab400scheme + ".ANLDTA SET GRKD07 ='" + newValue + "' WHERE TENR07=" + analysisNumber;
        insertSQL(SQL);
    }
}
