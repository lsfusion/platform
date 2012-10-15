package roman;

import jxl.read.biff.BiffException;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * User: DAle
 * Date: 25.02.11
 * Time: 16:25
 */

public class TeddyImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    public TeddyImportInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, LM.teddySupplier, "zip");
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        return new TeddyInputTable(inFile);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new TeddyInvoiceImporter(inputTable, new Object[]{new ImportField[]{invoiceSIDField, boxNumberField},
                dateInvoiceField, barCodeField, sidField, brandCodeField,
                new ImportField[]{originalNameField, subCategoryCodeField, subCategoryNameField},
                genderField, colorCodeField, colorNameField, sizeField, unitQuantityField, unitPriceField, RRPField,
                compositionField, unitNetWeightField, new ImportField[]{customCodeField, customCode6Field}, countryField,
                numberSkuField, new ImportField[]{sidDestinationDataSupplierBoxField, themeCodeField, themeNameField, collectionCodeField, collectionNameField,
                brandNameField}});
    }
}
