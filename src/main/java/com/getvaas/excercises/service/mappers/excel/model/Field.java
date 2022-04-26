package com.getvaas.excercises.service.mappers.excel.model;

import java.util.List;

public class Field {
    private String fieldName;
    private List<String> synonyms;
    private DataType dataType;
    /**
     * Number of offset columns of the data, it is applied when the data is not in
     * the same column of the header
     */
    private int offsetData;

    public Field(String fieldName, DataType dataType) {
        this.fieldName = fieldName;
        this.dataType = dataType;
    }

    public Field(String fieldName, List<String> synonyms, DataType dataType, int offsetData) {
        this.fieldName = fieldName;
        this.synonyms = synonyms;
        this.dataType = dataType;
        this.offsetData = offsetData;
    }

    public Field() {
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.synonyms = synonyms;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public int getOffsetData() {
        return offsetData;
    }

    public void setOffsetData(int offsetData) {
        this.offsetData = offsetData;
    }

    public static final class Builder {
        private final Field field;

        private Builder() {
            field = new Field();
        }

        public static Builder aField() {
            return new Builder();
        }

        public Builder fieldName(String fieldName) {
            field.setFieldName(fieldName);
            return this;
        }

        public Builder synonyms(List<String> synonyms) {
            field.setSynonyms(synonyms);
            return this;
        }

        public Builder dataType(DataType dataType) {
            field.setDataType(dataType);
            return this;
        }

        public Builder offsetData(int offset) {
            field.setOffsetData(offset);
            return this;
        }

        public Field build() {
            return field;
        }
    }
}
