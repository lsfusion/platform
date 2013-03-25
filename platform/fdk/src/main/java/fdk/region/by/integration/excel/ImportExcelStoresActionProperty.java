package fdk.region.by.integration.excel;

import fdk.integration.*;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportExcelStoresActionProperty extends ImportExcelActionProperty {

    public ImportExcelStoresActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.getDefinedInstance(false, false, "Файлы таблиц", "xls");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                List<byte[]> fileList = valueClass.getFiles(objectValue.getValue());

                for (byte[] file : fileList) {

                    ImportData importData = new ImportData();

                    importData.setStoresList(importStores(file));

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

    protected static List<LegalEntity> importStores(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<LegalEntity> data = new ArrayList<LegalEntity>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String storeID = parseString(sheet.getCell(0, i).getContents());
            String nameStore = parseString(sheet.getCell(1, i).getContents());
            String address = parseString(sheet.getCell(2, i).getContents());
            String legalEntityID = parseString(sheet.getCell(3, i).getContents());

            data.add(new Store(storeID, nameStore, address, legalEntityID, null, null));
        }

        return data;
    }
}