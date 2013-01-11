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
                "/scripts/by/ware/Ware.lsf",
                "/scripts/by/ware/WareInvoice.lsf",
                "/scripts/by/ware/WarePurchaseInvoice.lsf",
                "/scripts/by/ware/WarePurchaseReturnInvoice.lsf",
                "/scripts/by/ware/WareSaleInvoice.lsf",
                "/scripts/by/ware/WareSaleReturnInvoice.lsf",
                "/scripts/masterdata/LegalEntity.lsf",
                "/scripts/hr/Employee.lsf",
                "/scripts/stock/store/Store.lsf",
                "/scripts/stock/StockDocument.lsf",
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
                "/scripts/retail/CashOperation.lsf",
                "/scripts/pricelist/PriceInterval.lsf",
                "/scripts/retail/POS.lsf",
                "/scripts/manufacturing/BOM.lsf",
                "/scripts/ContractLedger.lsf",
                "/scripts/stock/writeoff/WriteOff.lsf",
                "/scripts/sale/order/SaleOrder.lsf",
                "/scripts/sale/order/SaleBlanketOrder.lsf",
                "/scripts/sale/invoice/SaleInvoice.lsf",
                "/scripts/sale/shipment/SaleShipment.lsf",
                "/scripts/sale/creditnote/SaleCreditNote.lsf",
                "/scripts/sale/order/SaleReturnOrder.lsf",
                "/scripts/sale/invoice/SaleReturnInvoice.lsf",
                "/scripts/sale/shipment/SaleReturnShipment.lsf",
                "/scripts/sale/creditnote/SaleReturnCreditNote.lsf",
                "/scripts/retail/pricing/PricingSale.lsf",
                "/scripts/sale/Sale.lsf",
                "/scripts/StockReserve.lsf",
                "/scripts/purchase/order/PurchaseOrder.lsf",
                "/scripts/purchase/invoice/PurchaseInvoice.lsf",
                "/scripts/purchase/shipment/PurchaseShipment.lsf",
                "/scripts/purchase/creditnote/PurchaseCreditNote.lsf",
                "/scripts/retail/pricing/PricingPurchase.lsf",
                "/scripts/purchase/order/PurchaseReturnOrder.lsf",
                "/scripts/purchase/invoice/PurchaseReturnInvoice.lsf",
                "/scripts/purchase/shipment/PurchaseReturnShipment.lsf",
                "/scripts/purchase/creditnote/PurchaseReturnCreditNote.lsf",
                "/scripts/purchase/Purchase.lsf",
                "/scripts/retail/pricing/PricingPurchaseReturn.lsf",
                "/scripts/purchase/writeoff/PurchaseWriteOff.lsf",
                "/scripts/purchase/shipment/PurchaseSaleShipment.lsf",
                "/scripts/purchase/invoice/PurchaseSaleInvoice.lsf",
                "/scripts/purchase/order/PurchaseSaleOrder.lsf",
                "/scripts/sale/SalePurchase.lsf",
                "/scripts/stock/inventory/Inventory.lsf",
                "/scripts/masterdata/Currency.lsf",
                "/scripts/stock/adjustment/StockAdjustment.lsf",
                "/scripts/masterdata/Country.lsf",
                "/scripts/masterdata/item/Item.lsf",
                "/scripts/masterdata/item/ItemArticle.lsf",
                "/scripts/masterdata/item/ItemDescription.lsf",
                "/scripts/masterdata/item/ItemFood.lsf",
                "/scripts/masterdata/item/ItemNutrition.lsf",
                "/scripts/masterdata/item/ItemSize.lsf",
                "/scripts/ItemMachinery.lsf",
                "/scripts/ItemScales.lsf",
                "/scripts/by/ware/WareItem.lsf",
                "/scripts/stock/writeoff/WriteOffItem.lsf",
                "/scripts/stock/store/StockDocumentStore.lsf",
                "/scripts/stock/warehouse/StockDocumentWarehouse.lsf",
                "/scripts/ContainerMovement.lsf",
                "/scripts/retail/pricing/PricingPOS.lsf",
                "/scripts/purchase/PurchaseItem.lsf",
                "/scripts/sale/SaleItem.lsf",
                "/scripts/stock/naturalloss/NaturalLoss.lsf",
                "/scripts/Order.lsf",
                "/scripts/BlanketOrder.lsf",
                "/scripts/Invoice.lsf",
                "/scripts/Shipment.lsf",
                "/scripts/ShipmentStore.lsf",
                "/scripts/purchase/writeoff/PurchaseWriteOffStore.lsf",
                "/scripts/retail/pricing/PricingPurchaseWriteOff.lsf",
                "/scripts/retail/pricing/Pricing.lsf",
                "/scripts/retail/pricing/Repricing.lsf",
                "/scripts/CreditNote.lsf",
                "/scripts/pricelist/PriceList.lsf",
                "/scripts/stock/disparity/Disparity.lsf",
                "/scripts/retail/pricing/RepricingDisparity.lsf",
                "/scripts/transport/Transport.lsf",
                "/scripts/transport/Route.lsf",
                "/scripts/transport/Trip.lsf",
                "/scripts/pricelist/Agreement.lsf",
                "/scripts/transport/TripInvoice.lsf",
                "/scripts/sale/statistics/Statistics.lsf",
                "/scripts/sale/statistics/StatisticsItem.lsf",
                "/scripts/stock/naturalloss/NaturalLossItem.lsf",
                "/scripts/sale/statistics/StatisticsStore.lsf",
                "/scripts/stock/warehouse/Warehouse.lsf",
                "/scripts/stock/StockTax.lsf",
                "/scripts/finance/TaxItem.lsf",
                "/scripts/machinery/cashregister/fiscal/FiscalVMK.lsf",
                "/scripts/ShipmentWarehouse.lsf",
                "/scripts/by/ware/WarePriceList.lsf",
                "/scripts/ScheduleOrder.lsf",
                "/scripts/sale/order/SaleScheduleOrder.lsf",
                "/scripts/purchase/order/PurchaseScheduleOrder.lsf",
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

