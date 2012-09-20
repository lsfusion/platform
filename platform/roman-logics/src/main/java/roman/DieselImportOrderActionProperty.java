package roman;

import jxl.read.biff.BiffException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;

public class DieselImportOrderActionProperty extends ImportOrderActionProperty {

    public DieselImportOrderActionProperty(RomanLogicsModule LM) {
        super(LM, LM.dieselSupplier);
    }

    @Override
    protected boolean hasBarCode() {
        return false;
    }

    @Override
    protected ImportInputTable createTable(ByteArrayInputStream inFile) throws BiffException, IOException, ParseException, InvalidFormatException {
        return new DieselOrderInputTable(inFile);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new DieselOrderImporter(inputTable, new Object[]{new ImportField[]{themeCodeField, themeNameField}, genderField, orderSIDField, dateOrderField,
                new ImportField[]{customCodeField, customCode6Field}, compositionField, sidField, originalNameField,
                colorCodeField, countryField, unitPriceField, new ImportField[]{dateFromOrderField, dateFromOrderArticleField},
                new ImportField[]{dateToOrderField, dateToOrderArticleField},
                sizeField, unitQuantityField,

                numberSkuField, colorNameField, RRPField,
                unitNetWeightField, subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField
        });
    }
}
