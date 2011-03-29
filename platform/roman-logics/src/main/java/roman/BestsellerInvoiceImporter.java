package roman;

import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

public class BestsellerInvoiceImporter extends SingleSheetImporter {
    public BestsellerInvoiceImporter(ImportInputTable inputTable, Object... fields){
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return true;
    }
}
