package roman;

import platform.base.Pair;
import platform.server.classes.DateClass;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.ImportTable;
import platform.server.integration.SingleSheetImporter;

import java.sql.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DieselInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = AF;

    public DieselInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, 2).trim().matches("(\\w{1}|\\w{2}|\\w{3})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if (column <= LAST_COLUMN) {
            return super.getCellString(field, row, column);
        } else if (column == LAST_COLUMN + 1) {
            return String.valueOf(currentRow + 1);
        } else {
            return "";
        }
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case B:
                Date sDate = new Date(Integer.parseInt(value.substring(6, 10)) - 1900, Integer.parseInt(value.substring(3, 5)) - 1, Integer.parseInt(value.substring(0, 2)));
                return DateClass.format(sDate);
            case Z:
                switch (part) {
                    case 0:
                        return value.substring(0, Math.min(10, value.length())); // customs code
                    case 1:
                        return value.substring(0, Math.min(6, value.length())); // customs code 6
                }

            default:
                return value;
        }
    }

    @Override
    public ImportTable getTable() throws ParseException, platform.server.data.type.ParseException {
        List<List<Object>> data = new ArrayList<List<Object>>();
        currentRow = -1;

        for (int i = 0; i < inputTable.rowsCnt(); ++i) {
            if (isCorrectRow(i)) {
                ++currentRow;
                int count = 0;
                List<Object> row = new ArrayList<Object>();
                for (Map.Entry<ImportField, Pair<Integer, Integer>> entry : fieldPosition.entrySet()) {
                    ImportField field = entry.getKey();
                    String cellValue = inputTable.getCellString(field, i, count);
                    count++;
                    String transformedValue = transformValue(i, entry.getValue().first, entry.getValue().second, cellValue);
                    row.add(getResultObject(field, transformedValue));
                }
                row.remove(20);
                row.remove(19);
                row.add(inputTable.getCellString(i, 19));
                row.add(inputTable.getCellString(i, 20));
                data.add(row);
            }
        }

        return new ImportTable(fieldPosition.keyList(), data);
    }
}
