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
 * Date: 25.02.11
 * Time: 16:25
 */

public class TeddyImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    public TeddyImportInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, LM.teddySupplier, "dat");
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        return new CSVInputTable(new InputStreamReader(inFile), 0, ';');
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new TeddyInvoiceImporter(inputTable, new Object[]{null, null, null, null, null,
                new ImportField[]{invoiceSIDField, boxNumberField}, dateInvoiceField, null, null, null, null,
                barCodeField, compositionField, null, null, null, null,
                new ImportField[]{originalNameField,subCategoryCodeField, subCategoryNameField},
                sidField, sizeField, colorCodeField, colorNameField, unitQuantityField, unitPriceField, null, null,
                RRPField, null, null, null, null, null, null, null, null, null, numberSkuField,
                new ImportField[]{sidDestinationDataSupplierBoxField, unitNetWeightField, countryField,
                customCodeField, customCode6Field, themeCodeField, themeNameField, genderField,
                collectionCodeField, collectionNameField}});
    }
}
