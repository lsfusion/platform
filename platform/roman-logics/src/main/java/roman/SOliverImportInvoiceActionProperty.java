package roman;

import jxl.read.biff.BiffException;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.DataClass;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static roman.InvoicePricatMergeInputTable.ResultField;

public class SOliverImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    public SOliverImportInvoiceActionProperty(RomanLogicsModule RomanLM) {
        super(RomanLM, RomanLM.sOliverSupplier);
    }

    @Override
    protected boolean isSimpleInvoice() {
        return true;
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        SOliverInvoiceEDIInputTable invoiceTable = new SOliverInvoiceEDIInputTable(inFile);
        return new InvoicePricatMergeInputTable(RomanLM, invoiceTable, ResultField.BARCODE, ResultField.QUANTITY, ResultField.NUMBERSKU,
                ResultField.INVOICE, ResultField.BOXNUMBER, ResultField.COUNTRY, ResultField.PRICE, ResultField.DATE);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {

        return new EDIInvoiceImporter(inputTable, barCodeField, sidField, invoiceSIDField, boxNumberField, colorCodeField,
                colorNameField, sizeField, originalNameField, countryField, unitNetWeightField, compositionField, unitPriceField, dateInvoiceField,
                RRPField, unitQuantityField, numberSkuField, customCodeField, customCode6Field, genderField, themeCodeField, themeNameField,
                subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField, brandCodeField, brandNameField);
    }

    @Override
    protected DataClass getReadType() {
        return CustomStaticFormatFileClass.get(true, false, "Файл данных (*.edi, *.txt)", "edi txt *.*");
    }
}

