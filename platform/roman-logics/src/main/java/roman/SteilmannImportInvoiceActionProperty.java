package roman;

import jxl.read.biff.BiffException;
import platform.server.integration.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;


public class SteilmannImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    private final RomanBusinessLogics BL;

    @Override
    protected boolean hasBarCodeKey() {
        return false;
    }

    public SteilmannImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL.RomanLM, BL.RomanLM.steilmannSupplier, "csv");
        this.BL = BL;
    }



    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        return new CSVInputTable(new InputStreamReader(inFile), 1, ';');
    }


    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {

        return new SteilmannInvoiceImporter(inputTable, new Object[] {null, null, null, invoiceSIDField, dateInvoiceField,
                null, null, null, null, sidField, originalNameField, colorCodeField, compositionField, null, null,
                new ImportField[] {customCodeField, customCode6Field}, null, countryField, sizeField, unitQuantityField,
                null, null, null, null, new ImportField[] {RRPField, unitPriceField}, null, barCodeField, unitNetWeightField,
                boxNumberField, null, null, null, numberSkuField, colorNameField, themeCodeField, themeNameField, genderField,
                sidDestinationDataSupplierBoxField, subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField});




    }


}