package roman;

import platform.interop.event.IDaemonTask;
import platform.server.daemons.ScannerDaemonTask;
import platform.server.daemons.WeightDaemonTask;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration", "DuplicateThrows"})
public class RomanBusinessLogics extends BusinessLogics<RomanBusinessLogics> {
    public ScriptingLogicsModule Stock;
    public ScriptingLogicsModule LegalEntity;
    public ScriptingLogicsModule Company;
    public ScriptingLogicsModule Store;
    public ScriptingLogicsModule Numerator;
    public RomanLogicsModule RomanLM;
    public ScriptingLogicsModule RomanRB;
    public ScriptingLogicsModule RetailCRM;
    public ScriptingLogicsModule PriceRound;
    public ScriptingLogicsModule Currency;
    public ScriptingLogicsModule Contract;
    public ScriptingLogicsModule StockContract;
    public ScriptingLogicsModule ContractLedger;
    public ScriptingLogicsModule Supplier;
    public ScriptingLogicsModule Country;
    public ScriptingLogicsModule I18n;
    public ScriptingLogicsModule RomanI18n;
    public ScriptingLogicsModule Utils;

    public RomanBusinessLogics() throws IOException {
        super();
        this.setDialogUndecorated(false);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        Stock = addModuleFromResource("scripts/stock/Stock.lsf");
        LegalEntity = addModuleFromResource("scripts/masterdata/LegalEntity.lsf");
        Company = addModuleFromResource("scripts/Company.lsf");
        Store = addModuleFromResource("scripts/stock/store/Store.lsf");
        Numerator = addModuleFromResource("scripts/utils/Numerator.lsf");
        RetailCRM = addModuleFromResource("scripts/retail/RetailCRM.lsf");
        PriceRound = addModuleFromResource("scripts/pricelist/PriceRound.lsf");
        Currency = addModuleFromResource("scripts/masterdata/Currency.lsf");
        Contract = addModuleFromResource("scripts/masterdata/Contract.lsf");
        StockContract = addModuleFromResource("scripts/stock/StockContract.lsf");
        ContractLedger = addModuleFromResource("scripts/finance/ContractLedger.lsf");
        Supplier = addModuleFromResource("scripts/Supplier.lsf");
        Country = addModuleFromResource("scripts/masterdata/Country.lsf");
        I18n = addModuleFromResource("scripts/utils/I18n.lsf");
        RomanI18n = addModuleFromResource("scripts/RomanI18n.lsf");
        Utils = addModuleFromResource("scripts/utils/Utils.lsf");

        addModulesFromResource(
            "scripts/utils/Hierarchy.lsf",
            "scripts/utils/Historizable.lsf",
            //"scripts/Numerator.lsf",
            "scripts/utils/Document.lsf",
            "scripts/transport/Transport.lsf",
            "scripts/region/by/finance/ConsignmentBy.lsf",
            "scripts/masterdata/Employee.lsf",
            "scripts/finance/Tax.lsf",
            "scripts/region/by/ware/Ware.lsf",
            "scripts/utils/Geo.lsf",
            "scripts/stock/StockDocument.lsf",
            "scripts/RomanDeclaration.lsf",
            "scripts/WholesalePrice.lsf",
            "scripts/RetailPrice.lsf",
            "scripts/stock/Barcode.lsf",
            "scripts/RomanStock.lsf",
            "scripts/RomanDocument.lsf",
            "scripts/CustomsFlow.lsf",
            "scripts/WHfromCS.lsf",
            "scripts/WHfromRF.lsf",
            "scripts/MasterData.lsf",
            "scripts/StorePrice.lsf",
            //"scripts/Contract.lsf",
            "scripts/Transfer.lsf",
            "scripts/RomanSale.lsf",
            "scripts/Return.lsf",
            "scripts/Move.lsf",
            "scripts/DocumentTransfer.lsf",
            "scripts/ListRegister.lsf",
            "scripts/StorePriceTransfer.lsf",
            "scripts/retail/CashOperation.lsf",
            "scripts/retail/POS.lsf",
            "scripts/sale/SaleLedger.lsf",
            "scripts/machinery/Machinery.lsf",
            "scripts/machinery/cashregister/CashRegister.lsf",
            "scripts/InnerOrder.lsf",
            "scripts/machinery/terminal/Terminal.lsf",
            "scripts/Customer.lsf",
            //"scripts/PriceRound.lsf",
            "scripts/WHtoLegalEntity.lsf",
            "scripts/DisparityBatch.lsf",
            "scripts/stock/writeoff/WriteOff.lsf",
            "scripts/RomanWriteOff.lsf",
            "scripts/PriceChange.lsf",
            "scripts/UserPriceChange.lsf",
            "scripts/Surplus.lsf",
            "scripts/stock/inventory/Inventory.lsf",
            "scripts/RomanContractLedger.lsf",
            "scripts/stock/warehouse/Warehouse.lsf",
            "scripts/PriceChangeDiscount.lsf",
            "scripts/CustomCategory.lsf",
            "scripts/Freight.lsf",
            "scripts/RomanInvoice.lsf",
            "scripts/Particulars.lsf",
            "scripts/Defect.lsf",
            "scripts/utils/DefaultData.lsf",
            "scripts/region/ua/machinery/cashregister/FiscalDatecs.lsf",
            "scripts/RomanTransfer.lsf",
            "scripts/RomanPOS.lsf",
            "scripts/RomanCashOperation.lsf",
            "scripts/ReturnCertification.lsf",
            "scripts/region/by/stock/StockBy.lsf",
            "scripts/region/by/stock/store/StoreBy.lsf",
            "scripts/RomanSales.lsf",
            "scripts/region/by/stock/writeoff/WriteOffBy.lsf",
            "scripts/CustomerBy.lsf",
            "scripts/SupplierBy.lsf",
            "scripts/region/by/stock/StockDocumentBy.lsf",
            "scripts/region/by/masterdata/LegalEntityBy.lsf",
            "scripts/utils/Backup.lsf",
            "scripts/pricelist/Agreement.lsf",
            "scripts/pricelist/PriceList.lsf",
            "scripts/machinery/label/Label.lsf",
            "scripts/ContractCompany.lsf",
            //"scripts/utils/Integration.lsf",
            "scripts/stock/StockTax.lsf",
            "scripts/finance/Finance.lsf",
            "scripts/retail/Retail.lsf",
            "scripts/sale/Sale.lsf",
            "scripts/purchase/Purchase.lsf",
            "scripts/retail/ZReport.lsf",
            "scripts/finance/PaymentLedger.lsf",
            "scripts/finance/Bank.lsf",
            "scripts/masterdata/item/Item.lsf",
            "scripts/machinery/scales/Scales.lsf",
            "scripts/machinery/pricechecker/PriceChecker.lsf",
            "scripts/machinery/Equipment.lsf",
            "scripts/masterdata/MasterData.lsf"
        );

        RomanLM = addModule(new RomanLogicsModule(LM, this));

        RomanRB = addModuleFromResource("scripts/RomanRB.lsf");
    }

    @Override
    public ArrayList<IDaemonTask> getDaemonTasks(int compId) {
        ArrayList<IDaemonTask> daemons = super.getDaemonTasks(compId);

        Integer scalesComPort, scalesSpeed, scannerComPort;
        Boolean scannerSingleRead;
        try {
            DataSession session = getDbManager().createSession();
            scalesComPort = (Integer) RomanLM.scalesComPort.read(session, new DataObject(compId, authenticationLM.computer));
            scalesSpeed = (Integer) RomanLM.scalesSpeed.read(session, new DataObject(compId, authenticationLM.computer));
            scannerComPort = (Integer) RomanLM.scannerComPort.read(session, new DataObject(compId, authenticationLM.computer));
            scannerSingleRead = (Boolean) RomanLM.scannerSingleRead.read(session, new DataObject(compId, authenticationLM.computer));
            session.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (scalesComPort != null) {
            IDaemonTask task = new WeightDaemonTask(scalesComPort, scalesSpeed, 1000, 0);
            daemons.add(task);
        }
        if (scannerComPort != null) {
            IDaemonTask task = new ScannerDaemonTask(scannerComPort, ((Boolean)true).equals(scannerSingleRead));
            daemons.add(task);
        }
        return daemons;
    }
}
