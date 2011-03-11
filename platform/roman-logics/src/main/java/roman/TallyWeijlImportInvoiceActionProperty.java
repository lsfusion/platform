package roman;

import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 16:23
 */

public class TallyWeijlImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    public TallyWeijlImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL, BL.tallyWeijlSupplier);
    }

    @Override
    protected SingleSheetImporter createExporter(ImportInputTable inputTable) {
        return new TallyWeijlInvoiceImporter(inputTable, new Object[] {null, null, invoiceSIDField, null, null, null, null, null,
                compositionField, countryField, boxNumberField, new ImportField[] {customCodeField, customCode6Field},
                barCodeField, null, sizeField, colorCodeField, sidField, new ImportField[] {originalNameField, colorNameField},
                null, null, null, null, unitQuantityField, unitNetWeightField, null, null, null, null, null,
                unitPriceField, null, null, numberSkuField});
    }
}
