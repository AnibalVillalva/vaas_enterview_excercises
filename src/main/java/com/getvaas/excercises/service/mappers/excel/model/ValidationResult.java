package com.getvaas.excercises.service.mappers.excel.model;

/**
 * The result of a validation of an excel row
 */
public class ValidationResult {
    /**
     * The message of the validation
     */
    private String message;
    /**
     * Indicates the result of the validation, i.e. the level of correctness
     */
    private CorrectnessLevel level;

    public ValidationResult(String message, CorrectnessLevel level) {
        this.message = message != null ? message : "";
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public CorrectnessLevel getLevel() {
        return level;
    }

    public void setLevel(CorrectnessLevel level) {
        this.level = level;
    }
}
