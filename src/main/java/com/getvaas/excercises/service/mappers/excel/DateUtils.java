package com.getvaas.excercises.service.mappers.excel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DateUtils {

    private static final int javaVersion = getJavaVersion();

    // NOTE: this function will only work until the year 9999 you have to do a
    // refactoring
    private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {

        /**
         * Serial version ID
         */
        private static final long serialVersionUID = 1L;

        {
            put("^\\d{8}$", "yyyyMMdd");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "dd/MM/yyyy");
            put("^\\d{1,2}/[a-zA-Z]{3}/\\d{2}$", "dd/MMM/yy");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
            put("^\\d{12}$", "yyyyMMddHHmm");
            put("^\\d{8}\\h\\d{4}$", "yyyyMMdd HHmm");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\h\\d{1,2}:\\d{2}$", "dd/MM/yyyy HH:mm");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\h\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
            put("^\\d{14}$", "yyyyMMddHHmmss");
            put("^\\d{8}\\h\\d{6}$", "yyyyMMdd HHmmss");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\h\\d{1,2}:\\d{2}:\\d{2}$", "dd/MM/yyyy HH:mm:ss");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\h\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
        }
    };

    private static String determineDateFormat(String dateString) {
        for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return DATE_FORMAT_REGEXPS.get(regexp);
            }
        }
        return null; // Unknown format.
    }

    /**
     * Parses a String to a Date
     * 
     * @param date the date string to be converted
     * @return the converted date
     */
    public static LocalDate stringToDate(String date) {
        String dateStandard = date.replaceAll("[!\\-.|<>]", "/");
        String dateFormat = determineDateFormat(dateStandard);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat).withLocale(new Locale("es"));
        try {
            return LocalDate.parse(dateStandard, formatter);
        } catch (DateTimeParseException e) { // Use the flexible format
            return stringToDateFlexible(dateStandard, dateFormat);
        }
    }

    /**
     * Parses a date with a flexible formatter
     * 
     * @param date       the date string to be converted
     * @param dateFormat the date format to be used
     * @return the converted Date. If there is an Exception then {@code null} is
     *         returned
     */
    protected static LocalDate stringToDateFlexible(String date, String dateFormat) {
        LocalDate localDate = stringToDateFlexible(date, dateFormat, new Locale("es"));
        if (localDate == null)
            localDate = stringToDateFlexible(date, dateFormat, new Locale("en"));
        return localDate;
    }

    /**
     * Parses a date with a flexible formatter
     * 
     * @param date       the date string to be converted
     * @param dateFormat the date format to be used
     * @param locale     the {@link Locale} to be used
     * @return the converted Date. If there is an Exception then {@code null} is
     *         returned
     */
    protected static LocalDate stringToDateFlexible(String date, String dateFormat, Locale locale) {
        // Super special case: DateTime accepts "feb." but not "feb"
        if (dateFormat.equals("dd/MMM/yy") && locale.equals(new Locale("es"))) {
            date = date.toLowerCase();
            if (javaVersion > 8) {
                String split = date.split("/")[1];
                date = date.replace(split, split + ".");
            }
        }
        DateFormat formatter = new SimpleDateFormat(dateFormat, locale);
        try {
            return formatter.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Gets the java version that is currently being used
     * 
     * @return the major java version, e.g. java 1.8 returns 8, and java 10.0
     *         returns 10
     */
    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

}
