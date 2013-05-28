package roman;

import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.text.ParseException;

/**
 * User: DAle
 * Date: 01.03.11
 * Time: 17:35
 */

public class MexxInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = 19;

    private final static int SHIP_PCS = 12, WEIGHT = 18, EAN = 11, BOXNUMBER = 4;

    public MexxInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, EAN).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if (column == BOXNUMBER)
            return super.getCellString(field, row, column).substring(5, 18);
        else if (column == WEIGHT) {
            double weight = Double.parseDouble(super.getCellString(field, row, WEIGHT));
            double cnt = Double.parseDouble(super.getCellString(field, row, SHIP_PCS));
            return String.valueOf(weight / cnt);
        } else if (column <= LAST_COLUMN) {
            return super.getCellString(field, row, column);
        } else if (column == LAST_COLUMN + 1) {
            return String.valueOf(currentRow + 1);
        } else {
            return "";
        }
    }
}
