package roman;

import jxl.read.biff.BiffException;
import platform.server.integration.CSVInputTable;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * User: DAle
 * Date: 28.02.11
 * Time: 19:26
 */

public class HugoBossImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    public HugoBossImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL, BL.hugoBossSupplier, "csv");
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        return new CSVInputTable(new InputStreamReader(inFile), 2, ';');
    }

    @Override
    protected SingleSheetImporter createExporter(ImportInputTable inputTable) {
        return new HugoBossInvoiceImporter(inputTable, new Object[] {null, new ImportField[] {invoiceSIDField, boxNumberField},
                11, numberSkuField, null, null, null, unitNetWeightField, null, originalNameField, sidField,
                colorCodeField, compositionField, new ImportField[] {customCodeField, customCode6Field}, countryField, null,
                null, barCodeField, sizeField, unitQuantityField, unitPriceField, 36, new ImportField[] {colorNameField, RRPField}});
    }
}
