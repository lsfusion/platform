package roman;

import platform.server.classes.DateClass;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.sql.Date;
import java.text.ParseException;

/**
 * User: DAle
 * Date: 16.06.11
 * Time: 12:56
 */

public class WomenSecretInvoiceImporter extends SingleSheetImporter {
    private final int LAST_COLUMN = WomenSecretInputTable.resultBarcodeColumn;

    public WomenSecretInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, WomenSecretInputTable.resultBarcodeColumn).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
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
            case E:
                switch (part) {
                    case 0:
                        return value.substring(0, Math.min(10, value.length())); // customs code
                    case 1:
                        return value.substring(0, Math.min(6, value.length())); // customs code 6
                }

            case G:
                return value.replace(',', '.');

            case J:
                if (!"".equals(value)) {
                    value = value.replace(',', '.');
                    return String.valueOf(Double.parseDouble(value) / 1000);
                }

//            case WomenSecretInputTable.lastInvoiceColumn + 3 + D:  // D column of PL table
//                return value.substring(value.lastIndexOf(' ') + 1);

            default:
                return value;
        }
    }

}
