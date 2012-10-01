package retail;

import equ.srv.EquipmentServer;
import equ.srv.EquipmentServerHolder;
import net.sf.jasperreports.engine.JRException;
import platform.server.auth.SecurityPolicy;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * User: DAle
 * Date: 05.01.12
 * Time: 15:34
 */


public class RetailBusinessLogics extends BusinessLogics<RetailBusinessLogics> implements EquipmentServerHolder {
    ScriptingLogicsModule retailLM;
    EquipmentServer equipmentServer;

    public RetailBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    public ScriptingLogicsModule getLM() {
        return retailLM;
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        addModulesFromResource(
                "/scripts/Utils.lsf",
                "/scripts/Hierarchy.lsf",
                "/scripts/Historizable.lsf",
                "/scripts/Numerator.lsf",
                "/scripts/Stock.lsf",
                "/scripts/Barcode.lsf",
                "/scripts/Document.lsf",
                "/scripts/Tax.lsf",
                "/scripts/Ware.lsf",
                "/scripts/LegalEntity.lsf",
                "/scripts/Employee.lsf",
                "/scripts/Store.lsf",
                "/scripts/ListRegister.lsf",
                "/scripts/Consignment.lsf",
                "/scripts/AccountDocument.lsf",
                "/scripts/StorePrice.lsf",
                "/scripts/Contract.lsf",
                "/scripts/Supplier.lsf",
                "/scripts/Sales.lsf",
                "/scripts/PriceChange.lsf",
                "/scripts/Machinery.lsf",
                "/scripts/CashRegister.lsf",
                "/scripts/Scales.lsf",
                "/scripts/PriceChecker.lsf",
                "/scripts/Terminal.lsf",
                "/scripts/Label.lsf",
                "/scripts/Default.lsf",
                "/scripts/RetailCRM.lsf",
                "/scripts/UserPriceChange.lsf",
                "/scripts/CashOperation.lsf",
                "/scripts/PriceInterval.lsf",
                "/scripts/POS.lsf",
                "/scripts/ContractLedger.lsf",
                "/scripts/WriteOff.lsf",
                "/scripts/Customer.lsf",
                "/scripts/SaleOrder.lsf",
                "/scripts/SaleInvoice.lsf",
                "/scripts/SaleShipment.lsf",
                "/scripts/ReturnOrder.lsf",
                "/scripts/ReturnInvoice.lsf",
                "/scripts/ReturnShipment.lsf",
                "/scripts/Inventory.lsf",
                "/scripts/Currency.lsf",
                "/scripts/I18n.lsf",
                "/scripts/Country.lsf",
                "/scripts/DefaultData.lsf",
                "/scripts/Item.lsf",
                "/scripts/Order.lsf",
                "/scripts/Invoice.lsf",
                "/scripts/Shipment.lsf",
                "/scripts/PriceList.lsf",
                "/scripts/ByStock.lsf",
                "/scripts/ByStore.lsf",
                "/scripts/ByWriteOff.lsf",
                "/scripts/ByCustomer.lsf",
                "/scripts/BySupplier.lsf",
                "/scripts/ByAccountDocument.lsf",
                "/scripts/ByLegalEntity.lsf"
        );
        retailLM = addModuleFromResource("/scripts/retail.lsf");

        equipmentServer = new EquipmentServer(retailLM);
        
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }

    @Override
    public EquipmentServer getEquipmentServer() {
        return equipmentServer;
    }
}

