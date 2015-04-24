package lsfusion.server.logics.property.actions.importing.xlsx;

import com.google.common.base.Throwables;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.TimeClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
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
import java.util.List;

public class ImportXLSXIterator extends ImportIterator {
    private final List<Integer> columns;
    private final List<LCP> properties;

    private int current;
    private XSSFSheet sheet;

    public ImportXLSXIterator(byte[] file, List<Integer> columns, List<LCP> properties, Integer sheetIndex) throws IOException {
        this.columns = columns;
        this.properties = properties;

        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(file));
        sheet = wb.getSheetAt(sheetIndex == null ? 0 : (sheetIndex - 1));
    }

    @Override
    public List<String> nextRow() {
        XSSFRow row = sheet.getRow(current);
        if (row != null) {
            List<String> listRow = new ArrayList<String>();
            try {
                for (Integer column : columns) {
                    ValueClass valueClass = properties.get(columns.indexOf(column)).property.getValueClass(ClassType.valuePolicy);
                    DateFormat dateFormat = null;
                    if (valueClass instanceof DateClass) {
                        dateFormat = DateClass.getDateFormat();
                    } else if (valueClass instanceof TimeClass) {
                        dateFormat = ((TimeClass) valueClass).getDefaultFormat();
                    } else if (valueClass instanceof DateTimeClass) {
                        dateFormat = DateTimeClass.getDateTimeFormat();
                    }
                    listRow.add(getXLSXFieldValue(sheet, current, column, dateFormat, null));
                }
            } catch (ParseException e) {
                Throwables.propagate(e);
            }
            current++;
            return listRow;
        }
        return null;
    }

    protected String getXLSXFieldValue(XSSFSheet sheet, Integer row, Integer cell, DateFormat dateFormat, String defaultValue) throws ParseException {
        String result = defaultValue;
        if (cell != null) {
            XSSFRow xssfRow = sheet.getRow(row);
            if (xssfRow != null) {
                XSSFCell xssfCell = xssfRow.getCell(cell);
                if (xssfCell != null) {
                    switch (xssfCell.getCellType()) {
                        case Cell.CELL_TYPE_NUMERIC:
                            if (dateFormat != null) {
                                result = dateFormat.format(xssfCell.getDateCellValue());
                            } else {
                                result = new DecimalFormat("#.#####").format(xssfCell.getNumericCellValue());
                                result = result.endsWith(".0") ? result.substring(0, result.length() - 2) : result;
                            }
                            break;
                        case Cell.CELL_TYPE_FORMULA:
                            result = xssfCell.getCellFormula();
                            break;
                        case Cell.CELL_TYPE_STRING:
                        default:
                            result = (xssfCell.getStringCellValue().isEmpty()) ? defaultValue : xssfCell.getStringCellValue().trim();
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected void release() {
    }
}
