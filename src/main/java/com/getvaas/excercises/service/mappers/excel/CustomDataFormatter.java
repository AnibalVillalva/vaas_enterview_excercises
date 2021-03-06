package com.getvaas.excercises.service.mappers.excel;

import org.docx4j.XmlUtils;
import org.docx4j.org.apache.poi.util.LocaleUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xlsx4j.jaxb.Context;
import org.xlsx4j.model.CellUtils;
import org.xlsx4j.org.apache.poi.ss.usermodel.DateUtil;
import org.xlsx4j.org.apache.poi.ss.usermodel.ExcelGeneralNumberFormat;
import org.xlsx4j.org.apache.poi.ss.usermodel.ExcelStyleDateFormatter;
import org.xlsx4j.org.apache.poi.ss.usermodel.FractionFormat;
import org.xlsx4j.org.apache.poi.ss.util.NumberToTextConverter;
import org.xlsx4j.sml.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Reimplementation of org.apache.poi.ss.usermodel.DataFormatter
 */
@SuppressWarnings("all")
public class CustomDataFormatter implements Observer {
    private static final String defaultFractionWholePartFormat = "#";
    private static final String defaultFractionFractionPartFormat = "#/##";
    /** Pattern to find a number format: "0" or  "#" */
    private static final Pattern numPattern = Pattern.compile("[0#]+");

    /** Pattern to find days of week as text "ddd...." */
    private static final Pattern daysAsText = Pattern.compile("([d]{3,})", Pattern.CASE_INSENSITIVE);

    /** Pattern to find "AM/PM" marker */
    private static final Pattern amPmPattern = Pattern.compile("((A|P)[M/P]*)", Pattern.CASE_INSENSITIVE);

    /** 
     * A regex to find locale patterns like [$$-1009] and [$?-452].
     * Note that we don't currently process these into locales 
     */
    private static final Pattern localePatternGroup = Pattern.compile("(\\[\\$[^-\\]]*-[0-9A-Z]+\\])");

    /**
     * A regex to match the colour formattings rules.
     * Allowed colours are: Black, Blue, Cyan, Green,
     *  Magenta, Red, White, Yellow, "Color n" (1<=n<=56)
     */
    private static final Pattern colorPattern = 
       Pattern.compile("(\\[BLACK\\])|(\\[BLUE\\])|(\\[CYAN\\])|(\\[GREEN\\])|" +
       		"(\\[MAGENTA\\])|(\\[RED\\])|(\\[WHITE\\])|(\\[YELLOW\\])|" +
       		"(\\[COLOR\\s*\\d\\])|(\\[COLOR\\s*[0-5]\\d\\])", Pattern.CASE_INSENSITIVE);

    /**
     * A regex to identify a fraction pattern.
     * This requires that replaceAll("\\?", "#") has already been called 
     */
    private static final Pattern fractionPattern = Pattern.compile("(?:([#\\d]+)\\s+)?(#+)\\s*\\/\\s*([#\\d]+)");

    /**
     * A regex to strip junk out of fraction formats
     */
    private static final Pattern fractionStripper = Pattern.compile("(\"[^\"]*\")|([^ \\?#\\d\\/]+)");

    /**
     * A regex to detect if an alternate grouping character is used
     *  in a numeric format 
     */
    private static final Pattern alternateGrouping = Pattern.compile("([#0]([^.#0])[#0]{3})");
    
    /**
      * Cells formatted with a date or time format and which contain invalid date or time values
     *  show 255 pound signs ("#").
      */
     private static final String invalidDateTimeString;
     static {
         StringBuilder buf = new StringBuilder();
         for(int i = 0; i < 255; i++) buf.append('#');
         invalidDateTimeString = buf.toString();
     }

    /**
     * The decimal symbols of the locale used for formatting values.
     */
    private DecimalFormatSymbols decimalSymbols;

    /**
     * The date symbols of the locale used for formatting values.
     */
    private DateFormatSymbols dateSymbols;

    /**
     * A default date format, if no date format was given
     */
    private DateFormat defaultDateformat;
    
    /** <em>General</em> format for numbers. */
    private Format generalNumberFormat;

    /** A default format to use when a number pattern cannot be parsed. */
    private Format defaultNumFormat;

    /**
     * A map to cache formats.
     *  Map<String,Format> formats
     */
    private final Map<String,Format> formats = new HashMap<>();

    private final boolean emulateCSV;

    /** stores the locale valid it the last formatting call */
    private Locale locale;
    
    /** stores if the locale should change according to {@link LocaleUtil#getUserLocale()} */
    private boolean localeIsAdapting;
    
    private class LocaleChangeObservable extends Observable {
        void checkForLocaleChange() {
            checkForLocaleChange(LocaleUtil.getUserLocale());
        }
        void checkForLocaleChange(Locale newLocale) {
            if (!localeIsAdapting) return;
            if (newLocale.equals(locale)) return;
            super.setChanged();
            notifyObservers(newLocale);
        }
    }
    
