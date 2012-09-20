package roman;

import jxl.read.biff.BiffException;
import platform.server.integration.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;

public class DieselImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    public DieselImportInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, LM.dieselSupplier);
    }

    @Override
    protected boolean hasBarCode() {
        return false;
    }

    @Override
    protected boolean hasBarCodeKey() {
        return false;
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException, ParseException {
        return new DieselInvoiceInputTable(inFile);
    }


    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new DieselInvoiceImporter(inputTable, new Object[]{invoiceSIDField, dateInvoiceField, numberSkuField, new ImportField[]{themeCodeField, themeNameField}, null,
                null, null, null, null, null, null, null, null, null, null, null, countryField, originalNameField, null, null,
                colorCodeField, null, genderField, null, null, new ImportField[]{customCodeField, customCode6Field},
                compositionField, null, new ImportField[]{unitPriceField, RRPField}, null, null, new ImportField[]{boxNumberField, sidField}, null, colorNameField, unitNetWeightField, null, sizeField, unitQuantityField,
                subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField});
    }

}

