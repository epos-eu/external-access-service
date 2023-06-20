/*******************************************************************************
 * Copyright 2021 EPOS ERIC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.epos.api.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class Utils {

	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
	
	public static final String PAYLOAD = "Payload";
	
	public static final Gson gson = new Gson();

	public static final String version = "1.2.0";
	
	public static final String EPOS_internal_format = "yyyy-MM-ddThh:mm:ssZ";

	public static String generateUUID() {
		return System.nanoTime()+UUID.randomUUID().toString();
	}
	
	public static String convertISOPatternToJavaFormatPattern(String pattern) {
		return pattern.replace("YYYY", "yyyy").replace("DD", "dd").replace("hh", "HH").replace("T", "'T'").replace("Z", "'Z'");
	}
	
	public static String convert(String dateString, String inputFormat, String outputFormat) throws ParseException {
		if(inputFormat==null) inputFormat=EPOS_internal_format;
		if(outputFormat==null) outputFormat=EPOS_internal_format;
		if(dateString==null || inputFormat==null || outputFormat==null) throw new NullPointerException();

		String dateConverted = null;
		
		if(dateString.contains(" ")) dateString=dateString.split(" ")[0];
		if(dateString.contains("\\+")) dateString=dateString.split("\\+")[0];
		
		if(outputFormat.equals("YYYY.yyy")) {
			dateConverted = Float.toString(fromDateToDecimalYear(new SimpleDateFormat("yyyy-MM-dd").parse(dateString)));
		} else if(inputFormat.equals("YYYY.yyy")) {
			dateConverted = fromDecimalYearToDate(Float.parseFloat(dateString));
		}
		else {
			if(!isValidFormat(convertISOPatternToJavaFormatPattern(inputFormat), dateString)) {
				inputFormat = EPOS_internal_format;
			}
			
			inputFormat = convertISOPatternToJavaFormatPattern(inputFormat);
			outputFormat = convertISOPatternToJavaFormatPattern(outputFormat);
			DateTimeFormatter format = org.joda.time.format.DateTimeFormat.forPattern(inputFormat);
			LocalDateTime lDate = null; 
			LOGGER.debug("Convert date: {} format: {}", dateString, inputFormat);

			DateTime dt = new DateTime(dateString);
			try {
				lDate = org.joda.time.LocalDateTime.parse(dt.toString(), format);			 
				LOGGER.debug("LDATE: {}", lDate);
			}catch(Exception e) {
				try {
					lDate = org.joda.time.LocalDateTime.parse(dateString, format);			 
					LOGGER.debug("LDATE2: {}", lDate);
				}catch(Exception e1) {
					lDate = dt.toLocalDateTime();
					LOGGER.debug("LDATE3: {}", lDate);
				}
			}

			dateConverted = lDate.toString();
			dateConverted = DateTime.parse(dateConverted).toString(outputFormat);

		}
		return dateConverted;

	}

	private static float fromDateToDecimalYear(Date myDate) throws ParseException {
		DateTime dt = new DateTime(myDate);
		Calendar c = new GregorianCalendar();
		c.setTime(myDate);
		int days_in_year = c.getActualMaximum(Calendar.DAY_OF_YEAR);		 
		LOGGER.debug("days_in_year: {}", days_in_year);
		int day_number = dt.getDayOfYear();
		float day = ((float)(day_number-1)/days_in_year) + dt.getYear();
		return Float.parseFloat(String.format("%.3f", day).replace(',', '.'));
	}

	private static String fromDecimalYearToDate(float date) throws ParseException {
		int year = (int) date;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
		Calendar c = new GregorianCalendar();
		c.setTime(new Date(year));

		int days_in_year = c.getActualMaximum(Calendar.DAY_OF_YEAR);

		float day = (date - year)*days_in_year;
		c.add(Calendar.DATE, (int)day);
		c.set(Calendar.YEAR, year);

		return sdf.format(c.getTime());
	}
	
	public static boolean isValidFormat(String format, String value) {
		Date date = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			date = sdf.parse(value);
			if (!value.equals(sdf.format(date))) {
				date = null;
			}
		} catch (ParseException ex) {
			LOGGER.error("Problem parsing date, format: {} value: {}", format, value);
		}
		return date != null;
	}

}
