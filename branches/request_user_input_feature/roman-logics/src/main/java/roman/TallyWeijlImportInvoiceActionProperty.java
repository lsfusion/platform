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

    public TallyWeijlImportInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, LM.tallyWeijlSupplier);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new TallyWeijlInvoiceImporter(inputTable, new Object[] {null, null, invoiceSIDField,
                dateInvoiceField, null, null, null, null,
                compositionField, countryField, boxNumberField, new ImportField[] {customCodeField, customCode6Field},
                barCodeField, null, sizeField, null, sidField, new ImportField[] {originalNameField, colorNameField},
                null, colorCodeField, seasonField, null, RRPField, null, unitNetWeightField, null, null, null, null, null,
                unitPriceField, unitQuantityField, numberSkuField, themeCodeField, themeNameField, genderField, sidDestinationDataSupplierBoxField});
    }
}