    /** the Observable to notify, when the locale has been changed */
    private final LocaleChangeObservable localeChangedObservable = new LocaleChangeObservable();
    
    /** For logging any problems we find */
	protected static Logger logger = LoggerFactory.getLogger(CustomDataFormatter.class);
    
    /**
     * Creates a formatter using the {@link Locale#getDefault() default locale}.
     */
    public CustomDataFormatter() {
        this(false);
    }

    /**
     * Creates a formatter using the {@link Locale#getDefault() default locale}.
     *
     * @param  emulateCSV whether to emulate CSV output.
     */
    public CustomDataFormatter(boolean emulateCSV) {
        this(LocaleUtil.getUserLocale(), true, emulateCSV);
    }

    /**
     * Creates a formatter using the given locale.
     */
    public CustomDataFormatter(Locale locale) {
        this(locale, false);
    }

    /**
     * Creates a formatter using the given locale.
     *
     * @param  emulateCSV whether to emulate CSV output.
     */
    public CustomDataFormatter(Locale locale, boolean emulateCSV) {
        this(locale, false, emulateCSV);
    }

    /**
     * Creates a formatter using the given locale.
     * @param  localeIsAdapting (true only if locale is not user-specified)
     * @param  emulateCSV whether to emulate CSV output.
     */
    private CustomDataFormatter(Locale locale, boolean localeIsAdapting, boolean emulateCSV) {
        this.localeIsAdapting = true;
        localeChangedObservable.addObserver(this);
        // localeIsAdapting must be true prior to this first checkForLocaleChange call.
        localeChangedObservable.checkForLocaleChange(locale);
        // set localeIsAdapting so subsequent checks perform correctly
        // (whether a specific locale was provided to this DataFormatter or DataFormatter should
        // adapt to the current user locale as the locale changes)
        this.localeIsAdapting = localeIsAdapting;
        this.emulateCSV = emulateCSV;
    }

    /**
     * Return a Format for the given cell if one exists, otherwise try to
     * create one. This method will return <code>null</code> if the any of the
     * following is true:
     * <ul>
     * <li>the cell's style is null</li>
     * <li>the style's data format string is null or empty</li>
     * <li>the format string cannot be recognized as either a number or date</li>
     * </ul>
     *
     * @param cell The cell to retrieve a Format for
     * @return A Format for the format String
     */
    private Format getFormat(Cell cell) {
    	
    	CTCellStyle cellStyle = CellUtils.getCellStyle(cell); 
    	
        if ( cellStyle == null) {
            return null;
        }

        String formatStr = CellUtils.getNumberFormatString(cell);
        if(formatStr == null || formatStr.trim().length() == 0) {
            return null;
        }
        
        long formatIndex = CellUtils.getNumberFormatIndex(cell);
        
        return getFormat(CellUtils.getNumericCellValue(cell), formatIndex, formatStr);
    }
    
	 
    private Format getFormat(double cellValue, long formatIndex, String formatStrIn) {
//      // Might be better to separate out the n p and z formats, falling back to p when n and z are not set.
//      // That however would require other code to be re factored.
//      String[] formatBits = formatStrIn.split(";");
//      int i = cellValue > 0.0 ? 0 : cellValue < 0.0 ? 1 : 2; 
//      String formatStr = (i < formatBits.length) ? formatBits[i] : formatBits[0];

        String formatStr = formatStrIn;
        
        // Excel supports 3+ part conditional data formats, eg positive/negative/zero,
        //  or (>1000),(>0),(0),(negative). As Java doesn't handle these kinds
        //  of different formats for different ranges, just +ve/-ve, we need to 
        //  handle these ourselves in a special way.
        // For now, if we detect 3+ parts, we call out to CellFormat to handle it
        // TODO Going forward, we should really merge the logic between the two classes
        
        if (formatStr.contains(";") &&
                formatStr.indexOf(';') != formatStr.lastIndexOf(';')) {
//            try {
//                // Ask CellFormat to get a formatter for it
//                CellFormat cfmt = CellFormat.getInstance(formatStr);
//                // CellFormat requires callers to identify date vs not, so do so
//                Object cellValueO = Double.valueOf(cellValue);
//                if (DateUtil.isADateFormat((int) formatIndex, formatStr) && 
//                        // don't try to handle Date value 0, let a 3 or 4-part format take care of it 
//                        ((Double)cellValueO).doubleValue() != 0.0) {
//                    cellValueO = DateUtil.getJavaDate(cellValue);
//                }
//                // Wrap and return (non-cachable - CellFormat does that)
//                return new CellFormatResultWrapper( cfmt.apply(cellValueO) );
//            } catch (Exception e) {
                logger.warn("Formatting not ported for format " + formatStr + ", falling back");
//            }
        }
        
       // Excel's # with value 0 will output empty where Java will output 0. This hack removes the # from the format.
       if (emulateCSV && cellValue == 0.0 && formatStr.contains("#") && !formatStr.contains("0")) {
           formatStr = formatStr.replaceAll("#", "");
       }
       
        // See if we already have it cached
        Format format = formats.get(formatStr);
        if (format != null) {
            return format;
        }
        
        // Is it one of the special built in types, General or @?
        if ("General".equalsIgnoreCase(formatStr) || "@".equals(formatStr)) {
            return generalNumberFormat;
        }
        
        // Build a formatter, and cache it
        format = createFormat(cellValue, formatIndex, formatStr);
        formats.put(formatStr, format);
        return format;
    }

