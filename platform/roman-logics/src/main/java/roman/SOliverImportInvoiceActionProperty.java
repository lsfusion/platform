package roman;

import jxl.read.biff.BiffException;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.DataClass;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;
import platform.server.logics.property.ExecutionContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static roman.InvoicePricatMergeInputTable.ResultField;

public class SOliverImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    private final RomanBusinessLogics BL;

    public SOliverImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL.RomanLM, BL.RomanLM.sOliverSupplier);
        this.BL = BL;
    }

    @Override
    protected boolean isSimpleInvoice() {
        return true;
    };

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        SOliverInvoiceEDIInputTable invoiceTable = new SOliverInvoiceEDIInputTable(inFile);
        return new InvoicePricatMergeInputTable(BL, invoiceTable, ResultField.BARCODE, ResultField.QUANTITY, ResultField.NUMBERSKU,
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
        return CustomStaticFormatFileClass.getDefinedInstance(true, "Файл данных (*.edi, *.txt)", "edi txt *.*");
    }
}

