package com.getvaas.excercises.util;

/**
 * Contains various utilities for String handling
 */
public class StringUtil {

    /**
     * Determines if a string is a number or not
     * 
     * @param strNum the String to be validated
     * @return {@code true} if the String is a valid decimal or integer
     */
    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
