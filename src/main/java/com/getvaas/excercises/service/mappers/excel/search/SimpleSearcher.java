package com.getvaas.excercises.service.mappers.excel.search;


import com.getvaas.excercises.service.mappers.excel.CustomDataFormatter;
import com.getvaas.excercises.service.mappers.excel.Xlsx4jUtils;
import com.getvaas.excercises.service.mappers.excel.model.Field;
import org.xlsx4j.sml.Cell;
import org.xlsx4j.sml.Row;
import org.xlsx4j.sml.STCellType;
import org.xlsx4j.sml.SheetData;

import java.text.Normalizer;
import java.util.List;

public class SimpleSearcher implements Searcher {
    CustomDataFormatter formatter = new CustomDataFormatter();

    public SimpleSearcher() {
    }

    private static String stripAccents(String s) {
        s = s.trim();
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return s;
    }

    @Override
    public Integer searchHeaderRow(SheetData sheet, List<Field> fields) {
        int ind = -1;
        for (Row row : sheet.getRow()) {
            ind++;
            if (!rowContainString(row))
                continue;
            if (countFieldCoincidencesInRow(fields, row) > 0)
                return ind;
        }
        return null;
    }

    private long countFieldCoincidencesInRow(List<Field> fields, Row row) {
        return row.getC().stream().mapToLong(cell -> fields.stream().parallel().filter(field -> {
            return matchWithList(cell, field.getSynonyms());
        }).count()).sum();
    }

    /**
     * check if line contains data that are mostly string
     */
    private boolean rowContainString(Row row) {
        for (Cell cell : row.getC()) {
            STCellType type = cell.getT();
            if (type == STCellType.S || type == STCellType.INLINE_STR)
                return true;
        }
        return false;
    }

    public boolean matchWithList(Cell cell, List<String> values) {
        if (cell.getT() != STCellType.S && cell.getT() != STCellType.INLINE_STR) {
            return false;
        }
        String cellV = formatter.formatCellValue(cell);
        long count = values.stream().parallel()
                .filter(value -> value.equalsIgnoreCase(cellV))
                .count();
        return count > 0;
    }

    @Override
    public Integer searchColumn(Row row, Field field) {
        for (Cell cell : row.getC()) {
            if (matchCell(field, cell)) {
                return Xlsx4jUtils.getColumn(cell.getR());
            }
        }
        return null;
    }

    /**
     * verify if a un cellname match with a synonyms list
     *
     * @return
     */
    public boolean matchCell(Field field, Cell cell) {
        if (cell != null
                && formatter.formatCellValue(cell) != null
                && stripAccents(formatter.formatCellValue(cell)).equalsIgnoreCase(stripAccents(field.getFieldName())))
            return true;

        for (String synonym : field.getSynonyms()) {
            if (cell != null
                    && formatter.formatCellValue(cell) != null
                    && stripAccents(formatter.formatCellValue(cell)).equalsIgnoreCase(stripAccents(synonym))) {
                return true;
            }
        }
        return false;
    }

}