    /**
     * Create and return a Format based on the format string from a  cell's
     * style. If the pattern cannot be parsed, return a default pattern.
     *
     * @param cell The Excel cell
     * @return A Format representing the excel format. May return null.
     */
    public Format createFormat(Cell cell) {

    	//CTCellStyle cellStyle = cellUtils.getCellStyle(cell);
    	
        long formatIndex = CellUtils.getNumberFormatIndex(cell);
        String formatStr = CellUtils.getNumberFormatString(cell);
        return createFormat(CellUtils.getNumericCellValue(cell), formatIndex, formatStr);
    }

    private Format createFormat(double cellValue, long formatIndex, String sFormat) {
        String formatStr = sFormat;
        
        // Remove colour formatting if present
        Matcher colourM = colorPattern.matcher(formatStr);
        while(colourM.find()) {
           String colour = colourM.group();
           
           // Paranoid replacement...
           int at = formatStr.indexOf(colour);
           if(at == -1) break;
           String nFormatStr = formatStr.substring(0,at) +
              formatStr.substring(at+colour.length());
           if(nFormatStr.equals(formatStr)) break;

           // Try again in case there's multiple
           formatStr = nFormatStr;
           colourM = colorPattern.matcher(formatStr);
        }

        // Strip off the locale information, we use an instance-wide locale for everything
        Matcher m = localePatternGroup.matcher(formatStr);
        while(m.find()) {
            String match = m.group();
            String symbol = match.substring(match.indexOf('$') + 1, match.indexOf('-'));
            if (symbol.indexOf('$') > -1) {
                symbol = symbol.substring(0, symbol.indexOf('$')) +
                        '\\' +
                        symbol.substring(symbol.indexOf('$'), symbol.length());
            }
            formatStr = m.replaceAll(symbol);
            m = localePatternGroup.matcher(formatStr);
        }

        // Check for special cases
        if(formatStr == null || formatStr.trim().length() == 0) {
            return getDefaultFormat(cellValue);
        }
        
        if ("General".equalsIgnoreCase(formatStr) || "@".equals(formatStr)) {
           return generalNumberFormat;
        }

        if(DateUtil.isADateFormat((int) formatIndex,formatStr) &&
                DateUtil.isValidExcelDate(cellValue)) {
            return createDateFormat(formatStr, cellValue);
        }
        // Excel supports fractions in format strings, which Java doesn't
        if (formatStr.contains("#/") || formatStr.contains("?/")) {
            String[] chunks = formatStr.split(";");
            for (String chunk1 : chunks) {
                String chunk = chunk1.replaceAll("\\?", "#");
                Matcher matcher = fractionStripper.matcher(chunk);
                chunk = matcher.replaceAll(" ");
                chunk = chunk.replaceAll(" +", " ");
                Matcher fractionMatcher = fractionPattern.matcher(chunk);
                //take the first match
                if (fractionMatcher.find()) {
                    String wholePart = (fractionMatcher.group(1) == null) ? "" : defaultFractionWholePartFormat;
                    return new FractionFormat(wholePart, fractionMatcher.group(3));
                }
            }
            
            // Strip custom text in quotes and escaped characters for now as it can cause performance problems in fractions.
            //String strippedFormatStr = formatStr.replaceAll("\\\\ ", " ").replaceAll("\\\\.", "").replaceAll("\"[^\"]*\"", " ").replaceAll("\\?", "#");
            //System.out.println("formatStr: "+strippedFormatStr);
            return new FractionFormat(defaultFractionWholePartFormat, defaultFractionFractionPartFormat);
        }
        
        if (numPattern.matcher(formatStr).find()) {
            return createNumberFormat(formatStr, cellValue);
        }

        if (emulateCSV) {
            return new ConstantStringFormat(cleanFormatForNumber(formatStr));
        }
        // TODO - when does this occur?
        return null;
    }
    
 

