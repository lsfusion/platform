package roman;

import jxl.read.biff.BiffException;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SOliverImportInvoiceActionProperty extends ImportBoxInvoiceActionProperty {
    public SOliverImportInvoiceActionProperty(RomanBusinessLogics BL) {
        super(BL, BL.sOliverSupplier);
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException {
        return new SOliverInvoiceEDIInputTable(inFile);
    }

    @Override
    protected SingleSheetImporter createExporter(ImportInputTable inputTable) {
        return new SOliverInvoiceImporter(BL, 5, inputTable, barCodeField, unitQuantityField, numberSkuField,
                invoiceSIDField, boxNumberField, sidField, colorCodeField, colorNameField, sizeField, originalNameField,
                countryField, unitNetWeightField, compositionField, unitPriceField, RRPField, customCodeField, customCode6Field);
    }

    @Override
    public DataClass getValueClass() {
        return FileActionClass.getInstance("Файл данных (*.edi, *.txt)", "edi txt");
    }
}

