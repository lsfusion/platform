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
                "/scripts/utils/Backup.lsf",
                "/scripts/utils/DefaultData.lsf",
                "/scripts/utils/Document.lsf",
                "/scripts/utils/EvalScript.lsf",
                "/scripts/utils/Geo.lsf",
                "/scripts/utils/Hierarchy.lsf",
                "/scripts/utils/Historizable.lsf",
                "/scripts/utils/Integration.lsf",
                "/scripts/utils/Numerator.lsf",
                "/scripts/utils/Utils.lsf",
                "/scripts/stock/Stock.lsf",
                "/scripts/Barcode.lsf",
                "/scripts/finance/Tax.lsf",
                "/scripts/Ware.lsf",
                "/scripts/WareInvoice.lsf",
                "/scripts/WarePurchaseInvoice.lsf",
                "/scripts/WarePurchaseReturnInvoice.lsf",
                "/scripts/WareSaleInvoice.lsf",
                "/scripts/WareSaleReturnInvoice.lsf",
                "/scripts/masterdata/LegalEntity.lsf",
                "/scripts/hr/Employee.lsf",
                "/scripts/stock/store/Store.lsf",
                "/scripts/StockDocument.lsf",
                "/scripts/Contract.lsf",
                "/scripts/ContractStock.lsf",
                "/scripts/sale/Sales.lsf",
                "/scripts/purchase/Supply.lsf",
                "/scripts/machinery/Machinery.lsf",
                "/scripts/machinery/cashregister/CashRegister.lsf",
                "/scripts/machinery/scales/Scales.lsf",
                "/scripts/machinery/pricechecker/PriceChecker.lsf",
                "/scripts/machinery/terminal/Terminal.lsf",
                "/scripts/machinery/label/Label.lsf",
                "/scripts/retail/RetailCRM.lsf",
                "/scripts/CashOperation.lsf",
                "/scripts/pricelist/PriceInterval.lsf",
                "/scripts/retail/POS.lsf",
                "/scripts/manufacturing/BOM.lsf",
                "/scripts/ContractLedger.lsf",
                "/scripts/stock/writeoff/WriteOff.lsf",
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
                "/scripts/sale/Sale.lsf",
                "/scripts/StockReserve.lsf",
                "/scripts/PurchaseOrder.lsf",
                "/scripts/PurchaseInvoice.lsf",
                "/scripts/PurchaseShipment.lsf",
                "/scripts/PurchaseCreditNote.lsf",
                "/scripts/PurchasePricing.lsf",
                "/scripts/PurchaseReturnOrder.lsf",
                "/scripts/PurchaseReturnInvoice.lsf",
                "/scripts/PurchaseReturnShipment.lsf",
                "/scripts/PurchaseReturnCreditNote.lsf",
                "/scripts/purchase/Purchase.lsf",
                "/scripts/PurchaseReturnPricing.lsf",
                "/scripts/PurchaseWriteOff.lsf",
                "/scripts/PurchaseSaleShipment.lsf",
                "/scripts/PurchaseSaleInvoice.lsf",
                "/scripts/PurchaseSaleOrder.lsf",
                "/scripts/SalePurchase.lsf",
                "/scripts/stock/inventory/Inventory.lsf",
                "/scripts/masterdata/Currency.lsf",
                "/scripts/StockAdjustment.lsf",
                "/scripts/masterdata/Country.lsf",
                "/scripts/Item.lsf",
                "/scripts/ItemArticle.lsf",
                "/scripts/ItemDescription.lsf",
                "/scripts/ItemFood.lsf",
                "/scripts/ItemNutrition.lsf",
                "/scripts/ItemSize.lsf",
                "/scripts/ItemMachinery.lsf",
                "/scripts/ItemScales.lsf",
                "/scripts/WareItem.lsf",
                "/scripts/stock/writeoff/WriteOffItem.lsf",
                "/scripts/StockDocumentStore.lsf",
                "/scripts/StockDocumentWarehouse.lsf",
                "/scripts/ContainerMovement.lsf",
                "/scripts/POSPricing.lsf",
                "/scripts/PurchaseItem.lsf",
                "/scripts/SaleItem.lsf",
                "/scripts/stock/naturalloss/NaturalLoss.lsf",
                "/scripts/Order.lsf",
                "/scripts/BlanketOrder.lsf",
                "/scripts/Invoice.lsf",
                "/scripts/Shipment.lsf",
                "/scripts/ShipmentStore.lsf",
                "/scripts/PurchaseWriteOffStore.lsf",
                "/scripts/PurchasePricingWriteOff.lsf",
                "/scripts/retail/pricing/Pricing.lsf",
                "/scripts/retail/pricing/Repricing.lsf",
                "/scripts/CreditNote.lsf",
                "/scripts/pricelist/PriceList.lsf",
                "/scripts/stock/disparity/Disparity.lsf",
                "/scripts/DisparityRepricing.lsf",
                "/scripts/transport/Transport.lsf",
                "/scripts/transport/Route.lsf",
                "/scripts/transport/Trip.lsf",
                "/scripts/Agreement.lsf",
                "/scripts/transport/TripInvoice.lsf",
                "/scripts/Statistics.lsf",
                "/scripts/StatisticsItem.lsf",
                "/scripts/stock/naturalloss/NaturalLossItem.lsf",
                "/scripts/StatisticsStore.lsf",
                "/scripts/stock/warehouse/Warehouse.lsf",
                "/scripts/StockTax.lsf",
                "/scripts/ItemTax.lsf",
                "/scripts/machinery/cashregister/fiscal/FiscalVMK.lsf",
                "/scripts/ShipmentWarehouse.lsf",
                "/scripts/WarePriceList.lsf",
                "/scripts/ScheduleOrder.lsf",
                "/scripts/SaleScheduleOrder.lsf",
                "/scripts/PurchaseScheduleOrder.lsf",
                "/scripts/by/CashOperationBy.lsf",
                "/scripts/by/TripInvoiceConsignmentBy.lsf",
                "/scripts/by/StockBy.lsf",
                "/scripts/by/StoreBy.lsf",
                "/scripts/by/WriteOffBy.lsf",
                "/scripts/by/ContainerMovementBy.lsf",
                "/scripts/by/StockDocumentBy.lsf",
                "/scripts/by/LegalEntityBy.lsf",
                "/scripts/by/NaturalLossBy.lsf",
                "/scripts/by/ConsignmentBy.lsf",
                "/scripts/by/OrderBy.lsf",
                "/scripts/by/InventoryBy.lsf",
                "/scripts/by/SaleOrderBy.lsf",
                "/scripts/by/InvoiceConsignmentBy.lsf",
                "/scripts/by/SaleInvoiceConsignmentBy.lsf",
                "/scripts/by/PurchaseReturnInvoiceConsignmentBy.lsf",
                "/scripts/by/PurchaseWriteOffBy.lsf",
                "/scripts/by/TripBy.lsf",
                "/scripts/by/TripInvoiceBy.lsf"
        );
        retailLM = addModuleFromResource("/scripts/retail.lsf");

        equipmentServer = new EquipmentServer(retailLM);
        
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.add(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }

    @Override
    public EquipmentServer getEquipmentServer() {
        return equipmentServer;
    }
}

