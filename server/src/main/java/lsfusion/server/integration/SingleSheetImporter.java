package lsfusion.server.integration;

import lsfusion.base.OrderedMap;
import lsfusion.base.Pair;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: DAle
 * Date: 10.02.11
 * Time: 18:03
 */

public abstract class SingleSheetImporter {
    public final static int A = 0, B = 1, C = 2, D = 3, E = 4, F = 5, G = 6, H = 7, I = 8, J = 9, K = 10, L = 11, M = 12,
            N = 13, O = 14, P = 15, Q = 16, R = 17, S = 18, T = 19, U = 20, V = 21, W = 22, X = 23, Y = 24, Z = 25,
            AA = 26, AB = 27, AC = 28, AD = 29, AE = 30, AF = 31, AG = 32, AH = 33, AI = 34, AJ = 35, AK = 36, AL = 37, AM = 38, AN = 39, AO = 40,
            AQ = 42, AS = 44, AX = 49;

    protected ImportInputTable inputTable;
    protected OrderedMap<ImportField, Pair<Integer, Integer>> fieldPosition = new OrderedMap<ImportField, Pair<Integer, Integer>>();
    protected int currentRow;

    public SingleSheetImporter(ImportInputTable inputTable, Object... fields) {
        this.inputTable = inputTable;
        int column = 0;
        for (Object field : fields) {
            if (field instanceof Integer) {
                column = (Integer) field;
            } else {
                if (field instanceof ImportField[]) {
                    ImportField[] subFields = (ImportField[]) field;
                    for (int j = 0; j < subFields.length; j++) {
                        fieldPosition.put(subFields[j], new Pair<Integer, Integer>(column, j));
                    }
                } else if (field instanceof ImportField) {
                    fieldPosition.put((ImportField) field, new Pair<Integer, Integer>(column, 0));
                }
                ++column;
            }
        }
    }

    protected abstract boolean isCorrectRow(int rowNum);

    protected String transformValue(int row, int column, int part, String value) {
        return value.trim();
    }

    protected Object getResultObject(ImportField field, String value) throws lsfusion.server.data.type.ParseException {
        if (!value.trim().equals("")) {
            return field.getFieldClass().parseString(value);
        } else {
            return null;
        }
    }

    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        return inputTable.getCellString(field, row, column);
    }

    protected String getCellString(int row, int column) throws ParseException {
        return inputTable.getCellString(row, column);
    }


    public ImportTable getTable() throws ParseException, lsfusion.server.data.type.ParseException {
        List<List<Object>> data = new ArrayList<List<Object>>();
        currentRow = -1;

        for (int i = 0; i < inputTable.rowsCnt(); ++i) {
            if (isCorrectRow(i)) {
                ++currentRow;
                List<Object> row = new ArrayList<Object>();
                for (Map.Entry<ImportField, Pair<Integer, Integer>> entry : fieldPosition.entrySet()) {
                    ImportField field = entry.getKey();
                    String cellValue = getCellString(field, i, entry.getValue().first);
                    String transformedValue = transformValue(i, entry.getValue().first, entry.getValue().second, cellValue);
                    row.add(getResultObject(field, transformedValue));
                }
                data.add(row);
            }
        }

        return new ImportTable(fieldPosition.keyList(), data);
    }

}
