package ui.Helper;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class PhoneNumber {

	/**
	 * @description With a regular phonenumber (as found on AS400 db), convert it to include the country code
	 */
	public static String db2PhoneToCountryCode(String phonenumber){
		String formatted = phonenumber;
		if(phonenumber.length()>2){
			formatted = "+" + phonenumber.substring(2);
		}
		return formatted;

	}








}
