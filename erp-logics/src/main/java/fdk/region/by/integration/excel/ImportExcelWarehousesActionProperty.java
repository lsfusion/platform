package fdk.region.by.integration.excel;

import fdk.integration.ImportActionProperty;
import fdk.integration.ImportData;
import fdk.integration.Warehouse;
import fdk.integration.WarehouseGroup;
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

public class ImportExcelWarehousesActionProperty extends ImportExcelActionProperty {

    public ImportExcelWarehousesActionProperty(ScriptingLogicsModule LM) {
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

                    importData.setWarehouseGroupsList(importWarehouseGroups(file));
                    importData.setWarehousesList(importWarehouses(file));

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

    protected static List<Warehouse> importWarehouses(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<Warehouse> data = new ArrayList<Warehouse>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String warehouseID = parseString(sheet.getCell(0, i).getContents());
            String warehouseName = parseString(sheet.getCell(1, i).getContents());
            String warehouseGroupID = parseString(sheet.getCell(2, i).getContents());
            String legalEntityID = parseString(sheet.getCell(4, i).getContents());
            String warehouseAddress = parseString(sheet.getCell(5, i).getContents());

            data.add(new Warehouse(legalEntityID, warehouseGroupID, warehouseID, warehouseName, warehouseAddress));
        }

        return data;
    }

    protected static List<WarehouseGroup> importWarehouseGroups(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<WarehouseGroup> data = new ArrayList<WarehouseGroup>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String warehouseGroupID = parseString(sheet.getCell(2, i).getContents());
            String warehouseGroupName = parseString(sheet.getCell(3, i).getContents());

            data.add(new WarehouseGroup(warehouseGroupID, warehouseGroupName));
        }

        return data;
    }
}