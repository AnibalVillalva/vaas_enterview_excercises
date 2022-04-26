package com.getvaas.excercises.service.mappers.excel.model;

/**
 * Enumeration with the types of results of a validation
 */
public enum CorrectnessLevel {
    /**
     * No error was found according to the validation
     */
    OK(1),
    /**
     * Warnings were found
     */
    WARN(2),
    /**
     * Errors were found. Warnings may also have been found
     */
    ERROR(3);

    /**
     * The priority of the correctness level
     */
    private Integer priority;

    private CorrectnessLevel(Integer priority) {
        this.priority = priority;
    }

    /**
     * Gets the {@link CorrectnessLevel} with the higher priority
     * 
     * @param a the first value to compare
     * @param b the second value to compare
     * @return the value with the highest priority from the two given
     */
    public static CorrectnessLevel getHigherPriority(CorrectnessLevel a, CorrectnessLevel b) {
        if (a == null)
            return b;
        if (b == null)
            return a;
        return a.priority > b.priority ? a : b;
    }
}
