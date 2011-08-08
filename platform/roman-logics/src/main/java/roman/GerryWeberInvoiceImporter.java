package roman;

import platform.base.BaseUtils;
import platform.server.classes.DateClass;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.sql.Date;
import java.text.ParseException;


public class GerryWeberInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = 17;//33

    private final static int D = 3, DATE=12, CUSTOM_NUMBER = 27;

    public GerryWeberInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, D).trim().matches("^(0\\d{13}|\\d{13}|\\d{12}|\\d{8})$");
    }

    //@Override
    //protected String getCellString(ImportField field, int row, int column) throws ParseException {
    //    if ((column >= LAST_COLUMN + 1)) {
    //        return "";
    //    }
    //    return super.getCellString(field, row, column);
    //}

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
            case DATE:
                if (value.length() < 7)
                    return "";
                else {
                    Date sDate = new Date(Integer.parseInt(value.substring(0, 4)) - 1900, Integer.parseInt(value.substring(4, 6)) - 1, Integer.parseInt(value.substring(6, 8)));
                    return DateClass.format(sDate);
                }
             case 15:
                 return value.substring(0,6);

            case CUSTOM_NUMBER:
                switch (part) {
                    case 0:
                        if (value.length() < 10) {
                            value = value + BaseUtils.replicate('0', 10 - value.length());
                        }
                        return value.substring(0, 10); // customs code
                    case 1: return value.substring(0, 6); // customs code 6
                }

            default: return value;
        }
    }
}
