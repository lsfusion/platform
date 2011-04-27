package roman;

import jxl.read.biff.BiffException;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static roman.InvoicePricatMergeInputTable.ResultField;

public class SOliverImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    public SOliverImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL, BL.sOliverSupplier);
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        SOliverInvoiceEDIInputTable invoiceTable = new SOliverInvoiceEDIInputTable(inFile);
        return new InvoicePricatMergeInputTable(BL, invoiceTable, ResultField.BARCODE, ResultField.QUANTITY, ResultField.NUMBERSKU,
                ResultField.INVOICE, ResultField.BOXNUMBER);
    }

    @Override
    protected SingleSheetImporter createExporter(ImportInputTable inputTable) {

        return new EDIInvoiceImporter(inputTable, barCodeField, sidField, invoiceSIDField, boxNumberField, colorCodeField,
                colorNameField, sizeField, originalNameField, countryField, unitNetWeightField, compositionField, unitPriceField,
                RRPField, unitQuantityField, numberSkuField, customCodeField, customCode6Field);
    }

    @Override
    public DataClass getValueClass() {
        return FileActionClass.getDefinedInstance(false, "Файл данных (*.edi, *.txt)", "edi txt");
    }
}

