package roman;

import platform.base.BaseUtils;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.text.ParseException;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 15:51
 */

public class JennyferInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = Q;

    public JennyferInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, C).trim().matches("^'(\\d{13}|\\d{12}|\\d{8})$");
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
            case B: return BaseUtils.replicate('0', Math.max(0, 8 - value.length())) + value;
            case C:
                switch (part) {
                    case 0 : return value.substring(1); // barcode
                    case 1 : return value.substring(1, 7); // article
                }
            case D: if (value.length() == 1) return '0' + value;
            case K:
                switch (part) {
                    case 0: return value.substring(0, Math.min(10, value.length())); // customs code
                    case 1: return value.substring(0, Math.min(6, value.length())); // customs code 6
                }
            case E:
                switch (part) {
                    case 0: return value.substring(value.indexOf(' ') + 1, value.lastIndexOf(' ')).trim(); // color
                    case 1: return value.substring(value.lastIndexOf(' ') + 1); // size
                }
            default: return value;
        }
    }
}
