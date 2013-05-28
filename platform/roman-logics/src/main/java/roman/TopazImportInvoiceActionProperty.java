package roman;

import jxl.read.biff.BiffException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.classes.DataClass;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static roman.InvoicePricatMergeInputTable.ResultField;

public class TopazImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    public TopazImportInvoiceActionProperty(RomanLogicsModule RomanLM) {
        super(RomanLM, RomanLM.topazSupplier);
    }

    @Override
    protected boolean isSimpleInvoice() {
        return true;
    }

    ;

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException, InvalidFormatException {
        TopazInputTable invoiceTable = new TopazInputTable(inFile);
        return new InvoicePricatMergeInputTable(RomanLM, invoiceTable,
                ResultField.ARTICLE, ResultField.BARCODE, ResultField.QUANTITY, ResultField.COMPOSITION,
                ResultField.SIZE, ResultField.COLORCODE, ResultField.COLOR, ResultField.INVOICE, ResultField.BOXNUMBER, ResultField.NUMBERSKU
        );
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {

        return new TopazInvoiceImporter(inputTable,
                barCodeField, sidField, invoiceSIDField, boxNumberField, colorCodeField, colorNameField,
                sizeField, originalNameField, countryField, unitNetWeightField, compositionField,
                unitPriceField, dateInvoiceField, RRPField, unitQuantityField, numberSkuField, customCodeField, customCode6Field,
                genderField, themeCodeField, themeNameField, sidDestinationDataSupplierBoxField,
                subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField,
                brandCodeField, brandNameField
        );

    }

    protected DataClass getReadType() {
        return CustomStaticFormatFileClass.get(true, false, "Файл Excel (*.xls)", "xls *.*");
    }
}

