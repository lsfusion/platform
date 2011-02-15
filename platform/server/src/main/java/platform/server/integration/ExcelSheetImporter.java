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
    protected final static int A = 0, B = 1, C = 2, D = 3, E = 4, F = 5, G = 6, H = 7, I = 8, J = 9, K = 10, L = 11, M = 12,
            N = 13, O = 14, P = 15, Q = 16, R = 17, S = 18, T = 19, U = 20, V = 21, W = 22, X = 23, Y = 24, Z = 25,
            AA = 26, AB = 27, AC = 28, AD = 29, AE = 30, AF = 31, AG = 32, AH = 33, AI = 34, AJ = 35;

    protected jxl.Sheet sheet;
    protected OrderedMap<ImportField, Pair<Integer, Integer>> fieldPosition = new OrderedMap<ImportField, Pair<Integer, Integer>>();
    protected int currentRow;

    public ExcelSheetImporter(jxl.Sheet sheet, Object... fields) {
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
    }

    protected abstract boolean isCorrectRow(int rowNum);

    protected String transformValue(int row, int column, int part, String value) {
        return value;
    }

    protected Object getResultObject(ImportField field, String value) throws platform.server.data.type.ParseException {
        if (!value.trim().equals("")) {
            return field.getFieldClass().parseString(value);
        } else {
            return null;
        }
    }

    protected String getCellString(int row, int column) {
        return sheet.getCell(column, row).getContents();
    }

    public ImportTable getTable() throws platform.server.data.type.ParseException {
        List<List<Object>> data = new ArrayList<List<Object>>();
        currentRow = -1;

        for (int i = 0; i < sheet.getRows(); ++i) {
            if (isCorrectRow(i)) {
                ++currentRow;
                List<Object> row = new ArrayList<Object>();
                for (Map.Entry<ImportField, Pair<Integer, Integer>> entry : fieldPosition.entrySet()) {
                    ImportField field = entry.getKey();
                    String cellValue = getCellString(i, entry.getValue().first);
                    String transformedValue = transformValue(i, entry.getValue().first, entry.getValue().second, cellValue);
                    row.add(getResultObject(field, transformedValue));
                }
                data.add(row);
            }
        }

        return new ImportTable(fieldPosition.keyList(), data);
    }

}
