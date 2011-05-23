package roman;

import jxl.read.biff.BiffException;
import platform.server.integration.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static roman.InvoicePricatMergeInputTable.ResultField;

public class BestsellerImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    public BestsellerImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL, BL.bestsellerSupplier, "edi txt");
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        BestsellerInvoiceEDIInputTable invoiceTable = new BestsellerInvoiceEDIInputTable(inFile);
        return new InvoicePricatMergeInputTable(BL, invoiceTable, ResultField.BARCODE, ResultField.QUANTITY, ResultField.NUMBERSKU,
                ResultField.INVOICE, ResultField.BOXNUMBER, ResultField.COUNTRY, ResultField.ARTICLE, /*ResultField.COLOR, ResultField.COLORCODE,*/
                ResultField.SIZE, ResultField.ORIGINALNAME, ResultField.NETWEIGHT, ResultField.PRICE);
    }

    @Override
    protected SingleSheetImporter createExporter(ImportInputTable inputTable) {
        return new EDIInvoiceImporter(inputTable, barCodeField, sidField, invoiceSIDField, boxNumberField, colorCodeField,
                colorNameField, sizeField, originalNameField, countryField, unitNetWeightField, compositionField, unitPriceField,
                RRPField, unitQuantityField, numberSkuField, customCodeField, customCode6Field);
    }
}
