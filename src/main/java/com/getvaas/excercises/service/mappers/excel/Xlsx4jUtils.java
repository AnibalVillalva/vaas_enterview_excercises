package com.getvaas.excercises.service.mappers.excel;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.parts.SpreadsheetML.Styles;
import org.docx4j.openpackaging.parts.SpreadsheetML.WorkbookPart;
import org.xlsx4j.jaxb.Context;
import org.xlsx4j.sml.*;

import java.util.List;

/**
 * Various methods for dealing with the Xlsx4jUtils low level API
 */
public class Xlsx4jUtils {
    /**
     * Gets the first row from a reference range. This field must follow the pattern
     * begin:end for xlsx files, e.g. A3:C5
     *
     * @param range the reference range
     * @return the number of the first row of the range (0-based)
     * @throws IllegalArgumentException if the range doesn't match the required
     *                                  pattern
     */
    public static int getFirstRowFromRange(String range) throws IllegalArgumentException {
        checkRangeFormat(range);
        return getRow(range.split(":")[0]);
    }

    /**
     * Gets the last row from a reference range. This field must follow the pattern
     * begin:end for xlsx files, e.g. A3:C5
     *
     * @param range the reference range
     * @return the number of the last row of the range (0-based)
     * @throws IllegalArgumentException if the range doesn't match the required
     *                                  pattern
     */
    public static int getLastRowFromRange(String range) throws IllegalArgumentException {
        checkRangeFormat(range);
        return getRow(range.split(":")[1]);
    }

    /**
     * Gets the row value from an address that follows the xlsx pattern, e.g. C5
     *
     * @param address the address
     * @return the row number (0-based)
     * @throws IllegalArgumentException if the address doesn't match the required
     *                                  pattern
     */
    public static int getRow(String address) throws IllegalArgumentException {
        checkAddressFormat(address);
        return Integer.valueOf(address.replaceAll("[A-Z]", "")) - 1;
    }

    /**
     * Gets the first column from a reference range. This field must follow the
     * pattern begin:end for xlsx files, e.g. A3:C5
     *
     * @param range the reference range
     * @return the number of the first column of the range (0-based)
     * @throws IllegalArgumentException if the range doesn't match the required
     *                                  pattern
     */
    public static int getFirstColumnFromRange(String range) throws IllegalArgumentException {
        checkRangeFormat(range);
        return getColumn(range.split(":")[0]);
    }

    /**
     * Gets the last column from a reference range. This field must follow the
     * pattern begin:end for xlsx files, e.g. A3:C5
     *
     * @param range the reference range
     * @return the number of the last column of the range (0-based)
     * @throws IllegalArgumentException if the range doesn't match the required
     *                                  pattern
     */
    public static int getLastColumnFromRange(String range) throws IllegalArgumentException {
        checkRangeFormat(range);
        return getColumn(range.split(":")[1]);
    }

    /**
     * Gets the row value from an address that follows the xlsx pattern, e.g. C5
     *
     * @param address the address
     * @return the number of the column (0-based)
     * @throws IllegalArgumentException if the address doesn't match the required
     *                                  pattern
     */
    public static int getColumn(String address) throws IllegalArgumentException {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        checkAddressFormat(address);
        String column = address.replaceAll("[0-9]", "");
        int colIndex = 0;
        int multiplier = 1;
        for (int i = column.length() - 1; i > -1; i--) {
            colIndex += (alphabet.indexOf(column.charAt(i)) + 1) * multiplier;
            multiplier *= alphabet.length();
        }
        return colIndex - 1;
    }

    /**
     * Gets the number of sheets in the workbook
     *
     * @param workbook the workbook
     * @return the number of sheets
     */
    public static int getSheetsCount(WorkbookPart workbook) {
        try {
            return workbook.getContents().getSheets().getSheet().size();
        } catch (Docx4JException e) {
            return 0;
        }
    }

    protected static void checkRangeFormat(String range) {
        if (!range.matches("[A-Z]+[1-9][0-9]*:[A-Z]+[1-9][0-9]*"))
            throw new IllegalArgumentException();
    }

    protected static void checkAddressFormat(String address) {
        if (!address.matches("[A-Z]+[1-9][0-9]*"))
            throw new IllegalArgumentException();
    }

    protected static CTXf generateCTXf(Long borderId, Long XfId, Long numFmtId, Long fontId, Long fillId,
            CTCellAlignment alignment, Boolean applyBorder) {
        CTXf xf = new CTXf();
        if (applyBorder != null)
            xf.setApplyBorder(applyBorder);
        if (borderId != null)
            xf.setBorderId(borderId);
        if (XfId != null)
            xf.setXfId(XfId);
        if (numFmtId != null)
            xf.setNumFmtId(numFmtId);
        if (fontId != null)
            xf.setFontId(fontId);
        if (fillId != null)
            xf.setFillId(fillId);
        if (alignment != null)
            xf.setAlignment(alignment);
        xf.setApplyFill(true);
        return xf;
    }

    /**
     * Creates a new style
     *
     * @return identifier of the new cellXfs
     */
    protected static Long createBasicStyle(WorkbookPart workbookPart, byte[] color) {
        try {
            Styles styles = workbookPart.getStylesPart();
            CTStylesheet ctStylesheet = styles.getContents();
            CTCellXfs ctCellXfs = ctStylesheet.getCellXfs();
            CTFills ctFills = ctStylesheet.getFills();

            Long nFills = ctFills.getCount();
            Long nCellStylesXfs = ctCellXfs.getCount();
            List<CTXf> ctCellXfsList = ctCellXfs.getXf();
            List<CTFill> fillsList = ctFills.getFill();

            CTFill ctFill = new CTFill();
            CTPatternFill ctPatternFill = new CTPatternFill();
            ctPatternFill.setPatternType(STPatternType.SOLID);
            CTColor ctColor = new CTColor();
            ctColor.setRgb(color);
            ctColor.setIndexed(64L);
            CTColor ctColorFg = new CTColor();
            ctColorFg.setRgb(color);
            ctColorFg.setIndexed(64L);
            ctPatternFill.setBgColor(ctColor);
            ctPatternFill.setFgColor(ctColorFg);
            ctFill.setPatternFill(ctPatternFill);
            fillsList.add(ctFill);
            ctFills.setCount(nFills + 1);
            long idFill = nFills;
            CTCellAlignment cellAlignment = new CTCellAlignment();
            cellAlignment.setWrapText(true);
            cellAlignment.setHorizontal(STHorizontalAlignment.JUSTIFY);
            cellAlignment.setVertical(STVerticalAlignment.JUSTIFY);
            ctCellXfsList.add(generateCTXf(0L, 0L, null, 0L, idFill, cellAlignment, null));
            ctCellXfs.setCount(nCellStylesXfs + 1);
            return nCellStylesXfs;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    /**
     * Modifies a row by appending an observation as a cell at the end
     *
     * @param row         row that could not be mapped
     * @param style       the new style of the row
     * @param observation the observation to put
     */
    public static void putObservationInRow(Row row, Long style, String observation) {
        for (Cell cell : row.getC()) {
            cell.setS(style);
        }
        Cell cell = Context.getsmlObjectFactory().createCell();
        CTXstringWhitespace ctx = Context.getsmlObjectFactory().createCTXstringWhitespace();
        ctx.setValue(observation);
        CTRst ctrst = new CTRst();
        ctrst.setT(ctx);
        cell.setT(STCellType.INLINE_STR);
        cell.setIs(ctrst);
        row.getC().add(cell);
    }
}