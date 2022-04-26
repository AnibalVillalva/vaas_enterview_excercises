package com.getvaas.excercises.service.mappers.excel;

import com.getvaas.excercises.service.mappers.excel.exceptions.ExcelMapperException;
import com.getvaas.excercises.service.mappers.excel.model.*;
import com.getvaas.excercises.service.mappers.excel.search.Searcher;
import org.apache.commons.lang3.tuple.Pair;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.SpreadsheetMLPackage;
import org.docx4j.openpackaging.parts.SpreadsheetML.WorkbookPart;
import org.docx4j.openpackaging.parts.SpreadsheetML.WorksheetPart;
import org.xlsx4j.exceptions.Xlsx4jException;
import org.xlsx4j.model.CellUtils;
import org.xlsx4j.sml.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.*;


public class ExcelMapper<T> {
    /**
     *
     */
    private final Searcher searcher;
    /**
     *
     */
    private final WorkbookPart workbookPart;
    private final SpreadsheetMLPackage opcPackagePkg;
    /**
     *
     */
    CustomDataFormatter formatter = new CustomDataFormatter();
    /**
     * Variable that controls whether or not finding an error when mapping the data
     * throws an exception
     */
    private boolean strict = false;
    /**
     * ID of the error style
     */
    private Long styleErrorID = null;
    /**
     * ID of the warning style
     */
    private Long styleWarnID = null;

    public ExcelMapper(Searcher searcher, InputStream excelStream) throws IOException, Docx4JException {
        this(searcher, excelStream, false);
    }

    public ExcelMapper(Searcher searcher, InputStream excelStream, boolean strict) throws Docx4JException {
        this.searcher = searcher;
        opcPackagePkg = SpreadsheetMLPackage.load(excelStream);
        workbookPart = opcPackagePkg.getWorkbookPart();

        styleErrorID = Xlsx4jUtils.createBasicStyle(workbookPart,
                new byte[]{(byte) 255, (byte) 255, (byte) 91, (byte) 91});
        styleWarnID = Xlsx4jUtils.createBasicStyle(workbookPart,
                new byte[]{(byte) 255, (byte) 255, (byte) 255, (byte) 0});
        this.strict = strict;
    }

    /**
     * @param fields         fields to map in excel
     * @param sheetName      name of the sheet that contains the data to be mapped,
     *                       the search is done by searching if the name of the sheet contains this parameter
     * @param validator      Check if a mapped row contains correct data
     * @param populateObject help build the required object
     * @return
     * @throws Xlsx4jException
     * @throws Docx4JException
     * @throws ExcelMapperException
     */
    public MapperResponse<T> mapExcelToDTO(List<Field> fields, String sheetName, Validator<T> validator, PopulateObject<T> populateObject)
            throws Xlsx4jException, Docx4JException,
            ExcelMapperException {
        Integer indexSheet = this.searchIndexSheet(sheetName);
        WorksheetPart sheet = workbookPart.getWorksheet(indexSheet);
        SheetData sheetData = sheet.getContents().getSheetData();
        Integer headerIndex = this.searcher.searchHeaderRow(sheetData, fields);

        Row headerRow = sheetData.getRow().get(headerIndex);

        Map<Integer, FieldIndex> fieldIndexMap = new HashMap<>();
        for (Field field : fields) {
            Integer column = this.searcher.searchColumn(headerRow, field);
            if (column != null) {
                FieldIndex fieldIndex = FieldIndex.FieldIndexBuilder.builder().sheetIndex(indexSheet)
                        .columnIndex(column + field.getOffsetData()).field(field).rowIndex(headerIndex).build();
                fieldIndexMap.put(fieldIndex.getColumnIndex(), fieldIndex);
            }
        }
        MapperResponse<T> data = populateData(fieldIndexMap, sheetData, headerIndex, validator, populateObject);

        return data;
    }

    /**
     * Perform a simple data extraction, it is indicated which fields to extract,
     * from which sheet and in which row the data begins
     *
     * @param fieldIndexMap Information that allows locating the data that needs to
     *                      be extracted, contains the name of the field and its
     *                      type of data, also in which column it is
     * @param indexSheet    Indicates on which document sheet the data is located
     * @param dataStart     Indicates in which row the data begins
     * @param validator     Is responsible for determining if the extracted data is
     *                      correct, this value can be null
     * @return A list containing the data contained in the excel document
     */
    public MapperResponse<T> simpleMapExcelToDTO(Map<Integer, FieldIndex> fieldIndexMap, int indexSheet, int dataStart,
                                                 Validator<T> validator, PopulateObject<T> populateObject) throws
            Xlsx4jException, Docx4JException, ExcelMapperException {
        WorksheetPart sheet = workbookPart.getWorksheet(indexSheet);
        SheetData sheetData = sheet.getContents().getSheetData();
        MapperResponse<T> data = populateData(fieldIndexMap, sheetData, dataStart - 1, validator, populateObject);
        return data;
    }

