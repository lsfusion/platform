package roman;

import platform.server.integration.ImportField;
import platform.server.integration.ImportInputTable;
import platform.server.integration.SingleSheetImporter;

public class MexxImportOrderActionProperty extends ImportOrderActionProperty {

    public MexxImportOrderActionProperty(RomanLogicsModule LM) {
        super(LM, LM.mexxSupplier);
    }

    @Override
    protected SingleSheetImporter createImporter(ImportInputTable inputTable) {
        return new MexxOrderImporter(inputTable, new Object[]{null, null, null, null, null, null, null, null,
                null, new ImportField[]{themeCodeField, themeNameField}, null, null, null, null,
                dateOrderField, null, null, orderSIDField, null, sidField, originalNameField, compositionField,
                null, genderField, null, null, null, null, null, null, colorCodeField, colorNameField, sizeField,
                null, unitQuantityField, unitPriceField, null, RRPField, null, null, barCodeField, countryField,
                new ImportField[] {customCodeField, customCode6Field},
                numberSkuField, new ImportField[]{dateFromOrderField, dateToOrderField, dateFromOrderArticleField, dateToOrderArticleField}, unitNetWeightField,
                subCategoryCodeField, subCategoryNameField, collectionCodeField, collectionNameField});
    }
}