    private Format createDateFormat(String pFormatStr, double cellValue) {
        String formatStr = pFormatStr;
        formatStr = formatStr.replaceAll("\\\\-","-");
        formatStr = formatStr.replaceAll("\\\\,",",");
        formatStr = formatStr.replaceAll("\\\\\\.","."); // . is a special regexp char
        formatStr = formatStr.replaceAll("\\\\ "," ");
        formatStr = formatStr.replaceAll("\\\\/","/"); // weird: m\\/d\\/yyyy 
        formatStr = formatStr.replaceAll(";@", "");
        formatStr = formatStr.replaceAll("\"/\"", "/"); // "/" is escaped for no reason in: mm"/"dd"/"yyyy
        formatStr = formatStr.replace("\"\"", "'");	// replace Excel quoting with Java style quoting
        formatStr = formatStr.replaceAll("\\\\T","'T'"); // Quote the T is iso8601 style dates


        boolean hasAmPm = false;
        Matcher amPmMatcher = amPmPattern.matcher(formatStr);
        while (amPmMatcher.find()) {
            formatStr = amPmMatcher.replaceAll("@");
            hasAmPm = true;
            amPmMatcher = amPmPattern.matcher(formatStr);
        }
        formatStr = formatStr.replaceAll("@", "a");


        Matcher dateMatcher = daysAsText.matcher(formatStr);
        if (dateMatcher.find()) {
            String match = dateMatcher.group(0).toUpperCase(Locale.ROOT).replaceAll("D", "E");
            formatStr = dateMatcher.replaceAll(match);
        }

        // Convert excel date format to SimpleDateFormat.
        // Excel uses lower and upper case 'm' for both minutes and months.
        // From Excel help:
        /*
            The "m" or "mm" code must appear immediately after the "h" or"hh"
            code or immediately before the "ss" code; otherwise, Microsoft
            Excel displays the month instead of minutes."
          */

        StringBuilder sb = new StringBuilder();
        char[] chars = formatStr.toCharArray();
        boolean mIsMonth = true;
        List<Integer> ms = new ArrayList<Integer>();
        boolean isElapsed = false;
        for(int j=0; j<chars.length; j++) {
            char c = chars[j];
            if (c == '\'') {
                sb.append(c);
                j++;

                // skip until the next quote
                while(j<chars.length) {
                    c = chars[j];
                    sb.append(c);
                    if(c == '\'') {
                        break;
                    }
                    j++;
                }
            }
            else if (c == '[' && !isElapsed) {
                isElapsed = true;
                mIsMonth = false;
                sb.append(c);
            }
            else if (c == ']' && isElapsed) {
                isElapsed = false;
                sb.append(c);
            }
            else if (isElapsed) {
            if (c == 'h' || c == 'H') {
                    sb.append('H');
                }
                else if (c == 'm' || c == 'M') {
                    sb.append('m');
                }
                else if (c == 's' || c == 'S') {
                    sb.append('s');
                }
                else {
                    sb.append(c);
                }
            }
            else if (c == 'h' || c == 'H') {
                mIsMonth = false;
                if (hasAmPm) {
                    sb.append('h');
                } else {
                    sb.append('H');
                }
            }
            else if (c == 'm' || c == 'M') {
                if(mIsMonth) {
                    sb.append('M');
                    ms.add(
                            Integer.valueOf(sb.length() -1)
                    );
                } else {
                    sb.append('m');
                }
            }
            else if (c == 's' || c == 'S') {
                sb.append('s');
                // if 'M' precedes 's' it should be minutes ('m')
                for (int index : ms) {
                    if (sb.charAt(index) == 'M') {
                        sb.replace(index, index + 1, "m");
                    }
                }
                mIsMonth = true;
                ms.clear();
            }
            else if (Character.isLetter(c)) {
                mIsMonth = true;
                ms.clear();
                if (c == 'y' || c == 'Y') {
                    sb.append('y');
                }
                else if (c == 'd' || c == 'D') {
                    sb.append('d');
                }
                else {
                    sb.append(c);
                }
            }
            else {
                if (Character.isWhitespace(c)){
                    ms.clear();
                }
                sb.append(c);
            }
        }
        formatStr = sb.toString();

        try {
            return new ExcelStyleDateFormatter(formatStr, dateSymbols);
        } catch(IllegalArgumentException iae) {

            // the pattern could not be parsed correctly,
            // so fall back to the default number format
            return getDefaultFormat(cellValue);
        }

    }

