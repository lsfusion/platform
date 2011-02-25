package roman;

import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 15:52
 */

public class TallyWeijlInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = AF;

    public TallyWeijlInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, M).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(int row, int column) {
        if (column == LAST_COLUMN + 1) {
            return String.valueOf(currentRow + 1);
        } else if (column == LAST_COLUMN + 2) {
            String customCode = super.getCellString(row, K);
            return customCode.substring(0, Math.min(6, customCode.length()));
        }
        return super.getCellString(row, column);
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case L: return value.substring(0, Math.min(10, value.length())); // customs code
            case X: case AD: return value.replace(',', '.');
            case Q:
                int lastBackslashPos = value.lastIndexOf('\\');
                return (lastBackslashPos == -1 ? value : value.substring(0, lastBackslashPos));
            case R:
                switch (part) {
                    case 0: return value.substring(0, value.indexOf(',')).trim(); // original name
                    case 1: return value.substring(value.indexOf(',') + 1, value.lastIndexOf(',')).trim(); // color
                }
            default: return value;
        }
    }
}
