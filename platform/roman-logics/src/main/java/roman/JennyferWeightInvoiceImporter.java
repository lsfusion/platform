package roman;

import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 15:55
 */

public class JennyferWeightInvoiceImporter extends SingleSheetImporter {
    public JennyferWeightInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, C).replaceAll("\\D", "").matches("^(\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case C: return value.replaceAll("\\D", "").substring(0, Math.min(6, value.length()));
            case N: return value.replace(',', '.');
            default: return value;
        }
    }
}

