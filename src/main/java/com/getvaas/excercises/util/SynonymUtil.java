package com.getvaas.excercises.util;

import javax.annotation.Nullable;

/**
 * Utility to search for synonyms
 */
public class SynonymUtil {
    /**
     * Gets the corrresponding value from a list that matches the synonym. If there
     * are multiple matches then the first match is returned
     * 
     * @param <T>     the class of {@code values}
     * @param values  an array of values
     * @param synonym the synonym String to be matched
     * @return the synonym from the array of {@code values}. It returns {@code null} if 
     *         no match is found
     */
    public static @Nullable <T extends WithSynonym> T fromSynonym(T[] values, String synonym) {
        for (T value : values) {
            if (value.getSynonym().equals(synonym)) {
                return value;
            }
        }
        return null;
    }
}
