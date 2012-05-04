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
 * Date: 28.02.11
 * Time: 19:30
 */

public class HugoBossInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = 36;

    private final static int CUSTOM_NUMBER = 16, CUSTOM_NUMBER_6 = 17, EAN = 0, ARTICLE = 1, COLORCODE = 5, SIZE = 7, DATE = 12;

    public HugoBossInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, EAN).trim().matches("^(0\\d{13}|\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if ((column >= LAST_COLUMN + 1)) {
            return "";
        }
        return super.getCellString(field, row, column);
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case DATE:
                if (value.length() < 7)
                    return "";
                else {
                    Date sDate = new Date(Integer.parseInt(value.substring(0, 4)) - 1900, Integer.parseInt(value.substring(4, 6)) - 1, Integer.parseInt(value.substring(6, 8)));
                    return DateClass.format(sDate);
                }
            case CUSTOM_NUMBER:
                if (value.length() < 10) {
                    value = value + BaseUtils.replicate('0', 10 - value.length());
                }
                return value.substring(0, 10);

            case CUSTOM_NUMBER_6:
                if (value.length() < 6)
                    return "";
                else
                    return value.substring(0, 6);

            case EAN:
                if (value.length() == 14)
                    return value.substring(1);
                else
                    return value;

            case COLORCODE:
                if (value.isEmpty())
                    return "000";
                else
                    return value;

            case SIZE:
                if (value.isEmpty())
                    return "ONESI";
                else
                    return value;

            case ARTICLE:
                return value.substring(value.length()-8);

            default: return value;
        }
    }
}
