package roman;

import jxl.read.biff.BiffException;
import platform.server.integration.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static roman.InvoicePricatMergeInputTable.ResultField;

public class BestsellerImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    private RomanBusinessLogics BL;

    public BestsellerImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL.RomanLM, BL.RomanLM.bestsellerSupplier, "edi txt");
        this.BL = BL;
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        BestsellerInvoiceEDIInputTable invoiceTable = new BestsellerInvoiceEDIInputTable(inFile);
        return new InvoicePricatMergeInputTable(BL, invoiceTable, ResultField.BARCODE, ResultField.QUANTITY, ResultField.NUMBERSKU,
                ResultField.INVOICE, ResultField.BOXNUMBER, ResultField.COUNTRY, ResultField.ARTICLE,
                ResultField.SIZE, ResultField.ORIGINALNAME, ResultField.NETWEIGHT, ResultField.PRICE, ResultField.DATE);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new EDIInvoiceImporter(inputTable, barCodeField, sidField, invoiceSIDField, boxNumberField, colorCodeField,
                colorNameField, sizeField, originalNameField, countryField, unitNetWeightField, compositionField, unitPriceField, dateInvoiceField,
                RRPField, unitQuantityField, numberSkuField, customCodeField, customCode6Field, genderField, themeCodeField, themeNameField, sidDestinationDataSupplierBoxField,
                new ImportField[]{subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField});
    }
}
