package roman;

import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 16:25
 */

public class JennyferImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    public JennyferImportInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, LM.jennyferSupplier);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new JennyferInvoiceImporter(inputTable, new Object[] {invoiceSIDField, boxNumberField, new ImportField[] {barCodeField, sidField},
                colorCodeField, new ImportField[] {colorNameField, sizeField}, new ImportField[]{subCategoryCodeField, subCategoryNameField}, compositionField, themeCodeField, themeNameField, countryField,
                new ImportField[] {customCodeField, customCode6Field}, RRPField, unitPriceField, unitQuantityField, null, null, null,
                unitNetWeightField, numberSkuField, originalNameField, dateInvoiceField, genderField,
                sidDestinationDataSupplierBoxField, collectionCodeField, collectionNameField, brandCodeField, brandNameField});
    }
}

