package fdk.region.by.integration.excel;

import fdk.integration.ImportActionProperty;
import fdk.integration.ImportData;
import fdk.integration.LegalEntity;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import lsfusion.server.classes.CustomStaticFormatFileClass;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportExcelLegalEntitiesActionProperty extends ImportExcelActionProperty {

    public ImportExcelLegalEntitiesActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.get(false, false, "Файлы таблиц", "xls");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                for (byte[] file : fileList) {

                    ImportData importData = new ImportData();

                    importData.setLegalEntitiesList(importLegalEntities(file));

                    new ImportActionProperty(LM, importData, context).makeImport();

                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (BiffException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected static List<LegalEntity> importLegalEntities(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<LegalEntity> data = new ArrayList<LegalEntity>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String legalEntityID = parseString(sheet.getCell(0, i).getContents());
            String nameLegalEntity = parseString(sheet.getCell(1, i).getContents());
            String address = parseString(sheet.getCell(2, i).getContents());
            String phone = parseString(sheet.getCell(3, i).getContents());
            String email = parseString(sheet.getCell(4, i).getContents());
            String account = parseString(sheet.getCell(5, i).getContents());
            String bankID = parseString(sheet.getCell(6, i).getContents());
            String country = parseString(sheet.getCell(7, i).getContents());
            Boolean isSupplier = parseBoolean(sheet.getCell(8, i).getContents());
            Boolean isCompany = parseBoolean(sheet.getCell(9, i).getContents());
            Boolean isCustomer = parseBoolean(sheet.getCell(10, i).getContents());
            String unp = parseString(sheet.getCell(11, i).getContents());
            String okpo = parseString(sheet.getCell(12, i).getContents());
            String[] ownership = getAndTrimOwnershipFromName(nameLegalEntity);

            data.add(new LegalEntity(legalEntityID, nameLegalEntity, address, unp, okpo, phone, email,
                    isCompany != null ? (legalEntityID + "ТС") : null, isCompany != null ? ownership[2] : null, account,
                    null, null, bankID, country, isSupplier, isCompany, isCustomer));
        }

        return data;
    }

    private static String[] getAndTrimOwnershipFromName(String name) {
        String ownershipName = "";
        String ownershipShortName = "";
        for (String[] ownership : ownershipsList) {
            if (name.contains(ownership[0] + " ") || name.contains(" " + ownership[0])) {
                ownershipName = ownership[1];
                ownershipShortName = ownership[0];
                name = name.replace(ownership[0], "");
            }
        }
        return new String[]{ownershipShortName, ownershipName, name};
    }

    static String[][] ownershipsList = new String[][]{
            {"ОАОТ", "Открытое акционерное общество торговое"},
            {"ОАО", "Открытое акционерное общество"},
            {"СООО", "Совместное общество с ограниченной ответственностью"},
            {"ООО", "Общество с ограниченной ответственностью"},
            {"ОДО", "Общество с дополнительной ответственностью"},
            {"ЗАО", "Закрытое акционерное общество"},
            {"ЧТУП", "Частное торговое унитарное предприятие"},
            {"ЧУТП", "Частное унитарное торговое предприятие"},
            {"ТЧУП", "Торговое частное унитарное предприятие"},
            {"ЧУП", "Частное унитарное предприятие"},
            {"РУП", "Республиканское унитарное предприятие"},
            {"РДУП", "Республиканское дочернее унитарное предприятие"},
            {"УП", "Унитарное предприятие"},
            {"ИП", "Индивидуальный предприниматель"},
            {"СПК", "Сельскохозяйственный производственный кооператив"},
            {"СП", "Совместное предприятие"}};
}