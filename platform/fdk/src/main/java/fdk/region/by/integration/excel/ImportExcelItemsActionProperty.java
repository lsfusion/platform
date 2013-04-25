package fdk.region.by.integration.excel;

import fdk.integration.ImportActionProperty;
import fdk.integration.ImportData;
import fdk.integration.Item;
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
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportExcelItemsActionProperty extends ImportExcelActionProperty {

    public ImportExcelItemsActionProperty(ScriptingLogicsModule LM) {
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

                    importData.setItemsList(importItems(file));

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

    protected static List<Item> importItems(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<Item> data = new ArrayList<Item>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String itemID = parseString(sheet.getCell(0, i).getContents());
            String groupID = parseString(sheet.getCell(1, i).getContents());
            String name = parseString(sheet.getCell(2, i).getContents());
            String uomName = parseString(sheet.getCell(3, i).getContents());
            String uomShortName = parseString(sheet.getCell(4, i).getContents());
            String uomID = parseString(sheet.getCell(5, i).getContents());
            String brandName = parseString(sheet.getCell(6, i).getContents());
            String brandID = parseString(sheet.getCell(7, i).getContents());
            String country = parseString(sheet.getCell(8, i).getContents());
            String barcode = parseString(sheet.getCell(9, i).getContents());
            Date date = parseDate(sheet.getCell(10, i).getContents());
            Boolean isWeight = parseBoolean(sheet.getCell(11, i).getContents());
            Double netWeight = parseDouble(sheet.getCell(12, i).getContents());
            Double grossWeight = parseDouble(sheet.getCell(13, i).getContents());
            String composition = parseString(sheet.getCell(14, i).getContents());
            Double retailVAT = parseDouble(sheet.getCell(15, i).getContents());
            String wareID = parseString(sheet.getCell(16, i).getContents());
            Double priceWare = parseDouble(sheet.getCell(17, i).getContents());
            Double wareVAT = parseDouble(sheet.getCell(18, i).getContents());
            String writeOffRateID = parseString(sheet.getCell(19, i).getContents());
            Double baseMarkup = parseDouble(sheet.getCell(20, i).getContents());
            Double retailMarkup = parseDouble(sheet.getCell(21, i).getContents());
            Double amountPack = parseDouble(sheet.getCell(22, i).getContents());

            data.add(new Item(itemID, groupID, name, uomName, uomShortName, uomID, brandName, brandID, country,
                    barcode, barcode, date, isWeight, netWeight, grossWeight, composition, retailVAT, wareID,
                    priceWare, wareVAT, writeOffRateID, baseMarkup, retailMarkup, itemID, amountPack));
        }

        return data;
    }
}