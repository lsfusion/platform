package roman;

import jxl.read.biff.BiffException;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static roman.InvoicePricatMergeInputTable.ResultField;

public class TopazImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    private final RomanBusinessLogics BL;

    public TopazImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL.RomanLM, BL.RomanLM.topazSupplier);
        this.BL = BL;
    }

    @Override
    protected boolean isSimpleInvoice() {
        return true;
    }

    ;

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        TopazInputTable invoiceTable = new TopazInputTable(inFile);
        return new InvoicePricatMergeInputTable(BL, invoiceTable,
                ResultField.ARTICLE, ResultField.BARCODE, ResultField.QUANTITY, ResultField.COMPOSITION,
                ResultField.SIZE, ResultField.COLORCODE, ResultField.COLOR, ResultField.INVOICE, ResultField.BOXNUMBER
        );
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {

        return new TopazInvoiceImporter(inputTable,
                barCodeField, sidField, invoiceSIDField, boxNumberField, colorCodeField, colorNameField,
                sizeField, originalNameField, countryField, unitNetWeightField, compositionField,
                RRPField, dateInvoiceField, unitPriceField, unitQuantityField, numberSkuField, customCodeField, customCode6Field,
                genderField, seasonField, themeCodeField, themeNameField, sidDestinationDataSupplierBoxField
        );

    }

    @Override
    public DataClass getValueClass() {
        return FileActionClass.getDefinedInstance(true, "Файл Excel (*.xls)", "xls *.*");
    }
}

