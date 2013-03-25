package fdk.region.by.integration.excel;

import fdk.integration.Bank;
import fdk.integration.ImportActionProperty;
import fdk.integration.ImportData;
import jxl.read.biff.BiffException;
import platform.server.classes.CustomStaticFormatFileClass;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class ImportExcelAllActionProperty extends ScriptingActionProperty {

    public ImportExcelAllActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        try {

            CustomStaticFormatFileClass valueClass = CustomStaticFormatFileClass.getDefinedInstance(true, true, "Файлы таблиц", "xls");
            ObjectValue objectValue = context.requestUserData(valueClass, null);
            if (objectValue != null) {
                Map<String, byte[]> fileList = valueClass.getNamedFiles(objectValue.getValue());

                for (Map.Entry<String, byte[]> file : fileList.entrySet()) {

                    ImportData importData = new ImportData();
                    if (file.getKey().contains("importItems")) {
                        importData.setItemsList(ImportExcelItemsActionProperty.importItems(file.getValue()));
                    }
                    if (file.getKey().contains("importGroupItems")) {
                        importData.setParentGroupsList(ImportExcelGroupItemsActionProperty.importGroupItems(file.getValue(), true));
                        importData.setItemGroupsList(ImportExcelGroupItemsActionProperty.importGroupItems(file.getValue(), false));
                    }
                    if (file.getKey().contains("importBanks")) {
                        importData.setBanksList(ImportExcelBanksActionProperty.importBanks(file.getValue()));
                    }
                    if (file.getKey().contains("importLegalEntities")) {
                        importData.setLegalEntitiesList(ImportExcelLegalEntitiesActionProperty.importLegalEntities(file.getValue()));
                    }
                    if (file.getKey().contains("importStores")) {
                        importData.setStoresList(ImportExcelStoresActionProperty.importStores(file.getValue()));
                    }
                    if (file.getKey().contains("importDepartmentStores")) {
                        importData.setDepartmentStoresList(ImportExcelDepartmentStoresActionProperty.importDepartmentStores(file.getValue()));
                    }
                    if (file.getKey().contains("importWarehouses")) {
                        importData.setWarehouseGroupsList(ImportExcelWarehousesActionProperty.importWarehouseGroups(file.getValue()));
                        importData.setWarehousesList(ImportExcelWarehousesActionProperty.importWarehouses(file.getValue()));
                    }
                    if (file.getKey().contains("importContracts")) {
                        importData.setContractsList(ImportExcelContractsActionProperty.importContracts(file.getValue()));
                    }
                    if (file.getKey().contains("importUserInvoices")) {
                        importData.setUserInvoicesList(ImportExcelUserInvoicesActionProperty.importUserInvoices(file.getValue()));
                    }

                    new ImportActionProperty(LM, importData, context).makeImport();
                }
            }
        } catch (BiffException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}