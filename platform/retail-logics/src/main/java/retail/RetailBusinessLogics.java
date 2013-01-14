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
    ScriptingLogicsModule importLM;
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
                "/scripts/stock/Barcode.lsf",
                "/scripts/finance/Tax.lsf",
                "/scripts/region/by/ware/Ware.lsf",
                "/scripts/region/by/ware/WareInvoice.lsf",
                "/scripts/region/by/ware/WarePurchaseInvoice.lsf",
                "/scripts/region/by/ware/WarePurchaseReturnInvoice.lsf",
                "/scripts/region/by/ware/WareSaleInvoice.lsf",
                "/scripts/region/by/ware/WareSaleReturnInvoice.lsf",
                "/scripts/masterdata/LegalEntity.lsf",
                "/scripts/hr/Employee.lsf",
                "/scripts/stock/store/Store.lsf",
                "/scripts/stock/StockDocument.lsf",
                "/scripts/masterdata/Contract.lsf",
                "/scripts/stock/StockContract.lsf",
                "/scripts/sale/SaleLedger.lsf",
                "/scripts/purchase/PurchaseLedger.lsf",
                "/scripts/machinery/Machinery.lsf",
                "/scripts/machinery/cashregister/CashRegister.lsf",
                "/scripts/machinery/scales/Scales.lsf",
                "/scripts/machinery/pricechecker/PriceChecker.lsf",
                "/scripts/machinery/terminal/Terminal.lsf",
                "/scripts/machinery/label/Label.lsf",
                "/scripts/retail/RetailCRM.lsf",
                "/scripts/retail/CashOperation.lsf",
                "/scripts/pricelist/PriceRound.lsf",
                "/scripts/retail/POS.lsf",
                "/scripts/manufacturing/Substitute.lsf",
                "/scripts/manufacturing/BOM.lsf",
                "/scripts/finance/ContractLedger.lsf",
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
                "/scripts/purchase/StockReserve.lsf",
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
                "/scripts/sale/shipment/SalePurchaseShipment.lsf",
                "/scripts/sale/invoice/SalePurchaseInvoice.lsf",
                "/scripts/sale/order/SalePurchaseOrder.lsf",
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
                "/scripts/machinery/MachineryItem.lsf",
                "/scripts/machinery/scales/ScalesItem.lsf",
                "/scripts/region/by/ware/WareItem.lsf",
                "/scripts/stock/writeoff/WriteOffItem.lsf",
                "/scripts/stock/store/StockDocumentStore.lsf",
                "/scripts/stock/warehouse/StockDocumentWarehouse.lsf",
                "/scripts/stock/container/ContainerMovement.lsf",
                "/scripts/retail/pricing/PricingPOS.lsf",
                "/scripts/purchase/PurchaseItem.lsf",
                "/scripts/sale/SaleItem.lsf",
                "/scripts/stock/naturalloss/NaturalLoss.lsf",
                "/scripts/stock/order/Order.lsf",
                "/scripts/stock/order/BlanketOrder.lsf",
                "/scripts/finance/invoice/Invoice.lsf",
                "/scripts/stock/shipment/Shipment.lsf",
                "/scripts/stock/store/ShipmentStore.lsf",
                "/scripts/purchase/writeoff/PurchaseWriteOffStore.lsf",
                "/scripts/retail/pricing/PricingPurchaseWriteOff.lsf",
                "/scripts/retail/pricing/Pricing.lsf",
                "/scripts/retail/repricing/Repricing.lsf",
                "/scripts/finance/creditnote/CreditNote.lsf",
                "/scripts/pricelist/PriceList.lsf",
                "/scripts/stock/disparity/Disparity.lsf",
                "/scripts/retail/repricing/RepricingDisparity.lsf",
                "/scripts/transport/Transport.lsf",
                "/scripts/transport/Route.lsf",
                "/scripts/transport/Trip.lsf",
                "/scripts/pricelist/Agreement.lsf",
                "/scripts/transport/TripInvoice.lsf",
                "/scripts/sale/statistics/SaleStatistics.lsf",
                "/scripts/sale/statistics/SaleStatisticsItem.lsf",
                "/scripts/stock/naturalloss/NaturalLossItem.lsf",
                "/scripts/sale/statistics/SaleStatisticsStore.lsf",
                "/scripts/stock/warehouse/Warehouse.lsf",
                "/scripts/stock/StockTax.lsf",
                "/scripts/finance/TaxItem.lsf",
                "/scripts/region/by/machinery/cashregister/FiscalVMK.lsf",
                "/scripts/stock/warehouse/ShipmentWarehouse.lsf",
                "/scripts/region/by/ware/WarePriceList.lsf",
                "/scripts/stock/order/ScheduleOrder.lsf",
                "/scripts/sale/order/SaleScheduleOrder.lsf",
                "/scripts/purchase/order/PurchaseScheduleOrder.lsf",
                "/scripts/region/by/retail/CashOperationBy.lsf",
                "/scripts/region/by/transport/TripInvoiceConsignmentBy.lsf",
                "/scripts/region/by/stock/StockBy.lsf",
                "/scripts/region/by/stock/store/StoreBy.lsf",
                "/scripts/region/by/stock/writeoff/WriteOffBy.lsf",
                "/scripts/region/by/stock/container/ContainerMovementBy.lsf",
                "/scripts/region/by/stock/StockDocumentBy.lsf",
                "/scripts/region/by/masterdata/LegalEntityBy.lsf",
                "/scripts/region/by/stock/naturalloss/NaturalLossBy.lsf",
                "/scripts/region/by/finance/ConsignmentBy.lsf",
                "/scripts/region/by/stock/order/OrderBy.lsf",
                "/scripts/region/by/stock/inventory/InventoryBy.lsf",
                "/scripts/region/by/sale/order/SaleOrderBy.lsf",
                "/scripts/region/by/finance/InvoiceConsignmentBy.lsf",
                "/scripts/region/by/sale/invoice/SaleInvoiceConsignmentBy.lsf",
                "/scripts/region/by/purchase/invoice/PurchaseReturnInvoiceConsignmentBy.lsf",
                "/scripts/region/by/purchase/writeoff/PurchaseWriteOffBy.lsf",
                "/scripts/region/by/transport/TripBy.lsf",
                "/scripts/region/by/transport/TripInvoiceBy.lsf",
                "/scripts/stock/store/StoreItem.lsf",
                "/scripts/machinery/label/LabelItem.lsf",
                "/scripts/Certificate/Certificate.lsf",
                "/scripts/Certificate/PurchaseCertificate.lsf",
                "/scripts/Certificate/SaleCertificate.lsf",
                "/scripts/Certificate/Declaration.lsf",
                "/scripts/Certificate/PurchaseDeclaration.lsf",
                "/scripts/Certificate/SaleDeclaration.lsf",
                "/scripts/Certificate/Compliance.lsf",
                "/scripts/Certificate/PurchaseCompliance.lsf",
                "/scripts/Certificate/SaleCompliance.lsf",
                "/scripts/Certificate/Sanitation.lsf",
                "/scripts/Certificate/PurchaseSanitation.lsf",
                "/scripts/Certificate/SaleSanitation.lsf"
                
        );
        retailLM = addModuleFromResource("/scripts/retail.lsf");

        importLM = addModuleFromResource("/scripts/import.lsf");

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

