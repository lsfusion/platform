package lsfusion.server.logics.property.actions.importing.xls;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import lsfusion.server.logics.property.actions.importing.IncorrectFileException;
import org.apache.poi.hssf.OldExcelFormatException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportXLSIterator extends ImportIterator {
    private final List<Integer> columns;
    private final ImOrderSet<LCP> properties;
    private int current;
    private HSSFSheet sheet;
    private int lastRow;
    
    public ImportXLSIterator(byte[] file, List<Integer> columns, ImOrderSet<LCP> properties, Integer sheetIndex) throws IOException, IncorrectFileException {
        this.columns = columns;
        this.properties = properties;

        try {
            HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(file));

            sheet = wb.getSheetAt(sheetIndex == null ? 0 : (sheetIndex - 1));
            lastRow = sheet.getLastRowNum();
            lastRow = lastRow == 0 && sheet.getRow(0) == null ? 0 : (lastRow + 1);
        } catch (OldExcelFormatException e) {
            throw new IncorrectFileException(e.getMessage());
        }
    }

    @Override
    public List<String> nextRow() {
        List<String> listRow = new ArrayList<>();
        try {
            HSSFRow hssfRow = sheet.getRow(current);
            if (hssfRow != null) {
                for (Integer column : columns) {
                    try {
                        listRow.add(getXLSFieldValue(hssfRow, column, null));
                    } catch (Exception e) {
                        throw new RuntimeException(String.format("Error parsing row %s, column %s", current, column+1), e);
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        current++;
        return current > lastRow ? null : listRow;
    }

    protected String getXLSFieldValue(HSSFRow hssfRow, int cell, String defaultValue) throws ParseException {
        if (hssfRow != null) {
            HSSFCell hssfCell = hssfRow.getCell(cell);
            if (hssfCell != null) {
                DateFormat dateFormat = getDateFormat(properties, columns, cell);
                switch (hssfCell.getCellType()) {
                    case Cell.CELL_TYPE_NUMERIC:
                    case Cell.CELL_TYPE_FORMULA:
                        String result;
                        try {
                            if (dateFormat != null) {
                                result = dateFormat.format(hssfCell.getDateCellValue());
                            } else {
                                result = new DecimalFormat("#.#####").format(hssfCell.getNumericCellValue());
                            }
                        } catch (Exception e) {
                            result = hssfCell.getStringCellValue().isEmpty() ? defaultValue : hssfCell.getStringCellValue();
                        }
                        return result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
                    case Cell.CELL_TYPE_BOOLEAN:
                        return String.valueOf(hssfCell.getBooleanCellValue());
                    case Cell.CELL_TYPE_STRING:
                    default:
                        if(dateFormat != null) {
                            result = (hssfCell.getStringCellValue().isEmpty()) ? defaultValue : hssfCell.getStringCellValue();
                            if(result != null)
                                result = parseFormatDate(dateFormat, result);
                        } else {
                            result = (hssfCell.getStringCellValue().isEmpty()) ? defaultValue : hssfCell.getStringCellValue();
                        }
                        return result;
                }
            }
        }
        return defaultValue;
    }

    @Override
    protected void release() {
    }
}
