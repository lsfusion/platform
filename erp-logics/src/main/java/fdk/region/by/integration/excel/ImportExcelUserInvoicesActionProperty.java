package fdk.region.by.integration.excel;

import fdk.integration.ImportActionProperty;
import fdk.integration.ImportData;
import fdk.integration.UserInvoiceDetail;
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

public class ImportExcelUserInvoicesActionProperty extends ImportExcelActionProperty {

    public ImportExcelUserInvoicesActionProperty(ScriptingLogicsModule LM) {
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

                    importData.setUserInvoicesList(importUserInvoices(file));

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

    protected static List<UserInvoiceDetail> importUserInvoices(byte[] file) throws IOException, BiffException, ParseException {

        Workbook Wb = Workbook.getWorkbook(new ByteArrayInputStream(file));
        Sheet sheet = Wb.getSheet(0);

        List<UserInvoiceDetail> data = new ArrayList<UserInvoiceDetail>();

        for (int i = 1; i < sheet.getRows(); i++) {

            String userInvoiceSeries = parseString(sheet.getCell(0, i).getContents());
            String userInvoiceNumber = parseString(sheet.getCell(1, i).getContents());
            Date date = parseDate(sheet.getCell(2, i).getContents());
            String itemID = parseString(sheet.getCell(3, i).getContents());
            BigDecimal quantity = parseBigDecimal(sheet.getCell(4, i).getContents());
            String supplier = parseString(sheet.getCell(5, i).getContents());
            String customerWarehouse = parseString(sheet.getCell(6, i).getContents());
            String supplierWarehouse = parseString(sheet.getCell(7, i).getContents());
            BigDecimal price = parseBigDecimal(sheet.getCell(8, i).getContents());
            BigDecimal chargePrice = parseBigDecimal(sheet.getCell(9, i).getContents());
            BigDecimal retailPrice = parseBigDecimal(sheet.getCell(10, i).getContents());
            BigDecimal retailMarkup = parseBigDecimal(sheet.getCell(11, i).getContents());
            String textCompliance = parseString(sheet.getCell(12, i).getContents());

            String userInvoiceDetailSID = (userInvoiceSeries==null ? "" : userInvoiceSeries) + userInvoiceNumber + itemID;

            data.add(new UserInvoiceDetail(userInvoiceSeries + userInvoiceNumber, userInvoiceSeries, userInvoiceNumber, null, true,
                    userInvoiceDetailSID, date, itemID, false, quantity, supplier, customerWarehouse, supplierWarehouse,
                    price, chargePrice, null, null, null, retailPrice, retailMarkup, textCompliance, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        }

        return data;
    }
}