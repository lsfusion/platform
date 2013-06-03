package lsfusion.erp.region.by.integration.excel;

import lsfusion.erp.integration.Bank;
import lsfusion.erp.integration.ImportActionProperty;
import lsfusion.erp.integration.ImportData;
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

public class ImportExcelBanksActionProperty extends ImportExcelActionProperty {

    public ImportExcelBanksActionProperty(ScriptingLogicsModule LM) {
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

                    importData.setBanksList(importBanks(file));

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

    protected static List<Bank> importBanks(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<Bank> data = new ArrayList<Bank>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String bankID = parseString(sheet.getCell(0, i).getContents());
            String name = parseString(sheet.getCell(1, i).getContents());
            String address = parseString(sheet.getCell(2, i).getContents());
            String department = parseString(sheet.getCell(3, i).getContents());
            String mfo = parseString(sheet.getCell(4, i).getContents());
            String cbu = parseString(sheet.getCell(5, i).getContents());

            data.add(new Bank(bankID, name, address, department, mfo, cbu));
        }

        return data;
    }
}