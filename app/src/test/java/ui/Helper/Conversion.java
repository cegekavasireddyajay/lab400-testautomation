package ui.Helper;


import org.apache.commons.lang3.StringUtils;

public class Conversion {


	/**
	 * Convert string orderID (with/without institute number) to integer
	 */
	public static int toInt(String orderID){
		orderID = deleteInstitute(orderID);
		return Integer.parseInt(orderID.replace(".", ""));
	}

	public static String addDotsToOrderID(String orderId){
		final var lengthInitial = orderId.length();
		final var afterRemovingZeros = StringUtils.stripStart(orderId, "0");
		final int zerosCount= lengthInitial-afterRemovingZeros.length();
		StringBuilder buf = new StringBuilder();
			buf.append(".");
		for (int i = 0; i < afterRemovingZeros.length(); i += 3) {
			buf.append(afterRemovingZeros.substring(i, i + 3));
			buf.append(".");
		}
		var convertedOrderId = buf.toString();
		for(int i=0;i<zerosCount;i++){
			convertedOrderId="0" + convertedOrderId;
		}

		if(convertedOrderId.endsWith(".")) convertedOrderId = convertedOrderId.substring(0, convertedOrderId.length() - 1);
		return convertedOrderId;
	}

	/**
	 * Pad string orderID (with/without institute number) to a number which we can use to search for it on lab400 side
	 */
	public static String padWithLeadingZeros(String orderID){
		orderID = deleteInstitute(orderID);
		int orderIDInt = toInt(orderID); //Remove dots and cast to int
		return String.format("%08d", orderIDInt); //Pad with zeros
	}

	/**
	 * Pad int orderID to a number which we can use to search for it on lab400 side
	 */
	public static String padWithLeadingZeros(int orderID){
		return String.format("%08d", orderID); //Pad with zeros
	}

	/**
	 * Add whitespace when entering analyse results to lab400
	 */
	public static String padAS400AnalyseResults(String result){
		return String.format("%11s", result); //Pad with spaces
	}

	/**
	 * Add whitespace when entering analyse results to lab400
	 */
	public static String padAS400AnalyseResults(int result){
		return String.format("%11s", result); //Pad with spaces
	}

	/**
	 * Remove institute number from given orderID in case it's present
	 */
	private static String deleteInstitute(String orderID){
		//If it contains an institute number, delete it
		if(orderID.contains("/")){
			orderID = orderID.split("/")[1];
		}
		return orderID;
	}

	/***
	 * Given a string, check if it would be parseable as a number
	 * @param possibleNumber
	 * @return Boolean
	 */
	public static Boolean checkIfStringIsNumber(String possibleNumber){

		if (possibleNumber == null) {
			return false;
		}
		try {
			double d = Double.parseDouble(possibleNumber);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}



}
