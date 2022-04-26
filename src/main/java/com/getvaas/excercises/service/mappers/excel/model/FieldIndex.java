package com.getvaas.excercises.service.mappers.excel.model;

/**
 * Represent the position of a field in the excel file
 * *
 */
public class FieldIndex {
    private Field field;
    private Integer sheetIndex;
    private Integer columnIndex;
    private Integer rowIndex;

    public FieldIndex(Field field, Integer sheetIndex, Integer columnIndex, Integer rowIndex) {
        this.field = field;
        this.sheetIndex = sheetIndex;
        this.columnIndex = columnIndex;
        this.rowIndex = rowIndex;
    }

    public FieldIndex() {

    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Integer getSheetIndex() {
        return sheetIndex;
    }

    public void setSheetIndex(Integer sheetIndex) {
        this.sheetIndex = sheetIndex;
    }

    public Integer getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(Integer columnIndex) {
        this.columnIndex = columnIndex;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public static final class FieldIndexBuilder {
        private Field field;
        private Integer sheetIndex;
        private Integer columnIndex;
        private Integer rowIndex;

        private FieldIndexBuilder() {
        }

        public static FieldIndexBuilder builder() {
            return new FieldIndexBuilder();
        }

        public FieldIndexBuilder field(Field field) {
            this.field = field;
            return this;
        }

        public FieldIndexBuilder sheetIndex(int sheetIndex) {
            this.sheetIndex = sheetIndex;
            return this;
        }

        public FieldIndexBuilder columnIndex(int columnIndex) {
            this.columnIndex = columnIndex;
            return this;
        }

        public FieldIndexBuilder rowIndex(int rowIndex) {
            this.rowIndex = rowIndex;
            return this;
        }

        public FieldIndex build() {
            FieldIndex fieldIndex = new FieldIndex();
            fieldIndex.setField(field);
            fieldIndex.setSheetIndex(sheetIndex);
            fieldIndex.setColumnIndex(columnIndex);
            fieldIndex.setRowIndex(rowIndex);
            return fieldIndex;
        }
    }
}