    private String cleanFormatForNumber(String formatStr) {
        StringBuilder sb = new StringBuilder(formatStr);

        if (emulateCSV) {
            // Requested spacers with "_" are replaced by a single space.
            // Full-column-width padding "*" are removed.
            // Not processing fractions at this time. Replace ? with space.
            // This matches CSV output.
            for (int i = 0; i < sb.length(); i++) {
                char c = sb.charAt(i);
                if (c == '_' || c == '*' || c == '?') {
                    if (i > 0 && sb.charAt((i - 1)) == '\\') {
                        // It's escaped, don't worry
                        continue;
                    }
                    if (c == '?') {
                        sb.setCharAt(i, ' ');
                    } else if (i < sb.length() - 1) {
                        // Remove the character we're supposed
                        //  to match the space of / pad to the
                        //  column width with
                        if (c == '_') {
                            sb.setCharAt(i + 1, ' ');
                        } else {
                            sb.deleteCharAt(i + 1);
                        }
                        // Remove the character too
                        sb.deleteCharAt(i);
                        i--;
                    }
                }
            }
        } else {
            // If they requested spacers, with "_",
            //  remove those as we don't do spacing
            // If they requested full-column-width
            //  padding, with "*", remove those too
            for (int i = 0; i < sb.length(); i++) {
                char c = sb.charAt(i);
                if (c == '_' || c == '*') {
                    if (i > 0 && sb.charAt((i - 1)) == '\\') {
                        // It's escaped, don't worry
                        continue;
                    }
                    if (i < sb.length() - 1) {
                        // Remove the character we're supposed
                        //  to match the space of / pad to the
                        //  column width with
                        sb.deleteCharAt(i + 1);
                    }
                    // Remove the _ too
                    sb.deleteCharAt(i);
                    i--;
                }
            }
        }

        // Now, handle the other aspects like 
        //  quoting and scientific notation
        for(int i = 0; i < sb.length(); i++) {
           char c = sb.charAt(i);
            // remove quotes and back slashes
            if (c == '\\' || c == '"') {
                sb.deleteCharAt(i);
                i--;

            // for scientific/engineering notation
            } else if (c == '+' && i > 0 && sb.charAt(i - 1) == 'E') {
                sb.deleteCharAt(i);
                i--;
            }
        }

        return sb.toString();
    }

    private Format createNumberFormat(String formatStr, double cellValue) {
        String format = cleanFormatForNumber(formatStr);
        DecimalFormatSymbols symbols = decimalSymbols;
        
        // Do we need to change the grouping character?
        // eg for a format like #'##0 which wants 12'345 not 12,345
        Matcher agm = alternateGrouping.matcher(format);
        if (agm.find()) {
            char grouping = agm.group(2).charAt(0);
            // Only replace the grouping character if it is not the default
            // grouping character for the US locale (',') in order to enable
            // correct grouping for non-US locales.
            if (grouping!=',') {
                symbols = DecimalFormatSymbols.getInstance(locale);

                symbols.setGroupingSeparator(grouping);
                String oldPart = agm.group(1);
                String newPart = oldPart.replace(grouping, ',');
                format = format.replace(oldPart, newPart);
            }
        }
        
        try {
            DecimalFormat df = new DecimalFormat(format, symbols);
            setExcelStyleRoundingMode(df);
            return df;
        } catch(IllegalArgumentException iae) {

            // the pattern could not be parsed correctly,
            // so fall back to the default number format
            return getDefaultFormat(cellValue);
        }
    }

    /**
     * Returns a default format for a cell.
     * @param cell The cell
     * @return a default format
     */
    public Format getDefaultFormat(Cell cell) {
        return getDefaultFormat(CellUtils.getNumericCellValue(cell));
    }
    private Format getDefaultFormat(double cellValue) {
        localeChangedObservable.checkForLocaleChange();
        
        // for numeric cells try user supplied default
        if (defaultNumFormat != null) {
            return defaultNumFormat;

          // otherwise use general format
        }
        return generalNumberFormat;
    }
    
    /**
     * Performs Excel-style date formatting, using the
     *  supplied Date and format
     */
    private String performDateFormatting(Date d, Format dateFormat) {
       return (dateFormat != null ? dateFormat : defaultDateformat).format(d);
    }

    /**
     * Returns the formatted value of an Excel date as a <tt>String</tt> based
     * on the cell's <code>DataFormat</code>. i.e. "Thursday, January 02, 2003"
     * , "01/02/2003" , "02-Jan" , etc.
     *
     * @param cell The cell
     * @return a formatted date string
     */
    private String getFormattedDateString(Cell cell) {
        Format dateFormat = getFormat(cell);
        if(dateFormat instanceof ExcelStyleDateFormatter) {
           // Hint about the raw excel value
           ((ExcelStyleDateFormatter)dateFormat).setDateToBeFormatted(
                 CellUtils.getNumericCellValue(cell)
           );
        }
        Date d = CellUtils.getDateCellValue(cell);
        //Date d = cell.getDateCellValue();
        return performDateFormatting(d, dateFormat);
    }

