package roman;

import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.text.ParseException;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 15:52
 */

public class TallyWeijlInvoiceImporter extends SingleSheetImporter {
    private static final int INVOICENUMBER = C, COLLECTIONNUMBER = E, BOXNUMBER = K, LAST_COLUMN = AF;

    public TallyWeijlInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, M).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if (column <= LAST_COLUMN) {
            String cellValue = super.getCellString(field, row, column);
            if (column == BOXNUMBER) {
                if ("#".equals(cellValue))
                    return super.getCellString(field, row, INVOICENUMBER);
            } else if (column == COLLECTIONNUMBER) {
                int index = cellValue.indexOf('/');
                if (index != -1 && cellValue.length() >= (index + 5))
                    return cellValue.substring(index + 1, index + 5);
            }
            return cellValue;
        } else if (column == LAST_COLUMN + 1) {
            return String.valueOf(currentRow + 1);
        } else {
            return "";
        }

        //if (column <= LAST_COLUMN) {
        //    return super.getCellString(field, row, column);
        //} else
        //if (column == LAST_COLUMN + 1) {
        //    return String.valueOf(currentRow + 1);
        //} else {
        //    return "";
        //}
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case K:
                if (value.startsWith("IWBTA"))
                    return value.substring(5);
                else
                    return value;
            case L:
                switch (part) {
                    case 0:
                        return value.substring(0, Math.min(10, value.length())); // customs code
                    case 1:
                        return value.substring(0, Math.min(6, value.length())); // customs code 6
                }
            case Q:
                int lastBackslashPos = value.lastIndexOf('\\');
                return (lastBackslashPos == -1 ? value : value.substring(0, lastBackslashPos));
            case R:
                int commonIndex = value.indexOf(',');
                if (commonIndex >= 0) {
                    switch (part) {
                        case 0:
                            return value.substring(0, commonIndex).trim(); // original name
                        case 1:
                            int endIndex = value.lastIndexOf(',');
                            if (commonIndex == endIndex) {
                                endIndex = value.length();
                            }
                            return value.substring(commonIndex + 1, endIndex).trim(); // color
                    }
                }
            default:
                return value;
        }
    }
}
