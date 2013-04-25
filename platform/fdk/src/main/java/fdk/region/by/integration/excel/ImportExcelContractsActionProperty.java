package fdk.region.by.integration.excel;

import fdk.integration.Contract;
import fdk.integration.ImportActionProperty;
import fdk.integration.ImportData;
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

public class ImportExcelContractsActionProperty extends ImportExcelActionProperty {

    public ImportExcelContractsActionProperty(ScriptingLogicsModule LM) {
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

                    importData.setContractsList(importContracts(file));

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

    protected static List<Contract> importContracts(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<Contract> data = new ArrayList<Contract>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String number = parseString(sheet.getCell(0, i).getContents());
            String supplierID = parseString(sheet.getCell(1, i).getContents());
            String customerID = parseString(sheet.getCell(2, i).getContents());
            String contractID = supplierID + "/" + customerID;
            Date dateFrom = parseDate(sheet.getCell(3, i).getContents());
            Date dateTo = parseDate(sheet.getCell(4, i).getContents());
            String shortNameCurrency = parseString(sheet.getCell(5, i).getContents());
            data.add(new Contract(contractID, supplierID, customerID, number, dateFrom, dateTo, shortNameCurrency));
        }

        return data;
    }
}