    /**
     * Returns the formatted value of an Excel number as a <tt>String</tt>
     * based on the cell's <code>DataFormat</code>. Supported formats include
     * currency, percents, decimals, phone number, SSN, etc.:
     * "61.54%", "$100.00", "(800) 555-1234".
     *
     * @param cell The cell
     * @return a formatted number string
     */
    private String getFormattedNumberString(Cell cell) {

        Format numberFormat = getFormat(cell);
        double d = CellUtils.getNumericCellValue(cell);
        if (numberFormat == null) {
            return String.valueOf(d);
        }
        String formatted = numberFormat.format(Double.valueOf(d));
        return formatted.replaceFirst("E(\\d)", "E+$1"); // to match Excel's E-notation
    }

    /**
     * Formats the given raw cell value, based on the supplied
     *  format index and string, according to excel style rules.
     * @see #formatCellValue(Cell)
     */
    public String formatRawCellContents(double value, int formatIndex, String formatString) {
        return formatRawCellContents(value, formatIndex, formatString, false);
    }
    /**
     * Formats the given raw cell value, based on the supplied
     *  format index and string, according to excel style rules.
     * @see #formatCellValue(Cell)
     */
    public String formatRawCellContents(double value, int formatIndex, String formatString, boolean use1904Windowing) {
        localeChangedObservable.checkForLocaleChange();
        
        // Is it a date?
        if(DateUtil.isADateFormat(formatIndex,formatString)) {
            if(DateUtil.isValidExcelDate(value)) {
                Format dateFormat = getFormat(value, formatIndex, formatString);
                if(dateFormat instanceof ExcelStyleDateFormatter) {
                    // Hint about the raw excel value
                    ((ExcelStyleDateFormatter)dateFormat).setDateToBeFormatted(value);
                }
                Date d = DateUtil.getJavaDate(value, use1904Windowing);
                return performDateFormatting(d, dateFormat);
            }
            // RK: Invalid dates are 255 #s.
            if (emulateCSV) {
                return invalidDateTimeString;
            }
        }
        
        // else Number
        Format numberFormat = getFormat(value, formatIndex, formatString);
        if (numberFormat == null) {
            return String.valueOf(value);
        }
        
        // When formatting 'value', double to text to BigDecimal produces more
        // accurate results than double to Double in JDK8 (as compared to
        // previous versions). However, if the value contains E notation, this
        // would expand the values, which we do not want, so revert to
        // original method.
        String result;
        final String textValue = NumberToTextConverter.toText(value);
        if (textValue.indexOf('E') > -1) {
            result = numberFormat.format(Double.valueOf(value));
        }
        else {
            result = numberFormat.format(new BigDecimal(textValue));
        }
        // Complete scientific notation by adding the missing +.
        if (result.indexOf('E') > -1 && !result.contains("E-")) {
            result = result.replaceFirst("E", "E+");
        }
        return result;
    }

    /**
     * <p>
     * Returns the formatted value of a cell as a <tt>String</tt> regardless
     * of the cell type. If the Excel format pattern cannot be parsed then the
     * cell value will be formatted using a default format.
     * </p>
     * <p>When passed a null or blank cell, this method will return an empty
     * String (""). Formulas in formula type cells will not be evaluated.
     * </p>
     *
     * @param cell The cell
     * @return the formatted cell value as a String
     */
    public String formatCellValue(Cell cell) {
        
        if (cell == null) {
            return "";
        }

        int cellType = CellUtils.getCellType(cell);
        if (cellType == CellUtils.CELL_TYPE_FORMULA) {
                return getCellFormula(cell);
        }
        switch (cellType) {
            case CellUtils.CELL_TYPE_NUMERIC :

                if (DateUtil.isCellDateFormatted(cell)) {
                    return getFormattedDateString(cell);
                }
                return getFormattedNumberString(cell);

            case CellUtils.CELL_TYPE_STRING :
                //return cell.getRichStringCellValue().getString();
            	//throw new RuntimeException("TODO: RichStringCellValue");
            	return getCellStringValue(cell);
            	
            case CellUtils.CELL_TYPE_BOOLEAN :
                return String.valueOf(CellUtils.getBooleanCellValue(cell));
            case CellUtils.CELL_TYPE_BLANK :
                return "";
            case CellUtils.CELL_TYPE_ERROR:
            	//return FormulaError.forInt(cell.getErrorCellValue()).getString();
            	throw new RuntimeException("TODO: FormulaError");
        }
        throw new RuntimeException("Unexpected celltype (" + cellType + ")");
    }
    
