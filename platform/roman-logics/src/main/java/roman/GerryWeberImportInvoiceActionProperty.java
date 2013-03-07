package roman;

import jxl.read.biff.BiffException;
import platform.server.integration.CSVInputTable;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static roman.InvoicePricatMergeInputTable.ResultField;

public class GerryWeberImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    public GerryWeberImportInvoiceActionProperty(RomanLogicsModule RomanLM) {
        super(RomanLM, RomanLM.gerryWeberSupplier, "txt");
    }

    @Override
    protected boolean isSimpleInvoice() {
        return true;
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        ImportInputTable invoiceTable = new CSVInputTable(new InputStreamReader(inFile), 0, ';', false, 4, 4, 5, 21, 22, 22, 23, 24, 25, 26, 27, 27, 28, 29, 30);
        return new InvoicePricatMergeInputTable(RomanLM, invoiceTable,
                ResultField.INVOICE, ResultField.BOXNUMBER, ResultField.DATE, ResultField.BARCODE,
                ResultField.NUMBERSKU, ResultField.ARTICLE, ResultField.COLORCODE, ResultField.SIZE,
                ResultField.ORIGINALNAME, ResultField.COLOR, ResultField.CUSTOMCODE, ResultField.CUSTOMCODE6,
                ResultField.COUNTRY, ResultField.QUANTITY, ResultField.PRICE);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new GerryWeberInvoiceImporter(inputTable, barCodeField, sidField, invoiceSIDField, boxNumberField,  colorCodeField,
                colorNameField, sizeField, originalNameField, countryField, unitNetWeightField, compositionField, unitPriceField, dateInvoiceField,
                RRPField, unitQuantityField, numberSkuField, customCodeField, customCode6Field, null, genderField, themeCodeField, themeNameField,
                subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField, brandCodeField, brandNameField);
    }
}