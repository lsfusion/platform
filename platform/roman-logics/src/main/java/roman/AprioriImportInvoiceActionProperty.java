package roman;

import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

public class AprioriImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    public AprioriImportInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, LM.aprioriSupplier);
    }

    @Override
    protected boolean isSimpleInvoice() {
        return true;
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new AprioriInvoiceImporter(inputTable, new Object[] {null, null, null, invoiceSIDField,
                null, dateInvoiceField, sidField, null, colorCodeField,
                compositionField, null, null, null, //состав
                null, null, new ImportField[] {customCodeField, customCode6Field}, null,
                countryField, unitQuantityField, null, null, null, null,
                unitPriceField, null, unitNetWeightField, null,
                sizeField, barCodeField, numberSkuField,

                colorNameField, boxNumberField, originalNameField, RRPField,
                themeCodeField, themeNameField, genderField, sidDestinationDataSupplierBoxField,
                subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField,
                brandCodeField, brandNameField

        });
    }
}
