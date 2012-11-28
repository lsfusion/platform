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
                "/scripts/WareInvoice.lsf",
                "/scripts/WarePurchaseInvoice.lsf",
                "/scripts/WarePurchaseReturnInvoice.lsf",
                "/scripts/WareSaleInvoice.lsf",
                "/scripts/WareSaleReturnInvoice.lsf",
                "/scripts/LegalEntity.lsf",
                "/scripts/Employee.lsf",
                "/scripts/Store.lsf",
                "/scripts/AccountDocument.lsf",
                "/scripts/StorePrice.lsf",
                "/scripts/Contract.lsf",
                "/scripts/ContractStock.lsf",
                "/scripts/Supplier.lsf",
                "/scripts/Sales.lsf",
                "/scripts/Machinery.lsf",
                "/scripts/CashRegister.lsf",
                "/scripts/Scales.lsf",
                "/scripts/PriceChecker.lsf",
                "/scripts/Terminal.lsf",
                "/scripts/Label.lsf",
                "/scripts/Default.lsf",
                "/scripts/RetailCRM.lsf",
                "/scripts/CashOperation.lsf",
                "/scripts/PriceInterval.lsf",
                "/scripts/POS.lsf",
                "/scripts/ContractLedger.lsf",
                "/scripts/WriteOff.lsf",
                "/scripts/Customer.lsf",
                "/scripts/SaleOrder.lsf",
                "/scripts/SaleBlanketOrder.lsf",
                "/scripts/SaleInvoice.lsf",
                "/scripts/SaleShipment.lsf",
                "/scripts/SaleCreditNote.lsf",
                "/scripts/SaleReturnOrder.lsf",
                "/scripts/SaleReturnInvoice.lsf",
                "/scripts/SaleReturnShipment.lsf",
                "/scripts/SaleReturnCreditNote.lsf",
                "/scripts/SalePricing.lsf",
                "/scripts/Sale.lsf",
                "/scripts/PurchaseOrder.lsf",
                "/scripts/PurchaseInvoice.lsf",
                "/scripts/PurchaseShipment.lsf",
                "/scripts/PurchaseCreditNote.lsf",
                "/scripts/PurchasePricing.lsf",
                "/scripts/PurchaseReturnOrder.lsf",
                "/scripts/PurchaseReturnInvoice.lsf",
                "/scripts/PurchaseReturnShipment.lsf",
                "/scripts/PurchaseReturnCreditNote.lsf",
                "/scripts/Purchase.lsf",
                "/scripts/PurchaseReturnPricing.lsf",
                "/scripts/PurchaseWriteOff.lsf",
                "/scripts/PurchaseSaleShipment.lsf",
                "/scripts/PurchaseSaleInvoice.lsf",
                "/scripts/PurchaseSaleOrder.lsf",
                "/scripts/SalePurchase.lsf",
                "/scripts/Inventory.lsf",
                "/scripts/Currency.lsf",
                "/scripts/I18n.lsf",
                "/scripts/Country.lsf",
                "/scripts/DefaultData.lsf",
                "/scripts/Item.lsf",
                "/scripts/ItemArticle.lsf",
                "/scripts/ItemDescription.lsf",
                "/scripts/ItemFood.lsf",
                "/scripts/ItemSize.lsf",
                "/scripts/ItemWare.lsf",
                "/scripts/ItemWriteOff.lsf",
                "/scripts/Order.lsf",
                "/scripts/BlanketOrder.lsf",
                "/scripts/Invoice.lsf",
                "/scripts/Shipment.lsf",
                "/scripts/StoreShipment.lsf",
                "/scripts/StoreWriteOff.lsf",
                "/scripts/PurchasePricingWriteOff.lsf",
                "/scripts/Pricing.lsf",
                "/scripts/Repricing.lsf",
                "/scripts/CreditNote.lsf",
                "/scripts/PriceList.lsf",
                "/scripts/Transport.lsf",
                "/scripts/Route.lsf",
                "/scripts/Trip.lsf",
                "/scripts/InvoiceTrip.lsf",
                "/scripts/by/InvoiceTripConsignmentBy.lsf",
                "/scripts/Agreement.lsf",
                "/scripts/Backup.lsf",
                "/scripts/Warehouse.lsf",
                "/scripts/StockTax.lsf",
                "/scripts/ItemTax.lsf",
                "/scripts/Geo.lsf",
                "/scripts/FiscalVMK.lsf",
                "/scripts/EvalScript.lsf",
                "/scripts/by/StockBy.lsf",
                "/scripts/by/StoreBy.lsf",
                "/scripts/by/WriteOffBy.lsf",
                "/scripts/by/CustomerBy.lsf",
                "/scripts/by/SupplierBy.lsf",
                "/scripts/by/AccountDocumentBy.lsf",
                "/scripts/by/LegalEntityBy.lsf",
                "/scripts/by/ConsignmentBy.lsf",
                "/scripts/by/InvoiceConsignmentBy.lsf",
                "/scripts/by/SaleInvoiceConsignmentBy.lsf",
                "/scripts/by/PurchaseReturnInvoiceConsignmentBy.lsf"
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

