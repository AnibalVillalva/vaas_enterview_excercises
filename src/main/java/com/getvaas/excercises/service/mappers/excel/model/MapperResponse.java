package com.getvaas.excercises.service.mappers.excel.model;


import java.io.OutputStream;
import java.util.List;

/**
 * Wraps the excel mapper's response,
 * contains a list of the values it was able to map, and if there was an error,
 * this contain a file with  original excel file with the errors highlighted
 *
 * @param <T>
 */
public class MapperResponse<T> {
    /**
     * Data that was mapped correctly
     */
    private List<T> data;
    /**
     * Indicates the result of the validation, i.e. the level of correctness
     */
    private CorrectnessLevel level;
    /**
     * Original Excel document with rows where there are errors or warnings highlighted
     */
    private OutputStream fileWithObservations;
    /**
     * Number of items loaded successfully
     */
    private Integer successCount;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public OutputStream getFileWithObservations() {
        return fileWithObservations;
    }

    public void setFileWithObservations(OutputStream fileWithObservations) {
        this.fileWithObservations = fileWithObservations;
    }

    public CorrectnessLevel getLevel() {
        return level;
    }

    public void setLevel(CorrectnessLevel level) {
        this.level = level;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }
}
