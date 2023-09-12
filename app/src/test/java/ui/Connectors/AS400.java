package ui.Connectors;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.util.TimeUtils;
import se.daan.as400automation.AbstractScript;
import se.daan.as400automation.Engine;
import se.daan.as400automation.Position;
import se.daan.as400automation.SearchCriterion;
import se.daan.as400automation.TimeoutException;
import se.daan.as400automation.engine.DefaultEngine;
import se.daan.as400automation.engine.EnginePlugin;
import se.daan.as400automation.engine.output.OutputPlugin;
import se.daan.as400automation.engine.record.RecordPlugin;
import ui.BaseTestThreading;
import ui.Helper.Conversion;
import ui.Helper.DateTime;
import ui.Helper.Helper;
import ui.Helper.Passwords;

public class AS400 {

  private static final Logger LOG = LoggerFactory.getLogger(AS400.class);

  public static long timeout = 10000;

  public AS400() {
  }

  public void ordVal(int orderID) throws Exception {
    LOG.info("Performing ORDVAL for order ID " + orderID);
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        engine.run(new OrdvalScript(lab400OrderID));
      } catch (Exception e) {
        throw e;
      }
    }
    LOG.info("\nORDVAL for order ID " + orderID + " is completed");
  }

  public void partialOrdVal(int orderID, List<String> materials) throws Exception {
    LOG.info("Performing ORDVAL for order ID " + orderID);
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        engine.run(new partialOrdvalScript(lab400OrderID, materials));
      } catch (Exception e) {
        throw e;
      }
    }
    LOG.info("\nORDVAL for order ID " + orderID + " is completed");
  }

  public void sendResults(int orderID, String instituteNumber, String mailAddress)
      throws Exception {
    LOG.info("Sending results for " + instituteNumber + "/" + orderID);
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        engine.run(new SendResultsScript(lab400OrderID, instituteNumber, mailAddress));
      } catch (Exception e) {
        throw e;
      }
    }
    LOG.info("\nFinished sending results for " + instituteNumber + "/" + orderID);
  }

  public void invoice(int orderID) throws Exception {
    LOG.info("Performing invoice action for order ID " + orderID);
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        engine.run(new InvoiceScript(lab400OrderID));
      } catch (Exception e) {
        throw e;
      }
    }
    LOG.info("\nInvoicing for order ID " + orderID + " is completed");
  }


  public void enterLabResults(int orderID, int analyseID, String result) throws Exception {
    LOG.info("Entering lab results for order ID " + orderID + " and analysis ID "
        + analyseID);
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        engine.run(new EnterResultScript(lab400OrderID, analyseID, result));
      } catch (Exception e) {
        throw e;
      }

    }
    LOG.info("\nEntering lab results for order ID " + orderID + " is completed.");

  }

  public void enterPropertiesForPatient(int orderID, int patientID,
      HashMap<String, String> propertiesToEnter) throws Exception {
    LOG.info("Entering patient details for patient ID " + patientID);
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        engine.run(new EnterPropertiesForPatient(lab400OrderID, patientID, propertiesToEnter));
      } catch (Exception e) {
        throw e;
      }

    }
  }


  public JSONObject getLabResults(String orderID) throws Exception {
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        GetLabResultScript script = new GetLabResultScript(lab400OrderID);
        engine.run(script);
        return script.analyseData;
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public JSONObject getOrderAdministration(String orderID) throws Exception {
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        GetOrderAdministrationScript script = new GetOrderAdministrationScript(lab400OrderID);
        engine.run(script);
        return script.orderAdm;
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public JSONObject getPatientDetails(String orderID, int patientID) throws Exception {
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        GetPatientDetails script = new GetPatientDetails(lab400OrderID, patientID);
        engine.run(script);
        return script.details;
      } catch (Exception e) {
        throw e;
      }

    }
  }

  public void deletePropertiesForPatient(int orderID, int patientID,
      List<String> propertiesToDelete) throws Exception {
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        engine.run(
            new DeletePropertiesForPatientScript(lab400OrderID, patientID, propertiesToDelete));
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public JSONObject getMismatchList(String orderID) throws Exception {
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        GetMismatchList script = new GetMismatchList(lab400OrderID);
        engine.run(script);
        return script.details;
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public void putSampleInSerothek(String orderID, String analyseToPutInSerothek) throws Exception {
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        PutSampleInSerothek script = new PutSampleInSerothek(lab400OrderID, analyseToPutInSerothek);
        engine.run(script);
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public void createUpdateDeleteDoctor(String doctorNumber, String name,
      Map<String, String> updatedData, Runnable afterCreate, Runnable afterUpdate,
      Runnable afterDelete) throws Exception {
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        CreateUpdateDeleteDoctorScript script = new CreateUpdateDeleteDoctorScript(doctorNumber,
            name, updatedData, afterCreate, afterUpdate, afterDelete);
        engine.run(script);
      }
    }
  }

  public void createNewDoctor(String doctorNumber, String name) throws Exception {
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        CreateNewDoctorScript script = new CreateNewDoctorScript(doctorNumber, name);
        engine.run(script);
      }
    }
  }

  public void deleteADoctor(String doctorNumber) throws Exception {
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        DeleteADoctorScript script = new DeleteADoctorScript(doctorNumber);
        engine.run(script);
      }
    }
  }

  public void changeSampleDate(String orderID, String newSampleDate) throws Exception {
    String lab400OrderID = Conversion.padWithLeadingZeros(orderID);
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        ChangeSampleDate script = new ChangeSampleDate(lab400OrderID, newSampleDate);
        engine.run(script);
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public void setPDFFlag(Integer doctorID, String flagValue) throws Exception {

    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new OutputPlugin(1, 1),
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        SetPDFFlag script = new SetPDFFlag(doctorID, flagValue);
        engine.run(script);
      } catch (Exception e) {
        throw e;
      }
    }
  }

  public void changePatientAddress(int vioNumber, String newAddress, String orderIdForResend)
      throws Exception {
    try (final Writer writer = FileWriter()) {
      final List<EnginePlugin> plugins = Arrays.asList(
          new RecordPlugin(writer)
      );

      try (Engine engine = new DefaultEngine(plugins)) {
        ChangePatientAddressScript script = new ChangePatientAddressScript(vioNumber, newAddress,
            orderIdForResend);
        engine.run(script);
      } catch (Exception e) {
        throw e;
      }
    }
  }

  //////////////////////////////////////////////////////////

  private Writer FileWriter() throws IOException {

    String directoryBase = Helper.resolveGlobalPath("as400TraceLocation");
    File directory = new File(directoryBase);
    if (!directory.exists()) {
      directory.mkdirs();
    }
    Helper helper = new Helper();
    return new FileWriter(directoryBase + helper.testCaseExecutionID()+".txt", StandardCharsets.UTF_8, true);
  }

  private static class OrdvalScript extends AbstractScript {

    private final String orderNumber;

    public OrdvalScript(String orderNumber) {
      this.orderNumber = orderNumber;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      fKey(9);
      SearchCriterion cmdInput = first(underlined().after(text("===>")).under(text("Command")));
      typeAt(cmdInput, "ORDVAL", timeout);
      enter();

      waitFor(text("Order validieren").and(text("== TUBE by TUBE ==")));
      SearchCriterion orderInput = first(underlined().after(text("Order / Tube")));
      typeAt(orderInput, orderNumber);
      enter();

      waitFor(text("Recipient Empfang"));
      enter();

      waitFor(text("Order validieren").and(text("== TUBE by TUBE ==")), timeout);

      screenshot();
      fKey(12);
      fKey(12);
    }
  }

  private static class SendResultsScript extends AbstractScript {

    private final String orderNumber;
    private final String instituteNumber;
    private final String mailAddress;

    public SendResultsScript(String orderNumber, String instituteNumber, String mailAddress) {
      this.orderNumber = orderNumber;
      this.instituteNumber = instituteNumber;
      this.mailAddress = mailAddress;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      fKey(9);
      SearchCriterion cmdInput = first(underlined().after(text("===>")).under(text("Command")));
      typeAt(cmdInput, "wrkopv");
      enter();
      waitFor(text("Daten aufrufen"));

      //Enter institute number
      SearchCriterion instituteInput = first(underlined().after(text("Labor/Einr.")));
      typeAt(instituteInput, instituteNumber);
      //Enter order number
      SearchCriterion orderNumberInput = first(underlined().after(text("Nummer")));
      typeAt(orderNumberInput, orderNumber);
      enter();
      waitFor(text("Analysen pro Probe").and(text("F23")));

      //Reactivate sending of the results
      screenshot();
      fKey(23);
      waitFor(text("Results were reactivated to be resent electronically"));
      screenshot();

      //Send mail
      fKey(4);
      waitFor(text("Bestimmung"));
      Position positionOfType = findNow(text("Type"));
      String type = readText(position(positionOfType.row() + 1, positionOfType.col()), 6).trim();
      Assert.assertEquals(type, "Mail");
      screenshot();
      underlined().under(text("Bestimmung"));
      typeAt(first(underlined().under(text("Mail"))), mailAddress);
      waitFor(text(mailAddress));
      screenshot();
      enter();
      screenshot();
      waitFor(cursor().before(text("Mail")));
      enter();
      screenshot();
      waitFor(text("Geburtsdatum"), timeout * 4);
      screenshot();

      //Export to file
      fKey(4);
      waitFor(text("Bestimmung"));
      fKey(1);
      waitFor(text("File").under(text("Type")));
      positionOfType = findNow(text("Type"));
      type = readText(position(positionOfType.row() + 1, positionOfType.col()), 6).trim();
      Assert.assertEquals(type, "File");
      screenshot();
      underlined().under(text("Bestimmung"));
      typeAt(first(underlined().under(text("File"))), "c:/export_imtf/");
      screenshot();
      enter();
      waitFor(cursor().before(text("File")));
      enter();
      waitFor(text("Geburtsdatum"), timeout * 4);

      //Fax
      fKey(4);
      waitFor(text("Bestimmung"), timeout * 4);
      fKey(1);
      waitFor(first(text("File").under(text("Type")).before(text(">"))));
      fKey(1);
      positionOfType = findNow(text("Type"));
      waitFor(first(text("Fax").under(text("Type")).before(text(">"))));
      type = readText(position(positionOfType.row() + 1, positionOfType.col()), 6).trim();
      screenshot();
      Assert.assertEquals(type, "Fax");
      screenshot();
      underlined().under(text("Bestimmung"));
      String faxnr = readText(position(positionOfType.row() + 2, 25), 20);
      Assert.assertFalse(faxnr.equals(""));
      screenshot();
      enter();
      waitFor(text("Geburtsdatum"), timeout * 4);

      //Paper
      fKey(4);
      waitFor(text("Bestimmung"), timeout * 4);
      fKey(1);
      waitFor(first(text("File").under(text("Type")).before(text(">"))));
      positionOfType = findNow(text("Type"));
      fKey(1);
      waitFor(first(text("Fax").under(text("Type")).before(text(">"))));
      fKey(1);
      waitFor(first(text("Paper").under(text("Type")).before(text(">"))));
      type = readText(position(positionOfType.row() + 1, positionOfType.col()), 6).trim();
      Assert.assertEquals(type, "Paper");
      screenshot();
      underlined().under(text("Bestimmung"));
      String printType = readText(position(positionOfType.row() + 2, 25), 20).trim();
      Assert.assertEquals(printType, "QPRINT");
      screenshot();
      enter();
      waitFor(text("Geburtsdatum"), timeout * 4);

    }
  }

  private static class InvoiceScript extends AbstractScript {

    private final String orderNumber;

    public InvoiceScript(String orderNumber) {
      this.orderNumber = orderNumber;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      String env = Helper.getEnvironment();

      SearchCriterion input;
      SearchCriterion auftragInput;
      //Invoicing is different on DEV and TST, than it is to QAS and PRD
      switch (env) {
        case "DEV":
        case "TST":
          waitFor(text("===>"));
          input = first(underlined().after(text("===>")));
          typeAt(input, "7");
          enter();
          waitFor(text("Tarifierung & Fakturierung"));
          type("11");
          enter();
          waitFor(text("Zeugnis"));
          auftragInput = first(underlined().after(text("Auftragnummer")).after(text("/")));
          typeAt(auftragInput, orderNumber);
          enter();
          waitFor(text("FAKT0101"));
          fKey(24);
          waitFor(text("FAKT0001"));
          break;
        case "QAS":

          waitFor(text("===>"));
          input = first(underlined().after(text("===>")));
          typeAt(input, "2");
          enter();
          waitFor(text("Auftr").and(text("geverarbeitung"))); //Aufträgeverarbeitung
          type("10");
          enter();
          waitFor(text("Auftrag Administration"));

          auftragInput = first(underlined().after(text("Einrichtung")).after(text("Auftrag")));
          typeAt(auftragInput, orderNumber);
          enter();
          waitFor(text("Fak.code/mode"));

          //Change rechnung to A, and delete doctorID
          SearchCriterion rechnungType = first(underlined().after(text("Rechnung an")));
          typeAt(rechnungType, "A");

          SearchCriterion rechnungDoctor = first(
              underlined().after(text("Rechnung an")).after(underlined()));
          String prescribingDoctor = readText(
              findNow(first(underlined().after(text("Rechnung an")).after(underlined()))),
              10).trim();
          typeAt(rechnungDoctor, "          ");

          SearchCriterion fakCode = first(underlined().after(text("mode")));
          move(first(underlined().after(text("mode"))));
          waitFor(cursor().after(text("mode")));
          typeAt(fakCode, "FAK");

          SearchCriterion kostenlos = first(underlined().after(text("Kostenlos")));
          typeAt(kostenlos, "N");

          enter();
          screenshot();
          enter();
          screenshot();

          fKey(12);
          fKey(12);
          fKey(12);
          fKey(12);
          fKey(12);
          waitFor(text("Hauptmen").and(text("===>")));
          type("7");
          enter();
          waitFor(text("Tarifierung & Fakturierung"));
          type("17");
          enter();
          waitFor(text("Fakturation"));

          SearchCriterion institute = first(underlined().after(text("Institut")));
          typeAt(institute, "001");

          SearchCriterion proforma = first(underlined().after(text("Proforma")));
          typeAt(proforma, "N");

          String currentDate = DateTime.generateCurrentDateTime("ddMMyyyy");
          SearchCriterion startDate = first(underlined().after(text("Ab/bis Datum")));
          typeAt(startDate, currentDate);
          Helper.sleep(1000);
          type(currentDate);

          screenshot();
          Helper.sleep(1000);

          SearchCriterion fakturierungscodes = first(
              underlined().after(underlined()).after(text("Fakturierungscodes")));
          typeAt(fakturierungscodes, "FAK");

          SearchCriterion bestimmung = first(underlined().after(text("Bestimmung")));
          typeAt(bestimmung, "A");

          waitFor(text("Kunde"));

          SearchCriterion kundeValue = first(underlined().after(underlined()).after(text("Kunde")));
          typeAt(kundeValue, prescribingDoctor);

          enter();
          screenshot();
          waitFor(text("Aufgabenplanung"));
          enter();
          screenshot();
          fKey(12);
          fKey(12);
          fKey(12);
          fKey(12);
          screenshot();
          break;
        case "PRD":
          BaseTestThreading.skipTestcase("Should not execute invoicing on PRD environment!");
          break;
        default:
          throw new RuntimeException("Unsupported environment given to perform invoicing");
      }

    }
  }

  private static class partialOrdvalScript extends AbstractScript {

    private final String orderNumber;
    private List<String> materials = new ArrayList<>();

    public partialOrdvalScript(String orderNumber, List<String> materials) {
      this.orderNumber = orderNumber;
      this.materials = materials;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      fKey(9);
      SearchCriterion cmdInput = first(underlined().after(text("===>")).under(text("Command")));
      typeAt(cmdInput, "ORDVAL");
      enter();

      waitFor(text("Order validieren").and(text("== TUBE by TUBE ==")));
      SearchCriterion orderInput = first(underlined().after(text("Order / Tube")));
      typeAt(orderInput, orderNumber);
      enter();

      waitFor(text("Recipient Empfang"));

      ///Partially validate orders here

      for (int i = 0; i < materials.size(); i++) {
        SearchCriterion orderOpt = first(underlined().before(text(materials.get(i))));
        typeAt(orderOpt, "1");
        enter(); //First enter will trigger the material to be displayed in red
        screenshot();
        waitFor(underlined().before(text(materials.get(i))));
        screenshot();
      }

      enter(); //Second enter will confirm the partial order

      waitFor(text("Order validieren").and(text("== TUBE by TUBE ==")));
      screenshot();
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);
      screenshot();
    }
  }

  private static class EnterResultScript extends AbstractScript {

    private final String orderNumber;
    private final int analyseID;
    private final String result;

    public EnterResultScript(String orderNumber, int analyseID, String result) {
      this.orderNumber = orderNumber;
      this.analyseID = analyseID;
      this.result = result;
    }

    @Override
    public void run() {
      final String lab400password = Passwords.readPassword("lab400-cortex");
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      SearchCriterion cmdInput = first(underlined().after(text("===>")));
      type("2");
      enter();
      waitFor(text("Auftr").and(text("geverarbeitung"))); //Aufträgeverarbeitung
      type("5");
      enter();

      waitFor(text("Identifikation"));
      SearchCriterion userInput = first(underlined().after(text("Benutzer")));
      SearchCriterion passInput = first(underlined().after(text("Kennwort")));

      typeAt(userInput, "CORTEX");
      enter();
      type(lab400password);
      enter();

      waitFor(text("Resultateadministration"));
      SearchCriterion selector = first(underlined().after(text("Auswahl")));
      typeAt(selector, "4");
      enter();

      SearchCriterion auftragnummer = first(underlined().after(text("Auftragnummer")));
      typeAt(auftragnummer, orderNumber);
      enter();

      SearchCriterion orderLocked = text("Auftrag wird verarbeitet| Einen Moment Geduld bitte");
      SearchCriterion resultProAuftrag = text("Resultate pro Auftrag");

      waitFor(resultProAuftrag.or(orderLocked));
      if (has(orderLocked)) {
        throw new RuntimeException("Order locked and cannot be edited!");
      }

      //Select the analysis ID for which we want to enter results
      SearchCriterion opt = first(underlined().before(text(Integer.toString(analyseID))));
      typeAt(opt, "7");
      enter();

      waitFor(text("Grenzwerte"));
      SearchCriterion borderValuesSC = first(after(text("Grenzwerte")));
      Position position;
      position = findNow(borderValuesSC);
      position = position(position.row(), position.col() + 13);
      String borderValues = readText(position, 20);
      position = findNow(first(after(text("Einheit"))));
      position = position(position.row(), position.col() + 13);
      String unit = readText(position, 20);

      String resultString = Conversion.padAS400AnalyseResults(result);

      screenshot();

      SearchCriterion result = first(underlined().after(text("Resultat")));
      typeAt(result, resultString);
      enter();
      waitFor(text("Validierung"));
      screenshot();

      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);
      screenshot();
    }
  }

  public static class GetLabResultScript extends AbstractScript {

    private final String orderNumber;
    public JSONObject analyseData = new JSONObject();

    public GetLabResultScript(String orderNumber) {
      this.orderNumber = orderNumber;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      SearchCriterion cmdInput = first(underlined().after(text("===>")));
      type("2");
      enter();
      waitFor(text("Auftr").and(text("geverarbeitung"))); //Aufträgeverarbeitung
      type("9");
      enter();

      waitFor(text("Daten aufrufen"));
      SearchCriterion orderInput = first(underlined().after(text("Nummer")));
      typeAt(orderInput, orderNumber);
      enter();

      waitFor(text("Analysen pro Probe"));

      //Get list of all applied analyses
      Position positionOfOpt = findNow(text("Opt Analyse"));
      List<Position> positionsOfAnalyses = findAllNow(underlined().under(text("Opt Analyse")));
      List<String> analyses = new ArrayList<>();
      for (int i = positionOfOpt.row() + 1; i < positionsOfAnalyses.get(0).row(); i++) {
        Position position = position(i, 5);
        String type = readText(position, 50);
        analyses.add(type.trim());
      }

      analyseData.put("analyses", analyses);

      //Get list of analyses
      JSONObject analysesDetails = new JSONObject();
      HashMap<String, String> entryMethods = new HashMap<>();
//			for(int i = positionsOfAnalyses.get(0).row(); i <= positionsOfAnalyses.get(positionsOfAnalyses.size() - 1).row(); i++){
      for (Position positionOfAnalyse : positionsOfAnalyses) {
        String analyseName = readText(position(positionOfAnalyse.row(), 5), 25);
        String result = readText(position(positionOfAnalyse.row(), 32), 10);
        String unit = readText(position(positionOfAnalyse.row(), 45), 10);
        String normalRange = readText(position(positionOfAnalyse.row(), 62), 20);
        String possibleComment = readText(position(positionOfAnalyse.row() + 2, 6), 55);

        JSONObject details = new JSONObject();
        details.put("name", analyseName.trim());
        details.put("result", result.trim());
        details.put("unit", unit.trim());
        details.put("normalRange", normalRange.trim());

        if (possibleComment.contains("->")) {
          details.put("comment", possibleComment.trim());
        }

        //Get entry method by opening their child page
        Position position = position(positionOfAnalyse.row(), 5);
        String type = readText(position, 25).trim();

        SearchCriterion opt = first(underlined().before(text(type)));

        typeAt(opt, "5");
        enter();
        waitFor(text("GLP-logging Analyse"));

        Position positionOfStatus = findNow(text("Status"));
        HashSet<String> statusses = new HashSet<>();
        for (int j = 0 + 1; j < 15; j++) {
          Position positionStatus = position(positionOfStatus.row() + j, 1);
          String entryMethod = readText(positionStatus, 11).trim();

          if (entryMethod.equals("RESULT")) {
            //Valid result found
            Position entryMethodPosition = find(first(under(text("RESULT"))));
            entryMethod = readText(entryMethodPosition, 11).trim();

            if (entryMethod.equals("RLT_SERVER")) {
              entryMethodPosition = find(first(under(text("RLT_SERVER"))));
              entryMethod = readText(entryMethodPosition, 11).trim();
            }

            String benutzer = readText(position(entryMethodPosition.row(), 27),
                10).trim(); //Of that same line, get the benutzer value

            details.put("entryMethod", entryMethod);
            details.put("benutzer", benutzer);

          }
        }

        screenshot();
        fKey(12); //Return to previous screen
        waitFor(text("Analysen pro Probe"));

        analysesDetails.put(details.getString("name"), details);
      }

      analyseData.put("analysesDetails", analysesDetails);

      screenshot();
      fKey(12);

    }

  }

  public static class GetOrderAdministrationScript extends AbstractScript {

    private final String orderNumber;
    public JSONObject orderAdm = new JSONObject();

    public GetOrderAdministrationScript(String orderNumber) {
      this.orderNumber = orderNumber;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      fKey(9);
      waitFor(text("===>"));

      type("wrkadm");
      enter();
      waitFor(text("Auftrag Administration"));

      SearchCriterion orderInput = first(underlined().after(text("Auftrag")));
      typeAt(orderInput, orderNumber);
      enter();

      waitFor(text("Fak.code/mode"));

      Position position = findNow(first(after(text("Name")).after(text(": "))));
      String name = readText(position, 32).trim();
      orderAdm.put("name", name);

      position = findNow(first(underlined().after(text("VioNummer"))));
      String vioNr = readText(position, 12).trim();
      orderAdm.put("vioNr", vioNr);

      position = findNow(first(after(text("Adresse")).after(text(": "))));
      String address = readText(position, 32).trim();
      orderAdm.put("address", address);

      String country = readText(position(position.row() + 1, 3), 5).trim();
      orderAdm.put("country", country);

      String zip = readText(position(position.row() + 1, position.col()), 7).trim();
      orderAdm.put("zip", zip);

      String city = readText(position(position.row() + 1, position.col() + 6), 20).trim();
      orderAdm.put("city", city);

      position = findNow(first(underlined().after(text("PIDFID"))));
      String hisReference = readText(position, 13).trim();
      orderAdm.put("his", hisReference);

      screenshot();
      fKey(15);

      waitFor(text("Ihre Fall-Nr"));

      position = findNow(first(underlined().after(text("Ihre Fall-Nr"))));
      String orderReference = readText(position, 25).trim();
      orderAdm.put("orderReference", orderReference);

      screenshot();
      fKey(12);
      fKey(12);
      fKey(12);

    }
  }

  public static class GetPatientDetails extends AbstractScript {

    private final String orderNumber;
    private final int patientID;
    public JSONObject details = new JSONObject();

    public GetPatientDetails(String orderNumber, int patientID) {
      this.orderNumber = orderNumber;
      this.patientID = patientID;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      fKey(9);
      SearchCriterion cmdInput = first(underlined().after(text("===>")).under(text("Command")));
      typeAt(cmdInput, "WRKPAT");
      enter();

      waitFor(text("Patienten"));
      SearchCriterion patientInput = first(underlined().after(text("Nummer")));
      typeAt(patientInput, Integer.toString(patientID));
      enter();

      waitFor(text(Integer.toString(patientID)));

      SearchCriterion optionInput = first(
          underlined().under(text("Opt")).before(text(Integer.toString(patientID))));
      typeAt(optionInput, "5");
      enter();

      //Patient details
      waitFor(text("Einzigartige"));

      fKey(2);
      waitFor(text("Alternate Patientnummers"));

      //Extract data on the AHV number and other data

      Position positionOfAssign = findNow(text("Assign."));
      Position positionOfAlternateNr = findNow(text("Alternate nummer"));
      Position positionOfUser = findNow(text("Benutzer"));
      Position positionOfDate = position(7, 57);
      Position positionOfTime = position(7, 67);

      for (int j = 0 + 1; j < 10; j++) {
        Position positionStatus = position(positionOfAssign.row() + j, 1);
        String assign = readText(positionStatus, 10).trim();

        JSONObject row = new JSONObject();
        if (!assign.equals("")) {
          String assignValue = readText(
              position(positionOfAssign.row() + j, positionOfAssign.col()), 12).trim();
          String alternateNumber = readText(
              position(positionOfAssign.row() + j, positionOfAlternateNr.col()), 26).trim();
          String user = readText(position(positionOfAssign.row() + j, positionOfUser.col()),
              11).trim();
          String date = readText(position(positionOfAssign.row() + j, positionOfDate.col()),
              10).trim();
          String time = readText(position(positionOfAssign.row() + j, positionOfTime.col()),
              7).trim();
          row.put("Number", alternateNumber);
          row.put("User", user);
          row.put("Date", date);
          row.put("Time", time);
          long epoch = DateTime.dateTimeToEpoch(date + time, "yyyyMMddHHmm");
          if (epoch == 0) {
            throw new RuntimeException(
                "Could not properly parse datetime. Date: '" + date + "', time: '" + time + "'");
          }
          row.put("Epoch", epoch);
          details.put(assignValue, row);
        }

      }
    }

  }

  private static class DeletePropertiesForPatientScript extends AbstractScript {

    private final String orderNumber;
    private final int patientID;
    private final List<String> propertiesToDelete;

    public DeletePropertiesForPatientScript(String orderNumber, int patientID,
        List<String> propertiesToDelete) {
      this.orderNumber = orderNumber;
      this.patientID = patientID;
      this.propertiesToDelete = propertiesToDelete;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      run(new WRKPAT());
      waitFor(text("Patienten"));
      SearchCriterion patientInput = first(underlined().after(text("Nummer")));
      typeAt(patientInput, Integer.toString(patientID));
      enter();

      waitFor(text(Integer.toString(patientID)));

      SearchCriterion optionInput = first(
          underlined().under(text("Opt")).before(text(Integer.toString(patientID))));
      typeAt(optionInput, "5");
      enter();

      //Go to patient details
      waitFor(text("Einzigartige"));
      fKey(2);
      waitFor(text("Alternate Patientnummers"));

      SearchCriterion AssignAut = text("Assign.Aut.");
      Position positionOfAssignAut = findNow(AssignAut);

      for (String propertyToDelete : propertiesToDelete) {

        for (int i = 1; i < 10; i++) {

          Position positionToLook = position(positionOfAssignAut.row() + i,
              positionOfAssignAut.col());
          String assignAutValue = readText(positionToLook, 10).trim();

          if (assignAutValue.equals(propertyToDelete)) {
            optionInput = first(underlined().under(text("Opt")).before(text(propertyToDelete)));
            typeAt(optionInput, "4");
            enter();
            screenshot();
            enter();
            screenshot();
            waitFor(text("** Deleted **"));
            fKey(12);
            waitFor(text("Einzigartige"));
            fKey(2);
            waitFor(text("Alternate Patientnummers"));
            i = 0; //Start from the top of the list again, because entries might have moved up because of the deletion

          }

          if (assignAutValue.equals("")) {
            i = 11; //Abort for loop immediately
          }

        }
      }

      screenshot();
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);

    }
  }

  private static class EnterPropertiesForPatient extends AbstractScript {

    private final String orderID;
    private final int patientID;
    private final HashMap<String, String> propertiesToEnter;

    public EnterPropertiesForPatient(String orderID, int patientID,
        HashMap<String, String> propertiesToEnter) {
      this.orderID = orderID;
      this.patientID = patientID;
      this.propertiesToEnter = propertiesToEnter;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      run(new WRKPAT());
      SearchCriterion patientInput = first(underlined().after(text("Nummer")));
      typeAt(patientInput, Integer.toString(patientID));
      enter();
      waitFor(text(Integer.toString(patientID)));

      SearchCriterion optionInput = first(
          underlined().under(text("Opt")).before(text(Integer.toString(patientID))));
      typeAt(optionInput, "5");
      enter();

      //Patient details
      waitFor(text("Einzigartige"));

      fKey(2);
      waitFor(text("Alternate Patientnummers"));

      for (Map.Entry<String, String> property : propertiesToEnter.entrySet()) {
        String key = property.getKey();
        String value = property.getValue();

        SearchCriterion assAut = first(underlined().after(text("Ass.Aut .")));
        typeAt(assAut, key);
        SearchCriterion altPID = first(underlined().after(text("Alt PID.")));
        typeAt(altPID, value);
        enter();
        waitFor(underlined().before(text(key)));
        screenshot();

      }

      fKey(12);
      fKey(12);


    }
  }

  public static class GetMismatchList extends AbstractScript {

    public JSONObject details = new JSONObject();

    public GetMismatchList(String orderNumber) {
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());
      run(new GotoORDVALScreen());

      SearchCriterion cmdInput = first(underlined().after(text("Order / Tube")));
      move(cmdInput);

      fKey(4);
      waitFor(text("Zu validierende Analysen ohne Recipient"));

      //Extract data on the list of shown orders
      Position positionOfEntnahmeDateHeader = findNow(text("Entnahme"));
      Position positionOfOrderHeader = findNow(text("Order"));
      Position positionOBirthDateHeader = findNow(text("Geburtsdatum"));
      Position positionOfPatientDoctorHeader = findNow(text("Patient/Arzt"));

      int newEntriesAdded = 0;

      //Repeat this until the full list is parsed. To loop through the full list, we'll be using the pagedown key
      do {

        newEntriesAdded = 0;
        for (int j = 1; j <= 14; j++) {

          String auftrag = readText(
              position(positionOfOrderHeader.row() + j, positionOfOrderHeader.col()), 12).trim();

          JSONObject row = new JSONObject();
          if (!auftrag.equals("") && !details.has(auftrag)) {
            newEntriesAdded++;
            String sampleDate = readText(position(positionOfEntnahmeDateHeader.row() + j,
                positionOfEntnahmeDateHeader.col()), 9).trim();
            String orderID = readText(
                position(positionOfOrderHeader.row() + j, positionOfOrderHeader.col()), 9).trim();
            String birthDate = readText(
                position(positionOBirthDateHeader.row() + j, positionOBirthDateHeader.col()),
                9).trim();
            String patient = readText(position(positionOfPatientDoctorHeader.row() + j,
                positionOfPatientDoctorHeader.col()), 30).trim().split(",")[0];

            row.put("OrderID", Conversion.toInt(orderID));
            row.put("SampleDate", sampleDate);

            String patternBuilder = "";
            int dayDigits = sampleDate.split("/")[0].length();

            for (int i = 0; i < dayDigits; i++) {
              patternBuilder = patternBuilder + "d";
            }

            patternBuilder = patternBuilder + "/MM/yy";

            long epoch = DateTime.dateTimeToEpoch(sampleDate, patternBuilder);

            row.put("SampleDate", epoch);
            row.put("BirthDate", birthDate);
            row.put("Patient", patient);

            details.put(auftrag, row);
          }

        }

        //Determining if the pageDown action was successful is quite difficult, as it's only the unknown content being refreshed.
        //Therefore, we're moving the cursor to the top of the screen first, then perform the pageDown action
        //When the pageDown is processed, the cursor will again put focus on the first entry in the list
        Position header = findNow(text("Zu validierende Analysen ohne Recipient"));
        move(header);
        waitFor(cursor().above(text("Optionen")));
        pageDown();
        waitFor(cursor().under(text("Opt")));
        screenshot();

      } while (newEntriesAdded != 0);


    }

  }

  public static class PutSampleInSerothek extends AbstractScript {

    public JSONObject analyseData = new JSONObject();
    public String orderNumber;
    public String analyseToPutInSerothek;

    public PutSampleInSerothek(String orderNumber, String analyseToPutInSerothek) {
      {
        this.orderNumber = orderNumber;
        this.analyseToPutInSerothek = analyseToPutInSerothek.toLowerCase();

      }
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      SearchCriterion cmdInput = first(underlined().after(text("===>")));
      type("2");
      enter();
      waitFor(text("Auftr").and(text("geverarbeitung"))); //Aufträgeverarbeitung
      type("9");
      enter();

      waitFor(text("Daten aufrufen"));
      SearchCriterion orderInput = first(underlined().after(text("Nummer")));
      typeAt(orderInput, orderNumber);
      enter();

      waitFor(text("Analysen pro Probe"));

      //Get list of all applied analyses, and search for the analyse we want to put in serothek
      Position positionOfOpt = findNow(text("Opt Analyse"));

      Boolean analyseHasBeenPutInSerothek = false;

      for (int i = positionOfOpt.row() + 1; i < positionOfOpt.row() + 10; i++) {
        Position position = position(i, 5);
        String type = readText(position, 25);

        if (!type.trim().equals("")) {

          if (analyseToPutInSerothek.equals(type.trim().toLowerCase())) {

            //Get analyse probe type. Should be 'S' for Ferritin, and 'SS' for Serothek
            SearchCriterion analyseCommand = first(underlined().before(text(type.trim())));
            typeAt(analyseCommand, "0");
            enter();
            waitFor(text("Status der Analyse"));
            String probeType = readText(position(4, 70), 5).trim();

            if (type.toLowerCase().trim().equals("ferritin")) {
              Assert.assertEquals(probeType, "S");
            }
            if (type.toLowerCase().trim().equals("serothek")) {
              Assert.assertEquals(probeType, "SS");
            }

            //Return to the main menu
            fKey(12);
            fKey(12);
            waitFor(text("Daten aufrufen"));
            fKey(12);
            waitFor(text("Auftr").and(text("geverarbeitung"))); //Aufträgeverarbeitung
            fKey(12);
            waitFor(text("Hauptmen").and(text("===>")));

            //STS
            cmdInput = first(underlined().after(text("===>")));
            typeAt(cmdInput, "30");
            enter();
            waitFor(text("SAMPLE TRACKING SYSTEM"));
            cmdInput = first(underlined().after(text("===>")));
            typeAt(cmdInput, "3");
            enter();
            String lab400password = Passwords.readPassword("lab400-cortex");
            waitFor(text("Identifikation"));
            SearchCriterion userInput = first(underlined().after(text("Benutzer")));
            SearchCriterion passInput = first(underlined().after(text("Kennwort")));
            typeAt(userInput, "CORTEX");
            enter();
            type(lab400password);
            enter();

            //Einlezen tray screen not always displayed
            waitFor(text("Einlezen Tray").or(text("Sample Tracking System")));
            if (has(text("Einlezen Tray"))) {
              enter();
            }

            waitFor(text("Sample Tracking System"));
            SearchCriterion location = first(underlined().before(text("001 - ")));
            typeAt(location, "7");
            enter();

            //Behaviour is slightly different on DEV environment.
            final String lab400environment = Helper.getEnvironmentProperty("lab400environment");

            if (lab400environment.equals("EDUC")) {
              waitFor(text("Corelab-Serothek (100000)"));
              location = first(underlined().before(text("Corelab-Serothek (100000)")));
              typeAt(location, "7");
              enter();
              waitFor(text("Raum 1.48"));
              location = first(underlined().before(text("Raum 1.48")));
              typeAt(location, "7");
              enter();
              waitFor(text("Tiefkühlschrank"));
              location = first(underlined().before(text("Tiefkühlschrank")));
              typeAt(location, "7");
              enter();

            } else if (lab400environment.equals("TEST") || lab400environment.equals("QAS")) {
              waitFor(text("Probenannahme-Serothek (475200)"));
              location = first(underlined().before(text("Probenannahme-Serothek (475200)")));
              typeAt(location, "7");
              enter();
              waitFor(text("Raum 0.131"));
              location = first(underlined().before(text("Raum 0.131")));
              typeAt(location, "7");
              enter();
              waitFor(text("Regal"));
              location = first(underlined().before(text("Regal")));
              typeAt(location, "7");
              enter();
            } else {
              throw new RuntimeException("Unsupported lab environment");
            }

            waitFor(text("Gitter "));
            location = first(underlined().before(text("Gitter ")));
            typeAt(location, "1");
            enter();
            waitFor(text("Sample Tracking System : detail"));

            //Repeat until all items are deleted

            location = first(underlined().under(text("Opt Lab")));
            while (location != null) {
              try {
                waitFor(text("Opt Lab"));
                move(location);
                Helper.sleep(2000);
                typeAt(location, "4");
                screenshot();
                enter();
                screenshot();
                enter();
                fKey(5);
                location = first(underlined().under(text("Opt Lab")));
              } catch (TimeoutException timeoutException) {
                break;
              }
            }
            //Enter probe type
            SearchCriterion probeType1 = first(underlined().after(text("Probe Type")));

            if (probeType.length() == 1) {
              probeType = probeType + " ";
            }

            typeAt(probeType1, probeType); //S = Ferritin, SS = Serothek, U = Urin
            SearchCriterion scanner = first(underlined().after(text("Input scanner")));
            typeAt(scanner, " 2");
            SearchCriterion mode = first(underlined().after(text("Mode")));
            typeAt(mode, "I");
            SearchCriterion barcode = first(underlined().after(text("Barcode")));
            move(barcode);
            Helper.sleep(1000);
            typeAt(barcode, orderNumber, timeout * 2);

            //Submit, and verify if orderID has been added to the list
            enter();
            screenshot();
            waitFor(text(orderNumber).under(text("Barcode")));
            analyseHasBeenPutInSerothek = true;
            i = positionOfOpt.row() + 11; //Abort for loop immediately

          }
        } else {
          i = positionOfOpt.row() + 11; //Abort for loop immediately
        }

      }

      //Return to home screen
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);

      if (!analyseHasBeenPutInSerothek) {
        throw new RuntimeException("Failed putting the requested analyse in serothek");
      }

    }

  }

  private static class CreateUpdateDeleteDoctorScript extends AbstractScript {

    private final String doctorNumber;
    private final String name;
    private final Map<String, String> updatedData;
    private final Runnable afterCreate;
    private final Runnable afterUpdate;
    private final Runnable afterDelete;

    private CreateUpdateDeleteDoctorScript(String doctorNumber, String name,
        Map<String, String> updatedData, Runnable afterCreate, Runnable afterUpdate,
        Runnable afterDelete) {
      this.doctorNumber = doctorNumber;
      this.name = name;
      this.updatedData = updatedData;
      this.afterCreate = afterCreate;
      this.afterUpdate = afterUpdate;
      this.afterDelete = afterDelete;
    }

    @Override
    public void run() {
			/*
				VBGH0101                    Ärzte
                                                                       INCL
				Nummer  . . . . 600000   Alias  . . N
				Name  . . . . .                               Suchwort  . .
				Ortschaft . . .                               Kategorie . .
				Adresse . . . .                               Anerkennung .
				Optionen
				  2=Ändern  4=Löschen  5=Anzeige
				Opt Nummer     Name                      Adresse                Anerkennungnr.
					612010     Anaesthesie OPS,          Gellertstrasse 144 /   W702712
					BETHESDA        Bethesda Spital      4020   Basel
					612011     Forler, Raymond           Rue Fleurs et Schiké   Z000000
														 68000  Colmar
					666821     Dr. med. dent Siegmar Det Lindenhof 6            R003006
									Biologische Zahnmedi 6060   Sarnen
					777771     EDV Test Softm Trigger,                          Z000000

					777777     Urs Widmer,               Spalenring 145/147 /   Z000000
					EDVTST          EDV Test             4002   Basel
					777779     test edv trigger softm,                          Z000000
					TE                                                                       +

				F3=Ende F5=Neuanzeige F6=Erstellen F10=Linientrennung F12=Zurück F13=Incl/Excl
			 */
      SearchCriterion listScreenId = text("VBGH0101");

			/*
				VBGH0201                    Ärzte

				Nummer . . .         Alias . .          Alpha  .        Such .

				Name . . . .
				p.A. . . . .
				Adresse  . .
				Ortschaft  .                                  CH
				Tel/privat                                             Fax
				Anerkennung                       Aktiv  . . . J       Intern . . . N
				Berichte
				Sprachcode  . . D                 Faxbericht  . . . N
				Anrede  . .          9            Prior/Gruppe  . . 5 /
				Einheit . . . . 1
				Kopie an  . . .                   E-mail  . . . . . N
				Kopie Patient . N

			 */
      SearchCriterion screen1Header = text("VBGH0201");
      SearchCriterion kopiePatient = text("Kopie Patient");
      SearchCriterion numberInput = first(underlined().after(text("Nummer")));
      SearchCriterion nameInput = first(underlined().after(text("Name")));
      SearchCriterion addressInput = first(underlined().after(text("Adresse")));
      Position addressCursorPosition = position(6, 14);

      Position workPhoneCursorPosition = position(8, 14);
      Position privatePhoneCursorPosition = position(8, 35);

      Position languageCursorPosition = position(11, 17);
      Position postalCodeCursorPosition = position(7, 14);
      Position cityCursorPosition = position(7, 21);
      Position countryCodeCursorPosition = position(7, 47);
      Position stateCursorPosition = position(7, 51);

			/*
				VBGH0202                    Ärzte

				Nummer . . . 244933            Name . . . . ConsultIT, Demo

				Einrichtung/Dienst/UF  . . . .             /     /
				Route/Nummer . . . . . . . . .    /
				Rezipient  . . . . . . . . . .
				Weiterleitend/Anerk./Patient . N /             / N

				Fakturierungscode/Fakt. Mode . T   / A
				Bestimmung Rechn. A/P/G/X /Nr. M /
				Selbstbet./Kostenlos . . . . . J / N
				Pseudo/Nomenclatur . . . . . . N / J

				Art Auftrag  . . . . . . . . .
				Ihre Pat-Nr. verpflichtet  . . N
				Immer Telefon  . . . . . . .   N
				Statistik  . . . . . . . . . .
				Kategorie/Klasse . . . . . . .      /
				Kontrolle  . . . . . . . . . . N
				Brief an Patienten . . . . . . N
				Befund mit fix Positionen  . . N
			 */
      SearchCriterion screen2Header = text("VBGH0202");
      SearchCriterion invoiceCodeInput = first(underlined().after(text("Fakturierungscode")));
      SearchCriterion invoiceModeInput = first(
          underlined().after(text("/").after(invoiceCodeInput)));
      SearchCriterion invoiceDestinationInput = first(
          underlined().after(text("Bestimmung Rechn.")));

      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      type("1");
      enter();
      waitFor(text("Stammdateien"));
      type("1");
      enter();

      waitFor(listScreenId);
      fKey(6);
      waitFor(screen1Header);

      typeAt(numberInput, doctorNumber);
      typeAt(nameInput, name);
      typeAt(addressInput, "Old address");
      pageDown();
      waitFor(screen2Header);
      typeAt(invoiceCodeInput, "T");
      typeAt(invoiceModeInput, "A");
      typeAt(invoiceDestinationInput, "A");
      exitEditScreen();

      afterCreate.run();

      type("2");
      enter();
      waitFor(kopiePatient.and(screen1Header));

      move(workPhoneCursorPosition);
      waitFor(cursor().under(text("Ortschaft")));

      typeAt(workPhoneCursorPosition, updatedData.get("workPhoneNumber"));
      typeAt(privatePhoneCursorPosition, updatedData.get("privatePhoneNumber"));
      typeAt(addressCursorPosition, updatedData.get("streetWithNumber"));
      typeAt(languageCursorPosition, updatedData.get("languageCode"));
      typeAt(postalCodeCursorPosition, updatedData.get("postalCode"));
      typeAt(cityCursorPosition, updatedData.get("city"));
      typeAt(countryCodeCursorPosition, updatedData.get("countryCode"));
      typeAt(stateCursorPosition, updatedData.get("state"));
      enter();

      exitEditScreen();

      afterUpdate.run();

      type("4");
      enter();
      waitFor(screen1Header.and(text("Drücken Sie bitte Enter zur Bestätigung")));
      enter();
      enter();
      waitFor(listScreenId);

      afterDelete.run();
    }


    private void exitEditScreen() {
      SearchCriterion listScreenId = text("VBGH0101");

      enter();
      enter();
      enter();
      enter();
      waitFor(text("Einsender"));
      enter();
      enter();
      waitFor(listScreenId);
    }
  }

  private static class CreateNewDoctorScript extends AbstractScript {

    private final String doctorNumber;
    private final String name;

    private CreateNewDoctorScript(String doctorNumber, String name) {
      this.doctorNumber = doctorNumber;
      this.name = name;
    }

    @Override
    public void run() {
			/*
				VBGH0101                    Ärzte
                                                                       INCL
				Nummer  . . . . 600000   Alias  . . N
				Name  . . . . .                               Suchwort  . .
				Ortschaft . . .                               Kategorie . .
				Adresse . . . .                               Anerkennung .
				Optionen
				  2=Ändern  4=Löschen  5=Anzeige
				Opt Nummer     Name                      Adresse                Anerkennungnr.
					612010     Anaesthesie OPS,          Gellertstrasse 144 /   W702712
					BETHESDA        Bethesda Spital      4020   Basel
					612011     Forler, Raymond           Rue Fleurs et Schiké   Z000000
														 68000  Colmar
					666821     Dr. med. dent Siegmar Det Lindenhof 6            R003006
									Biologische Zahnmedi 6060   Sarnen
					777771     EDV Test Softm Trigger,                          Z000000

					777777     Urs Widmer,               Spalenring 145/147 /   Z000000
					EDVTST          EDV Test             4002   Basel
					777779     test edv trigger softm,                          Z000000
					TE                                                                       +

				F3=Ende F5=Neuanzeige F6=Erstellen F10=Linientrennung F12=Zurück F13=Incl/Excl
			 */
      SearchCriterion listScreenId = text("VBGH0101");

			/*
				VBGH0201                    Ärzte

				Nummer . . .         Alias . .          Alpha  .        Such .

				Name . . . .
				p.A. . . . .
				Adresse  . .
				Ortschaft  .                                  CH
				Tel/privat                                             Fax
				Anerkennung                       Aktiv  . . . J       Intern . . . N
				Berichte
				Sprachcode  . . D                 Faxbericht  . . . N
				Anrede  . .          9            Prior/Gruppe  . . 5 /
				Einheit . . . . 1
				Kopie an  . . .                   E-mail  . . . . . N
				Kopie Patient . N

			 */
      SearchCriterion screen1Header = text("VBGH0201");
      SearchCriterion numberInput = first(underlined().after(text("Nummer")));
      SearchCriterion nameInput = first(underlined().after(text("Name")));
      SearchCriterion addressInput = first(underlined().after(text("Adresse")));

			/*
				VBGH0202                    Ärzte

				Nummer . . . 244933            Name . . . . ConsultIT, Demo

				Einrichtung/Dienst/UF  . . . .             /     /
				Route/Nummer . . . . . . . . .    /
				Rezipient  . . . . . . . . . .
				Weiterleitend/Anerk./Patient . N /             / N

				Fakturierungscode/Fakt. Mode . T   / A
				Bestimmung Rechn. A/P/G/X /Nr. M /
				Selbstbet./Kostenlos . . . . . J / N
				Pseudo/Nomenclatur . . . . . . N / J

				Art Auftrag  . . . . . . . . .
				Ihre Pat-Nr. verpflichtet  . . N
				Immer Telefon  . . . . . . .   N
				Statistik  . . . . . . . . . .
				Kategorie/Klasse . . . . . . .      /
				Kontrolle  . . . . . . . . . . N
				Brief an Patienten . . . . . . N
				Befund mit fix Positionen  . . N
			 */
      SearchCriterion screen2Header = text("VBGH0202");
      SearchCriterion invoiceCodeInput = first(underlined().after(text("Fakturierungscode")));
      SearchCriterion invoiceModeInput = first(
          underlined().after(text("/").after(invoiceCodeInput)));
      SearchCriterion invoiceDestinationInput = first(
          underlined().after(text("Bestimmung Rechn.")));

      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      type("1");
      enter();
      waitFor(text("Stammdateien"));
      type("1");
      enter();

      waitFor(listScreenId);
      fKey(6);
      waitFor(screen1Header);

      typeAt(numberInput, doctorNumber);
      typeAt(nameInput, name);
      typeAt(addressInput, "Old address");
      pageDown();
      waitFor(screen2Header);
      typeAt(invoiceCodeInput, "T");
      typeAt(invoiceModeInput, "A");
      typeAt(invoiceDestinationInput, "A");
      exitEditScreen();
    }

    private void exitEditScreen() {
      SearchCriterion listScreenId = text("VBGH0101");

      enter();
      enter();
      enter();
      enter();
      waitFor(text("Einsender"));
      enter();
      enter();
      waitFor(listScreenId);
    }
  }

  private static class DeleteADoctorScript extends AbstractScript {

    private final String doctorNumber;

    private DeleteADoctorScript(String doctorNumber) {
      this.doctorNumber = doctorNumber;
    }

    @Override
    public void run() {
      SearchCriterion listScreenId = text("VBGH0101");
      SearchCriterion screen1Header = text("VBGH0201");
      SearchCriterion numberInput = first(underlined().after(text("Nummer")));

      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      type("1");
      enter();
      waitFor(text("Stammdateien"));
      type("1");
      enter();

      waitFor(listScreenId);

      typeAt(numberInput, doctorNumber);
      enter();

      SearchCriterion actionInput = underlined().before(text(doctorNumber));
      move(actionInput);
      typeAt(actionInput, "4");
      enter();
      waitFor(screen1Header.and(text("Drücken Sie bitte Enter zur Bestätigung")));
      enter();
      enter();
      waitFor(listScreenId);
    }
  }

  public static class ChangeSampleDate extends AbstractScript {

    public JSONObject analyseData = new JSONObject();
    public String orderNumber;
    public String newSampleDate;

    public ChangeSampleDate(String orderNumber, String newSampleDate) {
      {
        this.orderNumber = orderNumber;
        this.newSampleDate = newSampleDate;
      }
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      SearchCriterion cmdInput = first(underlined().after(text("===>")));
      type("2");
      enter();
      waitFor(text("Auftr").and(text("geverarbeitung"))); //Aufträgeverarbeitung
      type("10");
      enter();

      SearchCriterion aufnamhenummer = first(underlined().after(text("Auftrag")));
      typeAt(aufnamhenummer, orderNumber);
      enter();

      waitFor(text("Administration vollst"));

      SearchCriterion verordnung = first(underlined().after(text("Verordnung")));
      typeAt(verordnung, newSampleDate);

      SearchCriterion eindgang = first(underlined().after(text("Eingang")));
      typeAt(eindgang, newSampleDate);

      SearchCriterion entnahme = first(underlined().after(text("Entnahme")));
      typeAt(entnahme, newSampleDate);

      enter();

      //Return to home screen
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);


    }

  }

  public static class SetPDFFlag extends AbstractScript {

    public JSONObject analyseData = new JSONObject();
    public Integer doctorID;
    public String flagValue;

    public SetPDFFlag(Integer doctorID, String flagValue) {
      {
        this.doctorID = doctorID;
        this.flagValue = flagValue;
      }
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);

      run(new LoginScript());

      SearchCriterion cmdInput = first(underlined().after(text("===>")));
      typeAt(cmdInput, "21");
      enter();
//			waitFor(text("Setup Resultatserver"));
//			cmdInput = first(underlined().after(text("===>")));
//			typeAt(cmdInput, "30");
//			enter();

      String env = Helper.getEnvironment();

      //Invoicing is different on DEV and TST, than it is to QAS and PRD
      switch (env) {
        case "DEV":
        case "TST":

          waitFor(text("Setup VBOX").and(text("RESULTATSERVER BEFUND")));
          cmdInput = first(underlined().after(text("===>")));
          typeAt(cmdInput, "41");
          enter();

          break;
        case "QAS":

          waitFor(text("Setup Resultatserver"));
          cmdInput = first(underlined().after(text("===>")));
          typeAt(cmdInput, "30");
          enter();

          waitFor(text("v-box").and(text("Resultat Server")));
          cmdInput = first(underlined().after(text("===>")));
          typeAt(cmdInput, "12");
          enter();
          break;

      }

      waitFor(text("Ärzte pro Bestimmung"));

      screenshot();

      //Assert if the doctorID is already shown. If it is not, scroll down as it could be displayed on another screen
      SearchCriterion doctorIDToChange = text(doctorID.toString());

      int failSafeCounter = 10; //Scroll max 10 times
      boolean doctorFlagAdjusted = false;
      do {

        if (has(doctorIDToChange)) {

          waitFor(cursor().under(text("Opt")));
          screenshot();
          Position positionOfDoctorFlag = findNow(doctorIDToChange);
          typeAt(position(positionOfDoctorFlag.row(), 50), flagValue);
          screenshot();
          enter();
          screenshot();
          doctorFlagAdjusted = true;

        } else {
          pageDown();
          Helper.sleep(1000);
          screenshot();

        }

        failSafeCounter--;

      } while (failSafeCounter > 0 && doctorFlagAdjusted == false);

      if (!doctorFlagAdjusted) {
        throw new RuntimeException("Failed adjusting PDF flag for doctor ID " + doctorID);
      }

      //Return to home screen
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);
      fKey(12);


    }
  }

  private static class ChangePatientAddressScript extends AbstractScript {

    private final String vioNumber;
    private final String newAddress;
    private final String orderIdForResend;

    public ChangePatientAddressScript(int vioNumber, String newAddress, String orderIdForResend) {
      this.vioNumber = String.valueOf(vioNumber);
      this.newAddress = newAddress;
      this.orderIdForResend = orderIdForResend;
    }

    @Override
    public void run() {
      open(Helper.getEnvironmentProperty("salpi"), 23);
      final String lab400environment = Helper.getEnvironmentProperty("lab400environment");

      run(new LoginScript());
      run(new WRKPAT());
      SearchCriterion numberInput = first(underlined().after(text("Nummer")));
      typeAt(numberInput, String.valueOf(vioNumber));
      enter();
      SearchCriterion actionInput = first(underlined()
          .before(text(vioNumber))
          .under(text("Opt Nummer"))
      );
      typeAt(actionInput, "2");
      enter();

      if (lab400environment.equalsIgnoreCase("QAS")) {
        SearchCriterion addressInput = first(underlined().after(text("Adresse/Telefon")));
        typeAt(addressInput, newAddress);
      } else {
        SearchCriterion addressInput = first(underlined().after(text("Adresse/Nummer")));
        typeAt(addressInput, newAddress);
      }
      SearchCriterion postalCodeInput = first(underlined().after(text("Ortschaft")));
      SearchCriterion cityInput = first(underlined().after(text(" ").after(postalCodeInput)));
      typeAt(postalCodeInput, "    ");
      typeAt(cityInput, "                         ");

      enter();
      enter();
      fKey(12);
      fKey(12);

      waitFor(text("Hauptmen").and(text("===>")));
      SearchCriterion cmdInput = first(underlined().after(text("===>")).under(text("Wahl")));
      typeAt(cmdInput, "2");
      enter();
      type("9");
      enter();
      SearchCriterion orderNumberInput = first(underlined().after(text("Nummer")));
      typeAt(orderNumberInput, orderIdForResend);
      enter();
      waitFor(text("Analysen pro Probe"));
      fKey(23);
      fKey(12);
      fKey(12);
      fKey(12);
    }
  }

  private static class LoginScript extends AbstractScript {

    @Override
    public void run() {
      final String password = Passwords.readPassword("salpi200-cortex");
      final String lab400environment = Helper.getEnvironmentProperty("lab400environment");

      SearchCriterion userLabel = text("User");
      SearchCriterion userInput = first(underlined().after(userLabel));
      SearchCriterion passInput = after(text("Password")).column(userInput);

      typeAt(userInput, "CORTEX");
      move(passInput);
      type(password);
      enter();

      waitFor(text("Previous sign-on").and(text("Press Enter to continue.")));
      enter();

      SearchCriterion displayProgramMessage = text("Display Program Messages");
      SearchCriterion mainMenu = text("IBM i Main Menu");

      waitFor(displayProgramMessage.or(mainMenu));
      if (has(displayProgramMessage)) {
        enter();
        waitFor(mainMenu);
      }

      SearchCriterion cmdInput = first(underlined().under(text("Selection or command")));
      typeAt(cmdInput, "STRLAB400 *" + lab400environment);
      enter();

      waitFor(text("Hauptmen"));
    }

  }

  /***
   * While logged in, call WRKPAT
   */
  private static class WRKPAT extends AbstractScript {

    @Override
    public void run() {
      fKey(9);
      SearchCriterion cmdInput = first(underlined().after(text("===>")).under(text("Command")));
      typeAt(cmdInput, "WRKPAT");
      enter();

      waitFor(text("Patienten"));

    }
  }

  private static class GotoORDVALScreen extends AbstractScript {

    @Override
    public void run() {
      fKey(9);
      SearchCriterion cmdInput = first(underlined().after(text("===>")).under(text("Command")));
      typeAt(cmdInput, "ORDVAL");
      enter();

      waitFor(text("Order validieren").and(text("== TUBE by TUBE ==")));
      SearchCriterion orderInput = first(underlined().after(text("Order / Tube")));
    }
  }
}