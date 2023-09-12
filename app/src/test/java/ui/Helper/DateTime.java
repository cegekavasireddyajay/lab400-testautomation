package ui.Helper;


import org.apache.commons.lang3.time.DateUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTime {

	// Pattern: yyyy-MM-dd HH:mm:ss.SSS
	/**
	 * @description Generate date time based on provided epoch timestamp and pattern.
	 * yyyy : Year
	 * MM : Month
	 * dd : Day
	 * HH : Hours
	 * mm : Minutes
	 * ss: Seconds
	 * SSS: Milliseconds
	 */
	public static String generateDateTime(long epoch, String pattern){
		Date date = new Date(epoch);
		DateFormat format = new SimpleDateFormat(pattern);
		String formatted = format.format(date);
		return formatted;
	}


	public static long dateTimeToEpoch(String datetime, String pattern){
		long epoch = 0;
		try {
			epoch = new SimpleDateFormat(pattern).parse(datetime).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return epoch;
	}

	public static String generateCurrentDateTime(String pattern){
		long epoch = System.currentTimeMillis();
		generateDateTime(epoch, pattern);
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
		ZonedDateTime now = ZonedDateTime.now();
		return dtf.format(now);
	}

	public static String patientInfoDateToNormalDate(int dateFromPatientInfo, String pattern){
		if(dateFromPatientInfo == 0){
			return null;
		}
		long epoch = dateTimeToEpoch(Integer.toString(dateFromPatientInfo), "yyyyMMdd");
		return generateDateTime(epoch, pattern);
	}

	public static String reformatDate(String date, String oldPattern, String newPattern) {
		long dateOfBirthLong = DateTime.dateTimeToEpoch(date, oldPattern);
		return DateTime.generateDateTime(dateOfBirthLong, newPattern);
	}

	public static Date swissDateOfBirthFormatToDate(String swissDateOfBirth) throws ParseException {
		return DateUtils.parseDate(swissDateOfBirth, "dd.MM.yyyy");
	}
}