    private Integer searchIndexSheet(String name) throws Docx4JException {
        List sheets = workbookPart.getContents().getSheets().getSheet();
        Integer index = -1;
        for (Object s :
                sheets) {
            index++;
            Sheet sheet = (Sheet) s;
            if (sheet.getName().toLowerCase().contains(name.toLowerCase())) {
                return index;
            }
        }
        return index;
    }

    private MapperResponse<T> populateData(Map<Integer, FieldIndex> fieldIndexMap, SheetData sheet, long headerRow, Validator<T> validator, PopulateObject<T> populateObject) throws ExcelMapperException {
        List<T> listData = new ArrayList<>();
        CorrectnessLevel level = CorrectnessLevel.OK;
        List<Row> rows = sheet.getRow().subList((int) headerRow + 1, sheet.getRow().size());
        for (Row row : rows) {
            Map<String, Object> rowData = new HashMap<>();
            for (Cell cell : row.getC()) {
                Pair<String, Object> cellContent = processCell(cell, fieldIndexMap);
                if (cellContent != null) {
                    rowData.put(cellContent.getLeft(), cellContent.getRight());
                }
            }
            if (!ifEmptyData(rowData)) {
                T data = populateObject.populate(rowData);
                ValidationResult result = validator != null ? validator.validate(data) : null;
                if (result == null || result.getLevel() == CorrectnessLevel.OK) {
                    listData.add(data);
                } else {
                    level = CorrectnessLevel.getHigherPriority(level, result.getLevel());
                    Xlsx4jUtils.putObservationInRow(row,
                            result.getLevel() == CorrectnessLevel.ERROR ? styleErrorID : styleWarnID,
                            result.getMessage());
                }
            }
        }
        MapperResponse<T> mapperResponse = new MapperResponse<>();
        mapperResponse.setData(listData);
        mapperResponse.setLevel(level);
        mapperResponse.setSuccessCount(listData.size());
        if (level != CorrectnessLevel.OK) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                this.opcPackagePkg.save(byteArrayOutputStream);
                mapperResponse.setFileWithObservations(byteArrayOutputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mapperResponse;
    }

    private Pair<String, Object> processCell(Cell cell, Map<Integer, FieldIndex> fieldIndexMap) throws ExcelMapperException {
        Integer index = -1;
        try {
            index = Xlsx4jUtils.getColumn(cell.getR());
        } catch (Exception ignored) {
        }
        if (index == -1)
            return null;
        FieldIndex fieldIndex = fieldIndexMap.get(index);
        if (fieldIndex == null)
            return null;
        String fieldName = fieldIndex.getField().getFieldName();
        STCellType type = cell.getT();
        if (type != STCellType.B) {
            try {
                Object val = handleType(cell, fieldIndex.getField());
                return Pair.of(fieldName, val);
            } catch (Exception e) {
                cell.setS(styleErrorID);
                if (strict)
                    throw new ExcelMapperException("Error processing cell " + cell.getR(), e.getCause());
                return Pair.of(fieldName, null);
            }
        }
        return null;
    }

    private boolean ifEmptyData(Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() != null)
                return false;
        }
        return true;
    }

    private Object handleType(Cell cell, Field field) {
        STCellType type = cell.getT();
        if (type == STCellType.B)
            return null;
        switch (field.getDataType()) {
            case LONG:
                if (type == STCellType.N)
                    return ((Double) CellUtils.getNumericCellValue(cell)).longValue();
                return (long) Double.parseDouble(formatter.formatCellValue(cell));
            case INT:
                if (type == STCellType.N)
                    return ((Double) CellUtils.getNumericCellValue(cell)).intValue();
                return Integer.parseInt(formatter.formatCellValue(cell));
            case STRING:
                if (type == STCellType.N) {
                    Double doubleValue = CellUtils.getNumericCellValue(cell);
                    if (doubleValue % 1 == 0) {
                        return Long.toString(doubleValue.longValue());
                    } else {
                        return doubleValue.toString();
                    }
                }
                try {
                    return formatter.formatCellValue(cell);
                } catch (Exception e) {
                    return "Error";
                }
            case DOUBLE:
                if (type == STCellType.S) {
                    String s = formatter.formatCellValue(cell);
                    return Double.parseDouble(s);
                }
                return CellUtils.getNumericCellValue(cell);
            case LOCAL_DATE:
                try {
                    Date date = CellUtils.getDateCellValue(cell);
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                } catch (IllegalStateException | NumberFormatException e) {
                    String dateString = formatter.formatCellValue(cell);
                    return DateUtils.stringToDate(dateString);
                }
            default:
                // TODO: lanzar exception
                return null;
        }
    }

}
