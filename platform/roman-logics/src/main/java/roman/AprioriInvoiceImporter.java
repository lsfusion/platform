package roman;

import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.text.ParseException;


public class AprioriInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = AC;

    public AprioriInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, AC).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if (column <= LAST_COLUMN) {
            return super.getCellString(field, row, column);
        } else
        if (column == LAST_COLUMN + 1) {
            return String.valueOf(currentRow + 1);
//        } else if (column == AE) { //временная заглушка, почему-то для состава он определяет column=30, а надо 9
//            String value = super.getCellString(field, row, J);
//            return value.substring(0, value.length()-1);
        }
        else
            return "";
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case I :
                 return value.substring(0, value.length()-1);
            case J :
                return value.substring(0, value.length()-1);
            default:
                return value;
        }
    }
}
