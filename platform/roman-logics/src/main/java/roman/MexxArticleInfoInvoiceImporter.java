package roman;

import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

/**
 * User: DAle
 * Date: 03.03.11
 * Time: 16:49
 */

public class MexxArticleInfoInvoiceImporter extends SingleSheetImporter {
    private final static int STYLE = 1, COO = 10;

    public MexxArticleInfoInvoiceImporter(ImportInputTable inputTable, Object... fields) {
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return (rowNum > 0 && inputTable.getCellString(0, STYLE).trim().equals("STYLE")
                && inputTable.getCellString(0, COO).trim().equals("COO"));
    }
}