    private String getCellStringValue(Cell c) {
        List<CTRst> stringItems;
        try {
            stringItems = c.getWorksheetPart().getWorkbookPart().getSharedStrings().getJaxbElement().getSi();
        } catch (NullPointerException e) {
            return c.getIs().getT().getValue();
        }
    	
    	int index; 
    	try {
    		index = Integer.parseInt(c.getV());
    	} catch (NumberFormatException nfe) {
    		throw new RuntimeException(c.getV() + " can't be converted to an index into the shared strings table");
    	}
    	
    	CTRst rst = stringItems.get(index);
    	
    	if (rst.getR().size()>0 
    			&& rst.getT()!=null) {
    		logger.error(XmlUtils.marshaltoString(rst, Context.jcSML));
    		throw new RuntimeException("Shared string contained 2 types of data");
    	}
    	
    	if (rst.getT()!=null) {
    		return rst.getT().getValue();
    	}
    	
    	// build value
    	StringBuilder sb = new StringBuilder();
    	for (CTRElt rElt : rst.getR()) {
    		
    		sb.append(rElt.getT().getValue() ); // TODO worry about whitespace
    	}
    	
    	return sb.toString();    	
    }

    /**
     * Return a formula for the cell, for example, <code>SUM(C4:E4)</code>
     *
     * @return a formula for the cell
     * @throws IllegalStateException if the cell type returned by {@link #getCellType()} is not CELL_TYPE_FORMULA
     */
    
    public String getCellFormula(Cell _cell) {
        int cellType = CellUtils.getCellType(_cell);
        if(cellType != CellUtils.CELL_TYPE_FORMULA) throw CellUtils.typeMismatch(CellUtils.CELL_TYPE_FORMULA, cellType, false);

        CTCellFormula f = _cell.getF();
//        if (isPartOfArrayFormulaGroup() && f == null) {
//            Cell cell = getSheet().getFirstCellInArrayFormula(this);
//            return cell.getCellFormula();
//        }
        if (f==null) {
        	throw new RuntimeException("TODO: handle isPartOfArrayFormulaGroup()");
        }
        if (f.getT() == STCellFormulaType.SHARED) {
            //return convertSharedFormula((int)f.getSi());
        	throw new RuntimeException("TODO: convertSharedFormula");
        }
        return f.getValue(); 
    }

    /**
     * <p>
     * Sets a default number format to be used when the Excel format cannot be
     * parsed successfully. <b>Note:</b> This is a fall back for when an error
     * occurs while parsing an Excel number format pattern. This will not
     * affect cells with the <em>General</em> format.
     * </p>
     * <p>
     * The value that will be passed to the Format's format method (specified
     * by <code>java.text.Format#format</code>) will be a double value from a
     * numeric cell. Therefore the code in the format method should expect a
     * <code>Number</code> value.
     * </p>
     *
     * @param format A Format instance to be used as a default
     * @see Format#format
     */
    public void setDefaultNumberFormat(Format format) {
        for (Map.Entry<String, Format> entry : formats.entrySet()) {
            if (entry.getValue() == generalNumberFormat) {
                entry.setValue(format);
            }
        }
        defaultNumFormat = format;
    }

    /**
     * Adds a new format to the available formats.
     * <p>
     * The value that will be passed to the Format's format method (specified
     * by <code>java.text.Format#format</code>) will be a double value from a
     * numeric cell. Therefore the code in the format method should expect a
     * <code>Number</code> value.
     * </p>
     * @param excelFormatStr The data format string
     * @param format A Format instance
     */
    public void addFormat(String excelFormatStr, Format format) {
        formats.put(excelFormatStr, format);
    }

    // Some custom formats

    /**
     * @return a <tt>DecimalFormat</tt> with parseIntegerOnly set <code>true</code>
     */
    private static DecimalFormat createIntegerOnlyFormat(String fmt) {
        DecimalFormatSymbols dsf = DecimalFormatSymbols.getInstance(Locale.ROOT);
        DecimalFormat result = new DecimalFormat(fmt, dsf);
        result.setParseIntegerOnly(true);
        return result;
    }
    
    /**
     * Enables excel style rounding mode (round half up) on the 
     *  Decimal Format given.
     */
    public static void setExcelStyleRoundingMode(DecimalFormat format) {
        setExcelStyleRoundingMode(format, RoundingMode.HALF_UP);
    }

    /**
     * Enables custom rounding mode on the given Decimal Format.
     * @param format DecimalFormat
     * @param roundingMode RoundingMode
     */
    public static void setExcelStyleRoundingMode(DecimalFormat format, RoundingMode roundingMode) {
       format.setRoundingMode(roundingMode);
    }

    /**
     * If the Locale has been changed via {@link LocaleUtil#setUserLocale(Locale)} the stored
     * formats need to be refreshed. All formats which aren't originated from DataFormatter
     * itself, i.e. all Formats added via {@link DataFormatter#addFormat(String, Format)} and
     * {@link DataFormatter#setDefaultNumberFormat(Format)}, need to be added again.
     * To notify callers, the returned {@link Observable} should be used.
     * The Object in {@link Observer#update(Observable, Object)} is the new Locale.
     *
     * @return the listener object, where callers can register themselves
     */
    public Observable getLocaleChangedObservable() {
        return localeChangedObservable;
    }

