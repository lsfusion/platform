package lsfusion.server.logics.property.actions.importing.xls;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public abstract class ImportXLSXIterator extends ImportIterator {
    protected final List<Integer> columns;
    protected final ImOrderSet<LCP> properties;

    public ImportXLSXIterator(List<Integer> columns, ImOrderSet<LCP> properties) {
        this.columns = columns;
        this.properties = properties;
    }

    protected String getXLSXFieldValue(XSSFRow xssfRow, Integer cell, String defaultValue) {
        String result = defaultValue;
        if (cell != null && xssfRow != null) {
            XSSFCell xssfCell = xssfRow.getCell(cell);
            if (xssfCell != null) {
                DateFormat dateFormat = getDateFormat(properties, columns, cell);
                switch (xssfCell.getCellType()) {
                    case Cell.CELL_TYPE_NUMERIC:
                        if (dateFormat != null) {
                            Date value = getDateValue(xssfCell);
                            if(value != null)
                                result = dateFormat.format(value);
                        } else {
                            result = new DecimalFormat("#.#####").format(xssfCell.getNumericCellValue());
                            result = result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
                        }
                        break;
                    case Cell.CELL_TYPE_FORMULA:
                        try {
                            if (dateFormat != null) {
                                String formulaCellValue = getFormulaCellValue(xssfCell);
                                result = formulaCellValue.isEmpty() ? defaultValue : formulaCellValue.trim();
                                if(result != null)
                                    result = parseFormatDate(dateFormat, result);
                            } else {
                                result = new DecimalFormat("#.#####").format(xssfCell.getNumericCellValue());
                            }
                        } catch (Exception e) {
                            String formulaCellValue = getFormulaCellValue(xssfCell);
                            result = formulaCellValue.isEmpty() ? defaultValue : formulaCellValue;
                        }
                        result = result != null && result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
                        break;
                    case Cell.CELL_TYPE_ERROR:
                        result = null;
                        break;
                    case Cell.CELL_TYPE_BOOLEAN:
                        result = String.valueOf(xssfCell.getBooleanCellValue());
                        break;
                    case Cell.CELL_TYPE_STRING:
                    default:
                        if(dateFormat != null) {
                            result = (xssfCell.getStringCellValue().isEmpty()) ? defaultValue : xssfCell.getStringCellValue().trim();
                            if(result != null)
                                result = parseFormatDate(dateFormat, result);
                        } else {
                            result = (xssfCell.getStringCellValue().isEmpty()) ? defaultValue : xssfCell.getStringCellValue().trim();
                        }
                }
            }
        }
        return result;
    }

    protected String getFormulaCellValue(Cell xssfCell) {
        String result;
        switch (xssfCell.getCachedFormulaResultType()) {
            case 0:
                result = String.valueOf(xssfCell.getNumericCellValue());
                break;
            default:
                result = xssfCell.getStringCellValue();
                break;
        }
        return result;
    }

    @Override
    protected void release() {
    }

    protected Date getDateValue(Cell xssfCell) {
        try {
            return DateUtils.round(xssfCell.getDateCellValue(), Calendar.SECOND);
        } catch (Exception ignored) {
            return null;
        }
    }
}