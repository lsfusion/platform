package roman;

import platform.base.BaseUtils;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.text.ParseException;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 15:51
 */

public class JennyferInvoiceImporter extends SingleSheetImporter {
    private static final int LAST_COLUMN = Q;

    public JennyferInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return inputTable.getCellString(rowNum, C).trim().matches("^'(\\d{13}|\\d{12}|\\d{8})$");
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
                return BaseUtils.replicate('0', Math.max(0, 8 - value.length())) + value;
            case C:
                switch (part) {
                    case 0:
                        if (value.length() == 14) { // barcode
                            value = value.substring(1, 13);
                            int checkSum = 0;
                            for (int i = 0; i <= 10; i = i + 2) {
                                checkSum += Integer.valueOf(String.valueOf(value.charAt(i)));
                                checkSum += Integer.valueOf(String.valueOf(value.charAt(i + 1))) * 3;
                            }
                            checkSum %= 10;
                            if (checkSum != 0) checkSum = 10 - checkSum;
                            return value.concat(String.valueOf(checkSum));
                        } else
                            return value.substring(1);
                    case 1:
                        String sid = value.substring(1, 7);
                        if(sid.equals("200000")){
                            try {
                            return getCellString(row, 4).split(" ")[0];
                            } catch (ParseException e) {
                                return null;
                            }

                        }
                        else
                            return sid; // article
                }
            case D:
                if (value.length() == 1) return '0' + value;
            case K:
                switch (part) {
                    case 0:
                        return value.substring(0, Math.min(10, value.length())); // customs code
                    case 1:
                        return value.substring(0, Math.min(6, value.length())); // customs code 6
                }
            case E:
                switch (part) {
                    case 0:
                        return value.substring(value.indexOf(' ') + 1, value.lastIndexOf(' ')).trim(); // color
                    case 1:
                        return value.substring(value.lastIndexOf(' ') + 1); // size
                }
            default:
                return value;
        }
    }
}
