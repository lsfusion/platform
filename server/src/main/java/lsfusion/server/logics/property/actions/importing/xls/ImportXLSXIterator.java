package lsfusion.server.logics.property.actions.importing.xls;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.actions.importing.ImportIterator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ImportXLSXIterator extends ImportIterator {
    private final List<Integer> columns;
    private final ImOrderSet<LCP> properties;
    private int current;
    private XSSFSheet sheet;
    private int lastRow;

    public ImportXLSXIterator(byte[] file, List<Integer> columns, ImOrderSet<LCP> properties, Integer sheetIndex) throws IOException {
        this.columns = columns;
        this.properties = properties;

        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(file));
        sheet = wb.getSheetAt(sheetIndex == null ? 0 : (sheetIndex - 1));
        lastRow = sheet.getLastRowNum();
        lastRow = lastRow == 0 && sheet.getRow(0) == null ? 0 : (lastRow + 1);
    }

    @Override
    public List<String> nextRow() {
        List<String> listRow = new ArrayList<>();
        try {
            XSSFRow xssfRow = sheet.getRow(current);
            if (xssfRow != null) {
                for (Integer column : columns) {
                    try {
                        listRow.add(getXLSXFieldValue(xssfRow, column, null));
                    } catch (Exception e) {
                        throw new RuntimeException(String.format("Error parsing row %s, column %s", current, column), e);
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        current++;
        return current > lastRow ? null : listRow;
    }

    protected String getXLSXFieldValue(XSSFRow xssfRow, Integer cell, String defaultValue) throws ParseException {
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

    private String getFormulaCellValue(XSSFCell xssfCell) {
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

    private Date getDateValue(XSSFCell xssfCell) {
        try {
            return xssfCell.getDateCellValue();
        } catch (Exception ignored) {
            return null;
        }
    }
}
