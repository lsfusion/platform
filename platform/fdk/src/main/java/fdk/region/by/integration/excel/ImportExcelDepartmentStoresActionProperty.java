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

public class ImportExcelDepartmentStoresActionProperty extends ImportExcelActionProperty {

    public ImportExcelDepartmentStoresActionProperty(ScriptingLogicsModule LM) {
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

                    importData.setDepartmentStoresList(importDepartmentStores(file));

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

    protected static List<DepartmentStore> importDepartmentStores(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<DepartmentStore> data = new ArrayList<DepartmentStore>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String departmentStoreID = parseString(sheet.getCell(0, i).getContents());
            String nameDepartmentStore = parseString(sheet.getCell(1, i).getContents());
            String storeID = parseString(sheet.getCell(2, i).getContents());

            data.add(new DepartmentStore(departmentStoreID, nameDepartmentStore, storeID));
        }

        return data;
    }
}