package com.getvaas.excercises.service.mappers.excel.model;

/**
 * Defines the behavior of a data validator
 */
public interface Validator<T> {
    /**
     * Validates if the mapped object has consistent values
     *
     * @param value value to be verified
     * @return returns a {@link ValidationResult}
     */
    ValidationResult validate(T value);
}
