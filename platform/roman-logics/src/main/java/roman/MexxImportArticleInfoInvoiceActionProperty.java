package roman;

import platform.interop.action.MessageClientAction;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: DAle
 * Date: 03.03.11
 * Time: 16:28
 */

public class MexxImportArticleInfoInvoiceActionProperty extends BaseImportActionProperty {
    public MexxImportArticleInfoInvoiceActionProperty(RomanLogicsModule LM) {
        super(LM, "Импортировать данные артикулов", LM.mexxSupplier, "dat");
    }

    @Override
    protected void executeRead(ExecutionContext<ClassPropertyInterface> context, Object userValue) throws SQLException {
        ImportField sidField = new ImportField(RomanLM.sidArticle);
        ImportField countryField = new ImportField(RomanLM.name);
        ImportField compositionField = new ImportField(RomanLM.mainCompositionOriginArticle);
        ImportField originalNameField = new ImportField(RomanLM.originalNameArticle);
        //ImportField seasonField = new ImportField(LM.sidSeasonSupplier);
        ImportField themeField = new ImportField(RomanLM.sidThemeSupplier);

        DataObject supplier = context.getKeyValue(supplierInterface);

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> articleKey = new ImportKey(RomanLM.articleComposite, RomanLM.articleSIDSupplier.getMapping(sidField, supplier));
        properties.add(new ImportProperty(sidField, RomanLM.sidArticle.getMapping(articleKey)));

        ImportKey<?> countryKey = new ImportKey(RomanLM.countrySupplier, RomanLM.countryNameSupplier.getMapping(countryField, supplier));
        properties.add(new ImportProperty(countryField, RomanLM.name.getMapping(countryKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierCountrySupplier.getMapping(countryKey)));
        properties.add(new ImportProperty(countryField, RomanLM.countrySupplierOfOriginArticle.getMapping(articleKey), RomanLM.object(RomanLM.countrySupplier).getMapping(countryKey)));

        //ImportKey<?> seasonKey = new ImportKey(LM.seasonSupplier, LM.seasonSIDSupplier.getMapping(seasonField, supplier));
        //properties.add(new ImportProperty(seasonField, LM.sidSeasonSupplier.getMapping(seasonKey)));
        //properties.add(new ImportProperty(supplier, LM.supplierSeasonSupplier.getMapping(seasonKey)));
        //properties.add(new ImportProperty(seasonField, LM.seasonSupplierArticle.getMapping(articleKey), LM.object(LM.seasonSupplier).getMapping(seasonKey)));

        ImportKey<?> themeKey = new ImportKey(RomanLM.themeSupplier, RomanLM.themeSIDSupplier.getMapping(themeField, supplier));
        properties.add(new ImportProperty(themeField, RomanLM.sidThemeSupplier.getMapping(themeKey)));
        properties.add(new ImportProperty(supplier, RomanLM.supplierThemeSupplier.getMapping(themeKey)));
        properties.add(new ImportProperty(themeField, RomanLM.themeSupplierArticle.getMapping(articleKey), RomanLM.object(RomanLM.themeSupplier).getMapping(themeKey)));


        properties.add(new ImportProperty(compositionField, RomanLM.mainCompositionOriginArticle.getMapping(articleKey)));
        properties.add(new ImportProperty(originalNameField, RomanLM.originalNameArticle.getMapping(articleKey)));

        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) userValue);
            // Заголовки тоже читаем, чтобы определить нужный ли файл импортируется
            ImportInputTable inputTable = new CSVInputTable(new InputStreamReader(inFile), 0, '|');

            ImportTable table = new MexxArticleInfoInvoiceImporter(inputTable, null, sidField, originalNameField, themeField,
                    10, countryField, compositionField).getTable();

            ImportKey<?>[] keysArray = {articleKey, countryKey, themeKey};
            new IntegrationService(context.getSession(), table, Arrays.asList(keysArray), properties).synchronize();

            context.delayUserInterfaction(new MessageClientAction("Данные были успешно приняты", "Импорт"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
