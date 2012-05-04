package roman;

import platform.base.BaseUtils;
import platform.server.classes.DataClass;
import platform.server.classes.DateClass;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.text.ParseException;
import java.util.Date;

public class MexxOrderImporter extends SingleSheetImporter {
    private static final int EAN = AO, LAST_COLUMN = AQ;

    public MexxOrderImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, EAN).trim().matches("^(\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if (column <= LAST_COLUMN)
            return super.getCellString(field, row, column);
        else if (column == LAST_COLUMN + 1) {
            return String.valueOf(currentRow + 1);
        } else if (column == LAST_COLUMN + 2) {
            if ("".equals(super.getCellString(row, I)) || "".equals(super.getCellString(row, L))) {
                return "";
            } else {
                int year = Integer.valueOf(super.getCellString(row, I));
                int month = BaseUtils.getNumberOfMonthEnglish(super.getCellString(row, L));
                return String.valueOf(BaseUtils.getLastDateInMonth(year, month).getTime());
            }
        }
        return "";
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {
            case AS:
                if(!"".equals(value))
                switch (part) {
                    case 0:
                        java.sql.Date date = new java.sql.Date(Long.valueOf(value));
                        return DateClass.format(new java.sql.Date(date.getYear() + 1900, date.getMonth(), 1));
                    case 1:
                        date = new java.sql.Date(Long.valueOf(value));
                        return DateClass.format(date);
                    case 2:
                        date = new java.sql.Date(Long.valueOf(value));
                        return DateClass.format(new java.sql.Date(date.getYear() + 1900, date.getMonth(), 1));
                    case 3:
                        date = new java.sql.Date(Long.valueOf(value));
                        return DateClass.format(date);
                }
            default:
                return value;
        }
    }
}
