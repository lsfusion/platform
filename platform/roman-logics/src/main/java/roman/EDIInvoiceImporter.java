package roman;

import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

public class EDIInvoiceImporter extends SingleSheetImporter {
    public EDIInvoiceImporter(ImportInputTable inputTable, Object... fields){
        super(inputTable, fields);
    }

    @Override
    protected boolean isCorrectRow(int rowNum) {
        return true;
    }
}
