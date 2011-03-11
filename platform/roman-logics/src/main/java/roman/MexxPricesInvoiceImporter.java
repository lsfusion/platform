package roman;

import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

/**
 * User: DAle
 * Date: 03.03.11
 * Time: 15:36
 */

public class MexxPricesInvoiceImporter extends SingleSheetImporter {
    private final static int EAN_CODE = 12, HTS_CODE = 14;

    public MexxPricesInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, EAN_CODE).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case HTS_CODE:
                switch (part) {
                    case 0: return value.substring(0, Math.min(10, value.length()));
                    case 1: return value.substring(0, Math.min(6, value.length()));
                }
            default: return value;
        }
    }
}
