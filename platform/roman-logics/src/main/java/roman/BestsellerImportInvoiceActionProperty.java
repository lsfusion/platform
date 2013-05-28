package roman;

import jxl.read.biff.BiffException;
import platform.server.integration.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static roman.InvoicePricatMergeInputTable.ResultField;

public class BestsellerImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    public BestsellerImportInvoiceActionProperty(RomanLogicsModule RomanLM) {
        super(RomanLM, RomanLM.bestsellerSupplier, "edi txt");
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        BestsellerInvoiceEDIInputTable invoiceTable = new BestsellerInvoiceEDIInputTable(inFile);
        return new InvoicePricatMergeInputTable(RomanLM, invoiceTable, ResultField.BARCODE, ResultField.QUANTITY, ResultField.NUMBERSKU,
                ResultField.INVOICE, ResultField.BOXNUMBER, ResultField.COUNTRY, ResultField.ARTICLE,
                ResultField.SIZE, ResultField.ORIGINALNAME, ResultField.NETWEIGHT, ResultField.PRICE, ResultField.DATE);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new EDIInvoiceImporter(inputTable, barCodeField, sidField, invoiceSIDField, boxNumberField, colorCodeField,
                colorNameField, sizeField, originalNameField, countryField, unitNetWeightField, compositionField, unitPriceField, dateInvoiceField,
                RRPField, unitQuantityField, numberSkuField, customCodeField, customCode6Field, genderField, brandCodeField, brandNameField,
                themeCodeField, themeNameField, sidDestinationDataSupplierBoxField,
                new ImportField[]{subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField});
    }
}
