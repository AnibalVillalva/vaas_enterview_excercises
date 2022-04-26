package com.getvaas.excercises.service.mappers.excel.search;

import com.getvaas.excercises.service.mappers.excel.model.Field;
import org.xlsx4j.sml.Row;
import org.xlsx4j.sml.SheetData;

import java.util.List;

public interface Searcher {
    /**
     * finds which is the row that contains the header of the data
     * @param sheet sheet where to search
     * @param values fields used to search the header
     * @return index of header
     */
    Integer searchHeaderRow(SheetData sheet, List<Field> values);

    /**
     *  finds which is the column of a particular field
     * @param row header where the field is to be searched
     * @param synonyms different strings with which the field can be found in the header
     * @return index of the column found for the field
     */
    Integer searchColumn(Row row, Field synonyms);
}
