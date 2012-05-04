package roman;

import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.text.ParseException;

public class TopazInvoiceImporter extends SingleSheetImporter {
    private final int LAST_COLUMN = V;
    private final int BARCODE_COLUMN = TopazInputTable.barcodeColumn - 1;

    public TopazInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, BARCODE_COLUMN).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if (column <= LAST_COLUMN) {
            return super.getCellString(field, row, column);
        } else {
            return "";
        }
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();
        if(row == B)   {
            String[] splitValue = value.split("\\."); 
            value = splitValue[0];
        }
        return value;
    }
}
