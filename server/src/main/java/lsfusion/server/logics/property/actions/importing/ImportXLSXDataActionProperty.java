package lsfusion.server.logics.property.actions.importing;

import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.TimeClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
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

public class ImportXLSXDataActionProperty extends ImportDataActionProperty {
    public ImportXLSXDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClass, LM, ids, properties);
    }

    @Override
    public List<List<String>> getTable(byte[] file) throws IOException, ParseException {
        XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(file));
        XSSFSheet sheet = wb.getSheetAt(0);

        List<List<String>> result = new ArrayList<List<String>>();

        List<Integer> columns = getSourceColumns(ImportXLSDataActionProperty.XLSColumnsMapping);
        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            XSSFRow row = sheet.getRow(i);
            if (row != null) {
                List<String> listRow = new ArrayList<String>();
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
                    listRow.add(getXLSXFieldValue(sheet, i, column, dateFormat, null));
                }
                result.add(listRow);
            }
        }

        return result;
    }

    protected String getXLSXFieldValue(XSSFSheet sheet, Integer row, Integer cell, DateFormat dateFormat, String defaultValue) throws ParseException {
        if (cell == null) return defaultValue;
        XSSFRow xssfRow = sheet.getRow(row);
        if (xssfRow == null) return defaultValue;
        XSSFCell xssfCell = xssfRow.getCell(cell);
        if (xssfCell == null) return defaultValue;
        String result;
        switch (xssfCell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC:
                if (dateFormat != null) {
                    result = dateFormat.format(xssfCell.getDateCellValue());
                }                                                           else {
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
                break;
        }
        return result;
    }
}
