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
 * Date: 01.03.11
 * Time: 17:32
 */

public class MexxImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    public MexxImportInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, LM.mexxSupplier, "dat");
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        return new CSVInputTable(new InputStreamReader(inFile), 1, '|');
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new MexxInvoiceImporter(inputTable, new Object[] {null, invoiceSIDField, null, null, boxNumberField, null, null,
                sidField, colorCodeField, null, sizeField, barCodeField, unitQuantityField, null, null,
                null, sidDestinationDataSupplierBoxField, null, unitNetWeightField, null, numberSkuField, new ImportField[] {
                compositionField, countryField, customCodeField, customCode6Field,
                originalNameField, colorNameField, unitPriceField, RRPField}, dateInvoiceField,  themeCodeField, themeNameField, genderField,
                subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField, brandCodeField, brandNameField});
    }

}
