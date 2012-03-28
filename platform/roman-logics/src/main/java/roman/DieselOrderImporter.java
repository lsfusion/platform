package roman;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.server.classes.DateClass;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.ImportTable;
import platform.server.integration.SingleSheetImporter;

import java.sql.Date;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DieselOrderImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = O;

    public DieselOrderImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, E).trim().matches("^(\\d{8}|\\d{10}|\\d{13})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if (column <= LAST_COLUMN)
            return super.getCellString(field, row, column);
        else if (column == LAST_COLUMN + 1) {
            return String.valueOf(currentRow + 1);
        } else
            return "";
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        if (!"".equals(value)) {
            switch (column) {
                case C:
                    return value.substring(0, value.length() - 2);
                case D:
                case L:
                case M:
                    Date sDate = new Date(Integer.parseInt(value.substring(6, 10)) - 1900, Integer.parseInt(value.substring(3, 5)) - 1, Integer.parseInt(value.substring(0, 2)));
                    return DateClass.format(sDate);
                case E:
                    switch (part) {
                        case 0:
                            return value.substring(0, Math.min(10, value.length())); // customs code
                        case 1:
                            return value.substring(0, Math.min(6, value.length())); // customs code 6
                    }
                default:
                    return value;
            }
        } else return value;
    }
}
