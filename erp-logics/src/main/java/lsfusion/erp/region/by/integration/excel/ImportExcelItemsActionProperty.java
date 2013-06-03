package lsfusion.erp.region.by.integration.excel;

import lsfusion.erp.integration.ImportActionProperty;
import lsfusion.erp.integration.ImportData;
import lsfusion.erp.integration.Item;
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
import java.math.BigDecimal;
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

            String idItem = parseString(sheet.getCell(0, i).getContents());
            String idGroup = parseString(sheet.getCell(1, i).getContents());
            String name = parseString(sheet.getCell(2, i).getContents());
            String uomName = parseString(sheet.getCell(3, i).getContents());
            String uomShortName = parseString(sheet.getCell(4, i).getContents());
            String idUOM = parseString(sheet.getCell(5, i).getContents());
            String brandName = parseString(sheet.getCell(6, i).getContents());
            String idBrand = parseString(sheet.getCell(7, i).getContents());
            String country = parseString(sheet.getCell(8, i).getContents());
            String barcode = parseString(sheet.getCell(9, i).getContents());
            Date date = parseDate(sheet.getCell(10, i).getContents());
            Boolean isWeight = parseBoolean(sheet.getCell(11, i).getContents());
            BigDecimal netWeight = parseBigDecimal(sheet.getCell(12, i).getContents());
            BigDecimal grossWeight = parseBigDecimal(sheet.getCell(13, i).getContents());
            String composition = parseString(sheet.getCell(14, i).getContents());
            BigDecimal retailVAT = parseBigDecimal(sheet.getCell(15, i).getContents());
            String idWare = parseString(sheet.getCell(16, i).getContents());
            BigDecimal priceWare = parseBigDecimal(sheet.getCell(17, i).getContents());
            BigDecimal wareVAT = parseBigDecimal(sheet.getCell(18, i).getContents());
            String idWriteOffRate = parseString(sheet.getCell(19, i).getContents());
            BigDecimal baseMarkup = parseBigDecimal(sheet.getCell(20, i).getContents());
            BigDecimal retailMarkup = parseBigDecimal(sheet.getCell(21, i).getContents());
            BigDecimal amountPack = parseBigDecimal(sheet.getCell(22, i).getContents());

            data.add(new Item(idItem, idGroup, name, uomName, uomShortName, idUOM, brandName, idBrand, country,
                    barcode, barcode, date, isWeight, netWeight, grossWeight, composition, retailVAT, idWare,
                    priceWare, wareVAT, idWriteOffRate, baseMarkup, retailMarkup, idItem, amountPack, null, null,
                    null, null));
        }

        return data;
    }
}