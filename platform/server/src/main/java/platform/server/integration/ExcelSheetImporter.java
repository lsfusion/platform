package platform.server.integration;

import platform.base.OrderedMap;
import platform.base.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 10.02.11
 * Time: 18:03
 */

public abstract class ExcelSheetImporter {

    protected jxl.Sheet sheet;
    private OrderedMap<ImportField, Pair<Integer, Integer>> fieldPosition = new OrderedMap<ImportField, Pair<Integer, Integer>>();

    public ExcelSheetImporter(jxl.Sheet sheet, List<ImportField> nullFields, Object... fields) {
        this.sheet = sheet;
        for (int i = 0; i < fields.length; i++) {
            if (fields[i] instanceof ImportField[]) {
                ImportField[] subFields = (ImportField[]) fields[i];
                for (int j = 0; j < subFields.length; j++) {
                    fieldPosition.put(subFields[j], new Pair<Integer, Integer>(i, j));
                }
            } else if (fields[i] instanceof ImportField) {
                fieldPosition.put((ImportField) fields[i], new Pair<Integer, Integer>(i, 0));
            }
        }

        if (nullFields != null) {
            for (ImportField field : nullFields) {
                fieldPosition.put(field, null);
            }
        }
    }

    protected abstract boolean isCorrectRow(int rowNum);

    protected abstract String transformValue(int column, int part, String value);

    public ImportTable getTable() throws platform.server.data.type.ParseException {
        List<List<Object>> data = new ArrayList<List<Object>>();

        for (int i = 0; i < sheet.getRows(); ++i) {
            if (isCorrectRow(i)) {
                List<Object> row = new ArrayList<Object>();
                for (Map.Entry<ImportField, Pair<Integer, Integer>> entry : fieldPosition.entrySet()) {
                    ImportField field = entry.getKey();
                    Object rowValue = null;
                    if (entry.getValue() != null) {
                        String cellValue = sheet.getCell(entry.getValue().first, i).getContents();
                        if (!cellValue.trim().equals("")) {
                            cellValue = transformValue(entry.getValue().first, entry.getValue().second, cellValue);
                            rowValue = field.getFieldClass().parseString(cellValue);
                        }
                    }
                    row.add(rowValue);
                }
                data.add(row);
            }
        }

        return new ImportTable(fieldPosition.keyList(), data);
    }

}
