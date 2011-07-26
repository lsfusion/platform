package roman;

import platform.base.BaseUtils;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.text.ParseException;

public class GerryWeberPricatCSVImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = 17, CUSTOMNO = 13;

    private final static int A = 0;

    public GerryWeberPricatCSVImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, A).trim().matches("^(0\\d{13}|\\d{13}|\\d{12}|\\d{8})$");
    }

    @Override
    protected String getCellString(ImportField field, int row, int column) throws ParseException {
        if (column == LAST_COLUMN + 1) {
            return "";
        }
        return super.getCellString(field, row, column);
    }

    @Override
    protected String transformValue(int row, int column, int part, String value) {
        value = value.trim();

        switch (column) {

            case CUSTOMNO:
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
