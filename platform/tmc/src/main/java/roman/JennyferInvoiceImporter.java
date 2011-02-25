package roman;

import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 15:51
 */

public class JennyferInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = P;

    public JennyferInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, C).trim().matches("^'(\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(int row, int column) {
        if (column <= LAST_COLUMN) {
            return super.getCellString(row, column);
        } else if (column == LAST_COLUMN + 1) {
            return String.valueOf(currentRow + 1);
        } else if (column == LAST_COLUMN + 2) {
            String customCode = super.getCellString(row, K);
            return customCode.substring(0, Math.min(6, customCode.length()));
        } else {
            return "";
        }
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case C:
                switch (part) {
                    case 0 : return value.substring(1); // barcode
                    case 1 : return value.substring(1, 7); // article
                }
            case K: return value.substring(0, Math.min(10, value.length())); // customs code
            case N: case O: return value.replace(',', '.');
            case E:
                switch (part) {
                    case 0: return value.substring(value.indexOf(' ') + 1, value.lastIndexOf(' ')).trim(); // color
                    case 1: return value.substring(value.lastIndexOf(' ') + 1); // size
                }
            default: return value;
        }
    }
}
