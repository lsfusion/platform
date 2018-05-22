package lsfusion.server.logics.property.actions.importing.xls;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.ss.usermodel.Cell;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.List;

public abstract class ImportXLSIterator extends ImportIterator {
    protected final List<Integer> columns;
    protected final ImOrderSet<LCP> properties;
    
    public ImportXLSIterator(List<Integer> columns, ImOrderSet<LCP> properties) {
        this.columns = columns;
        this.properties = properties;
    }

    protected String getXLSFieldValue(HSSFRow hssfRow, int cell, String defaultValue) {
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