    /**
     * Update formats when locale has been changed
     *
     * @param observable usually this is our own Observable instance
     * @param localeObj only reacts on Locale objects
     */
    public void update(Observable observable, Object localeObj) {
        if (!(localeObj instanceof Locale))  return;
        Locale newLocale = (Locale)localeObj;
        if (!localeIsAdapting || newLocale.equals(locale)) return;
        
        locale = newLocale;
        
        dateSymbols = DateFormatSymbols.getInstance(locale);
        decimalSymbols = DecimalFormatSymbols.getInstance(locale);
        generalNumberFormat = new ExcelGeneralNumberFormat(locale);

        // taken from Date.toString()
        defaultDateformat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", dateSymbols);
        defaultDateformat.setTimeZone(LocaleUtil.getUserTimeZone());       

        // init built-in formats

        formats.clear();
        Format zipFormat = ZipPlusFourFormat.instance;
        addFormat("00000\\-0000", zipFormat);
        addFormat("00000-0000", zipFormat);

        Format phoneFormat = PhoneFormat.instance;
        // allow for format string variations
        addFormat("[<=9999999]###\\-####;\\(###\\)\\ ###\\-####", phoneFormat);
        addFormat("[<=9999999]###-####;(###) ###-####", phoneFormat);
        addFormat("###\\-####;\\(###\\)\\ ###\\-####", phoneFormat);
        addFormat("###-####;(###) ###-####", phoneFormat);

        Format ssnFormat = SSNFormat.instance;
        addFormat("000\\-00\\-0000", ssnFormat);
        addFormat("000-00-0000", ssnFormat);
    }



    /**
     * Format class for Excel's SSN format. This class mimics Excel's built-in
     * SSN formatting.
     *
     * @author James May
     */
    @SuppressWarnings("serial")
   private static final class SSNFormat extends Format {
        public static final Format instance = new SSNFormat();
        private static final DecimalFormat df = createIntegerOnlyFormat("000000000");
        private SSNFormat() {
            // enforce singleton
        }

        /** Format a number as an SSN */
        public static String format(Number num) {
            String result = df.format(num);
            return result.substring(0, 3) + '-' +
                    result.substring(3, 5) + '-' +
                    result.substring(5, 9);
        }

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(format((Number)obj));
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return df.parseObject(source, pos);
        }
    }

    /**
     * Format class for Excel Zip + 4 format. This class mimics Excel's
     * built-in formatting for Zip + 4.
     * @author James May
     */
    @SuppressWarnings("serial")
   private static final class ZipPlusFourFormat extends Format {
        public static final Format instance = new ZipPlusFourFormat();
        private static final DecimalFormat df = createIntegerOnlyFormat("000000000");
        private ZipPlusFourFormat() {
            // enforce singleton
        }

        /** Format a number as Zip + 4 */
        public static String format(Number num) {
            String result = df.format(num);
            return result.substring(0, 5) + '-' +
                    result.substring(5, 9);
        }

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(format((Number)obj));
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return df.parseObject(source, pos);
        }
    }

    /**
     * Format class for Excel phone number format. This class mimics Excel's
     * built-in phone number formatting.
     * @author James May
     */
    @SuppressWarnings("serial")
   private static final class PhoneFormat extends Format {
        public static final Format instance = new PhoneFormat();
        private static final DecimalFormat df = createIntegerOnlyFormat("##########");
        private PhoneFormat() {
            // enforce singleton
        }

        /** Format a number as a phone number */
        public static String format(Number num) {
            String result = df.format(num);
            StringBuilder sb = new StringBuilder();
            String seg1, seg2, seg3;
            int len = result.length();
            if (len <= 4) {
                return result;
            }

            seg3 = result.substring(len - 4, len);
            seg2 = result.substring(Math.max(0, len - 7), len - 4);
            seg1 = result.substring(Math.max(0, len - 10), Math.max(0, len - 7));

            if(seg1.trim().length() > 0) {
                sb.append('(').append(seg1).append(") ");
            }
            if(seg2.trim().length() > 0) {
                sb.append(seg2).append('-');
            }
            sb.append(seg3);
            return sb.toString();
        }

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(format((Number)obj));
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return df.parseObject(source, pos);
        }
    }
    

    
    
    /**
     * Format class that does nothing and always returns a constant string.
     *
     * This format is used to simulate Excel's handling of a format string
     * of all # when the value is 0. Excel will output "", Java will output "0".
     *
     * @see DataFormatter#createFormat(double, int, String)
     */
    @SuppressWarnings("serial")
   private static final class ConstantStringFormat extends Format {
        private static final DecimalFormat df = createIntegerOnlyFormat("##########");
        private final String str;
        public ConstantStringFormat(String s) {
            str = s;
        }

        @Override
        public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
            return toAppendTo.append(str);
        }

        @Override
        public Object parseObject(String source, ParsePosition pos) {
            return df.parseObject(source, pos);
        }
    }
}