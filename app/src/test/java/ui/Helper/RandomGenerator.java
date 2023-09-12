package ui.Helper;


import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit.LUHN_CHECK_DIGIT;


public class RandomGenerator {


	public static int randomInt(int minVal, int maxVal){
		Random rand = new Random();
		int randomNum = rand.nextInt((maxVal - minVal) + 1) + minVal;
		return  randomNum;
	}

	public static float randomFloat(float minVal, float maxVal){
		int precision = 0;
		String digitString = Float.toString(minVal);
		if(digitString.contains(".")){
			String afterDecimal = digitString.split("\\.")[1];
			precision = afterDecimal.length();
		}
		Random rand = new Random();
		float randomNum = minVal + rand.nextFloat() * (maxVal - minVal);
		BigDecimal randomBigDecimal = new BigDecimal(randomNum).setScale(precision, RoundingMode.HALF_UP);
		return  randomBigDecimal.floatValue();
	}

	public static String randomString(){
		return randomString(10);
	}

	public static String randomString(int length){
		String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		StringBuilder salt = new StringBuilder();
		Random rnd = new Random();
		while (salt.length() < length) {
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
		return saltStr;
	}

	public static String randomDate(String pattern){
		int randomEpoch = randomInt(0, Integer.parseInt(Long.toString(System.currentTimeMillis() / 1000)) - 86400 ); //Date between Jan. 1 1970, and yesterday
		Long tmp = (long) (randomEpoch * 1000.0);
		return DateTime.generateDateTime(tmp, pattern);
	}


	public static String randomOrderState(){
		String[] orderStates = new String[]{"Draft", "Approved", "Ordered"};
		Random rand = new Random();
		return orderStates[rand.nextInt(orderStates.length)];
	}

	/**
	 * Select random gender to be used as URL input
	 */
	public static String randomGender(){
		String[] orderStates = new String[]{"M", "F"};
		Random rand = new Random();
		return orderStates[rand.nextInt(orderStates.length)];
	}

	/**
	 * Select random country to be used as URL input
	 */
	public static String randomCountry(){
		String[] orderStates = new String[]{"BE", "CH", "NL"};
		Random rand = new Random();
		return orderStates[rand.nextInt(orderStates.length)];
	}

	/**
	 * Select random integer value of the provided list
	 */
	public static int randomFromIntList(List<Integer> fullList){
		Random rand = new Random();
		return fullList.get(rand.nextInt(fullList.size()));
	}

	/**
	 * Select random string value of the provided list
	 */
	public static String randomFromStringList(List<String> fullList){
		Random rand = new Random();
		return fullList.get(rand.nextInt(fullList.size()));
	}

	/****
	 * Generate a random AHV number, starting from a known prefix
	 * @param prefix
	 * @return
	 */
	public static String randomValidCHAHVNumber(int prefix){
		String AHV = Integer.toString(prefix) + Integer.toString(randomInt(100000000, 1000000000));
		Double AHVWithoutCheckDigit = Double.parseDouble(AHV);
		return calculateCheckDigit(AHVWithoutCheckDigit);
	}

	/****
	 * Generate a random valid CH VEKA number.
	 * @return
	 */
	public static String randomValidCHVEKANumber(String prefix) throws CheckDigitException {

		Random r = new Random();
		double randomNum = 10000000000000.0 + (100000000000000.0 - 10000000000000.0) * r.nextDouble();
		String VEKA = String.format("%.0f", randomNum);

		String fullStringWithoutDots = prefix + VEKA;
		String checkDigit = LUHN_CHECK_DIGIT.calculate(fullStringWithoutDots.replace(".",""));

		String formatted = VEKA.substring(0,5) + "." + VEKA.substring(5);

		return prefix + "." + formatted + checkDigit;
	}

	public static String randomPhoneNumber(String prefix) {

		Random r = new Random();
		double randomNum = 10000000.0 + (100000000.0 - 10000000.0) * r.nextDouble();
		String partlyPhoneNr = String.format("%.0f", randomNum);

		String fullString = prefix.replace(" ","") + partlyPhoneNr;

		return fullString;
	}

	private static String calculateCheckDigit(Double ahvWithoutCheckDigit){
		//Calculate check digit as explained on https://www.axicon.com/checkdigitcalculator.html
		String ahvWithoutCheckDigitAsString = String.format("%.0f", ahvWithoutCheckDigit);

		if(ahvWithoutCheckDigitAsString.length() != 12){
			throw new RuntimeException("Invalid length provided to calculate AHV check digit number.");
		}

		//Step 1: Add together all alternate numbers starting from the right
		int totalStep1 = 0;
		for(int i = 0; i < ahvWithoutCheckDigitAsString.length() / 2; i++){
			String sub = ahvWithoutCheckDigitAsString.substring(i * 2 + 1,i * 2 + 2);
			int subInt = Integer.parseInt(sub);
			totalStep1 = totalStep1 + subInt;
		}

		//Step 2: Multiply previous result by 3
		int totalStep2 = totalStep1 * 3;

		//Step 3: Now add together the remaining numbers
		int totalStep3 = 0;
		for(int i = 0; i < ahvWithoutCheckDigitAsString.length() / 2; i++){
			String sub = ahvWithoutCheckDigitAsString.substring(i*2 ,i*2 + 1);
			int subInt = Integer.parseInt(sub);
			totalStep3 = totalStep3 + subInt;
		}

		//Step 4: Add step 2 and 3 together
		int totalStep4 = totalStep2 + totalStep3;

		//Step 5: The difference between step 4 and the NEXT 10th number
		int checkDigit = ( 10 - (totalStep4 % 10)) % 10;

		String totalAHV =  ahvWithoutCheckDigitAsString + Integer.toString(checkDigit);

		return totalAHV;
	}


	public static String randomFamilyname(){
		List<String> availableValues = Arrays.asList(new String[]{"Ammann", "Andros", "Brunner", "Baumann", "Baumgartner", "Bachmann", "Buhler", "Bucher", "Berge", "Brucker", "Fischer", "Fankhauser", "Graf", "Girtman", "Haller", "Hofer", "Hofmann", "Hess", "Hartmann", "Hedinger", "Jaggi", "Keller", "Kaufmann", "Kuntz", "Koch", "Kohler", "Kensinger", "Krieger", "Kuhn", "Klauser", "Klausner", "Lehmann", "Leuenberger", "Langel", "Liechti", "Meier", "Moser", "Maurer", "Noser", "Roth", "Reif", "Schmid", "Schneider", "Steiner", "Seiter"});
		return randomFromStringList(availableValues);
	}

	public static String randomFirstname(){
		List<String> availableValues = Arrays.asList(new String[]{"Alexander", "Alvarez", "Dorian", "Elijah", "Eric", "Finn", "Giovanni", "Gian", "Jakob", "Jacob", "Jason", "Jean", "Joel", "Levi", "Nikolas", "Noah", "Oskar", "Oscar", "Timeo", "Agatha", "Albina", "Anna", "Carmen", "Celia", "Clara", "Clarissa", "Claudia", "Elsa", "Emilia", "Emma", "Flurina", "Irene", "Isabella", "Joelle", "Julia", "Laetitia", "Leonie", "Luisa", "Lydia", "Lys", "Malea", "Malia", "Maria", "Marie", "Nina", "Sarah", "Sophia", "Sofia", "Yasmina", "Zoey", "Zoe", "Bruno", "Conrad", "Elias", "Gerfried", "Hans", "Julian", "Koloman", "Levin", "Wolfgang", "Aloisia", "Elena", "Ella", "Laura", "Lea", "Francisque", "Frederic", "Gabriel", "Laurent", "Renard", "Theo", "Yves", "Thomas"});
		return randomFromStringList(availableValues);
	}

	public static String randomStreet(){
		List<String> availableValues = Arrays.asList(new String[]{"Dorfstrasse", "Hauptstrasse", "Bahnhofstrasse", "Birkenweg", "Schulstrasse", "Oberdorfstrasse", "Kirchweg", "Industriestrasse", "Schulhausstrasse", "Rosenweg"});
		return randomFromStringList(availableValues);
	}

	public static String randomCity(){
		List<String> availableValues = Arrays.asList(new String[]{"Zurich", "Genève", "Basel", "Lausanne", "Bern", "Winterthur", "Lucerne", "St. Gallen", "Lugano", "Thun", "Bellinzona"});
		return randomFromStringList(availableValues);
	}






}
