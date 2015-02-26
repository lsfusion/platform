package lsfusion.server.logics.property.actions.importing;

import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.DateTimeClass;
import lsfusion.server.classes.TimeClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportXLSDataActionProperty extends ImportDataActionProperty {
    public final static Map<String, Integer> XLSColumnsMapping = new HashMap<String, Integer>() {{
        put("A", 0); put("B", 1); put("C", 2); put("D", 3); put("E", 4); put("F", 5); put("G", 6); put("H", 7); put("I", 8);
        put("J", 9); put("K", 10); put("L", 11); put("M", 12); put("N", 13); put("O", 14); put("P", 15); put("Q", 16);
        put("R", 17); put("S", 18); put("T", 19); put("U", 20); put("V", 21); put("W", 22); put("X", 23); put("Y", 24);
        put("Z", 25); put("AA", 26); put("AB", 27); put("AC", 28); put("AD", 29); put("AE", 30); put("AF", 31); put("AG", 32);
        put("AH", 33); put("AI", 34); put("AJ", 35); put("AK", 36); put("AL", 37); put("AM", 38); put("AN", 39); put("AO", 40);
        put("AP", 41); put("AQ", 42); put("AR", 43); put("AS", 44); put("AT", 45); put("BA", 52); put("BB", 53); put("BC", 54);
    }};
    
    public ImportXLSDataActionProperty(ValueClass valueClass, ScriptingLogicsModule LM, List<String> ids, List<LCP> properties) {
        super(valueClass, LM, ids, properties);
    }

    @Override
    public List<List<String>> getTable(byte[] file) throws IOException, ParseException {
        HSSFWorkbook wb = new HSSFWorkbook(new ByteArrayInputStream(file));
        HSSFSheet sheet = wb.getSheetAt(0);

        List<List<String>> result = new ArrayList<List<String>>();

        List<Integer> columns = getSourceColumns(XLSColumnsMapping);
        for (int i = 0; i < sheet.getLastRowNum(); i++) {
            HSSFRow row = sheet.getRow(i);
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
                    listRow.add(getXLSFieldValue(sheet, i, column, dateFormat, null));
                }
                result.add(listRow);
            }
        }
        return result;
    }

    protected String getXLSFieldValue(HSSFSheet sheet, int row, int cell, DateFormat dateFormat, String defaultValue) throws ParseException {
        HSSFRow hssfRow = sheet.getRow(row);
        if (hssfRow == null) return defaultValue;
        HSSFCell hssfCell = hssfRow.getCell(cell);
        if (hssfCell == null) return defaultValue;
        switch (hssfCell.getCellType()) {
            case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC:
            case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA:
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
            case org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING:
            default:
                return (hssfCell.getStringCellValue().isEmpty()) ? defaultValue : hssfCell.getStringCellValue();
        }
    }
}
