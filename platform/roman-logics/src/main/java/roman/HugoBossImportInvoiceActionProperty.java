package roman;

import jxl.read.biff.BiffException;
import platform.base.Result;
import platform.server.integration.CSVInputTable;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static roman.InvoicePricatMergeInputTable.ResultField;

/**
 * User: DAle
 * Date: 28.02.11
 * Time: 19:26
 */

public class HugoBossImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {

    private final RomanBusinessLogics BL;

    public HugoBossImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL.RomanLM, BL.RomanLM.hugoBossSupplier, "csv");
        this.BL = BL;
    }

    @Override
    protected boolean isSimpleInvoice() {
        return true;
    };

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        ImportInputTable invoiceTable = new CSVInputTable(new InputStreamReader(inFile), 2, ';', false, 1, 1, 2, 11, 17, 18, 19, 20, 21, 21, 22, 25, 26, 27, 28, 32);
        return new InvoicePricatMergeInputTable(BL, invoiceTable, ResultField.INVOICE, ResultField.BOXNUMBER, ResultField.DATE, ResultField.NUMBERSKU,
                ResultField.ORIGINALNAME, ResultField.ARTICLE, ResultField.COLORCODE, ResultField.COMPOSITION, ResultField.CUSTOMCODE,
                ResultField.CUSTOMCODE6, ResultField.COUNTRY, ResultField.BARCODE, ResultField.SIZE, ResultField.QUANTITY, ResultField.PRICE, ResultField.SEASON);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new HugoBossInvoiceImporter(inputTable, barCodeField, sidField, invoiceSIDField, boxNumberField,  colorCodeField,
                colorNameField, sizeField, originalNameField, countryField, unitNetWeightField, compositionField, unitPriceField, dateInvoiceField,
                RRPField, unitQuantityField, numberSkuField, customCodeField, customCode6Field, genderField, themeCodeField, themeNameField,
                subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField, brandCodeField, brandNameField);
    }
}
