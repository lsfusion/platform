package roman;

import platform.base.BaseUtils;
import platform.server.classes.DateClass;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.sql.Date;
import java.text.ParseException;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 15:51
 */

public class SteilmannInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = AF;

    public SteilmannInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, AA).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
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
                 Date sDate = new Date(Integer.parseInt(value.substring(4, 8)) - 1900, Integer.parseInt(value.substring(2, 4)) - 1, Integer.parseInt(value.substring(0, 2)));
                 return DateClass.format(sDate);
            case J:
                 return value.substring(0, 6);
            case P:
                switch (part) {
                    case 0: return value.substring(0, Math.min(10, value.length())); // customs code
                    case 1: return value.substring(0, Math.min(6, value.length())); // customs code 6
                }
            default: return value;
        }
    }
}

