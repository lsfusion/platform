package roman;

import jxl.read.biff.BiffException;
import platform.server.integration.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class BestsellerImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    public BestsellerImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL, BL.bestsellerSupplier, "edi txt");
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        return new BestsellerInvoiceEDIInputTable(inFile);
    }

    @Override
    protected SingleSheetImporter createExporter(ImportInputTable inputTable) {
        return new EDIInvoiceImporter(inputTable, countryField, colorCodeField, colorNameField, sizeField,
                unitNetWeightField, unitQuantityField, unitPriceField, invoiceSIDField, sidField, barCodeField,
                boxNumberField, customCodeField, customCode6Field, compositionField, originalNameField, numberSkuField, RRPField);
    }
}
