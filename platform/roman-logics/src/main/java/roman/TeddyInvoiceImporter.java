package roman;

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

public class TeddyInvoiceImporter extends SingleSheetImporter {
    private static final int BARCODENUMBER = C, LAST_COLUMN = M;

    public TeddyInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, BARCODENUMBER).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
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
            case B:
                if (value.length() >= 10) {
                    Date sDate = new Date(Integer.parseInt(value.substring(6, 10)) - 1900, Integer.parseInt(value.substring(3, 5)) - 1, Integer.parseInt(value.substring(0, 2)));
                    return DateClass.format(sDate);
                }
            case BARCODENUMBER: {
                if (value.length() == 12) {
                    int checkSum = 0;
                    for (int i = 0; i <= 10; i = i + 2) {
                        checkSum += Integer.valueOf(String.valueOf(value.charAt(i)));
                        checkSum += Integer.valueOf(String.valueOf(value.charAt(i + 1))) * 3;
                    }
                    checkSum %= 10;
                    if (checkSum != 0) checkSum = 10 - checkSum;
                    return value.concat(String.valueOf(checkSum));
                }
            }
            default:
                return value;
        }
    }
}
