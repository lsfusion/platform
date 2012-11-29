package roman;

import equ.srv.EquipmentServer;
import net.sf.jasperreports.engine.JRException;
import platform.interop.event.IDaemonTask;
import platform.server.auth.SecurityPolicy;
import platform.server.daemons.ScannerDaemonTask;
import platform.server.daemons.WeightDaemonTask;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

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
    public ScriptingLogicsModule PriceInterval;
    public ScriptingLogicsModule Currency;
    public ScriptingLogicsModule Contract;
    public ScriptingLogicsModule ContractLedger;
    public ScriptingLogicsModule Supplier;
    public ScriptingLogicsModule I18n;
    public ScriptingLogicsModule Utils;
    public ScriptingLogicsModule Country;
    EquipmentServer equipmentServer;

    public RomanBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);

        this.setDialogUndecorated(false);
    }

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        Stock = addModuleFromResource("/scripts/Stock.lsf");
        LegalEntity = addModuleFromResource("/scripts/LegalEntity.lsf");
        Company = addModuleFromResource("/scripts/Company.lsf");
        Store = addModuleFromResource("/scripts/Store.lsf");
        Numerator = addModuleFromResource("/scripts/Numerator.lsf");
        RetailCRM = addModuleFromResource("/scripts/RetailCRM.lsf");
        PriceInterval = addModuleFromResource("/scripts/PriceInterval.lsf");
        Currency = addModuleFromResource("/scripts/Currency.lsf");
        Contract = addModuleFromResource("/scripts/Contract.lsf");
        ContractLedger = addModuleFromResource("/scripts/ContractLedger.lsf");
        Supplier = addModuleFromResource("/scripts/Supplier.lsf");
        Utils = addModuleFromResource("/scripts/Utils.lsf");
        I18n = addModuleFromResource("/scripts/I18n.lsf");
        Country = addModuleFromResource("/scripts/Country.lsf");

        addModulesFromResource(
            "/scripts/Hierarchy.lsf",
            "/scripts/Historizable.lsf",
            //"/scripts/Numerator.lsf",
            "/scripts/Document.lsf",
            "/scripts/Consignment.lsf",
            "/scripts/Employee.lsf",
            "/scripts/Tax.lsf",
            "/scripts/Ware.lsf",
            "/scripts/AccountDocument.lsf",
            "/scripts/Declaration.lsf",
            "/scripts/WholesalePrice.lsf",
            "/scripts/RetailPrice.lsf",
            "/scripts/Barcode.lsf",
            "/scripts/RomanStock.lsf",
            "/scripts/RomanDocument.lsf",
            "/scripts/CustomsFlow.lsf",
            "/scripts/WHfromCS.lsf",
            "/scripts/WHfromRF.lsf",
            "/scripts/MasterData.lsf",
            "/scripts/StorePrice.lsf",
            //"/scripts/Contract.lsf",
            "/scripts/Transfer.lsf",
            "/scripts/RomanSale.lsf",
            "/scripts/Return.lsf",
            "/scripts/Move.lsf",
            "/scripts/DocumentTransfer.lsf",
            "/scripts/ListRegister.lsf",
            "/scripts/StorePriceTransfer.lsf",
            "/scripts/CashOperation.lsf",
            "/scripts/POS.lsf",
            "/scripts/Sales.lsf",
            "/scripts/Machinery.lsf",
            "/scripts/CashRegister.lsf",
            "/scripts/InnerOrder.lsf",
            "/scripts/Terminal.lsf",
                "/scripts/Customer.lsf",
//            "/scripts/PriceInterval.lsf",
            "/scripts/WHtoLegalEntity.lsf",
            "/scripts/DisparityBatch.lsf",
            "/scripts/WriteOff.lsf",
            "/scripts/RomanWriteOff.lsf",
            "/scripts/PriceChange.lsf",
            "/scripts/UserPriceChange.lsf",
            "/scripts/Surplus.lsf",
            "/scripts/Inventory.lsf",
            "/scripts/RomanContractLedger.lsf",
            "/scripts/Warehouse.lsf",
            "/scripts/PriceChangeDiscount.lsf",
            "/scripts/CustomCategory.lsf",
            "/scripts/Freight.lsf",
            "/scripts/RomanInvoice.lsf",
            "/scripts/Particulars.lsf",
            "/scripts/Defect.lsf",
            "/scripts/DefaultData.lsf",
            "/scripts/FiscalDatecs.lsf",
            "/scripts/RomanTransfer.lsf",
            "/scripts/RomanPOS.lsf",
            "/scripts/RomanCashOperation.lsf",
            "/scripts/ReturnCertification.lsf",
            "/scripts/ByStock.lsf",
            "/scripts/ByStore.lsf",
            "/scripts/RomanSales.lsf",
            "/scripts/ByWriteOff.lsf",
            "/scripts/CustomerBy.lsf",
            "/scripts/SupplierBy.lsf",
            "/scripts/ByAccountDocument.lsf",
            "/scripts/ByLegalEntity.lsf",
            "/scripts/Backup.lsf",
            "/scripts/Agreement.lsf",
            "/scripts/PriceList.lsf",
            "/scripts/Label.lsf",
            "/scripts/ContractCompany.lsf"
        );
        RomanLM = addModule(new RomanLogicsModule(LM, this));

        RomanRB = addModuleFromResource("/scripts/RomanRB.lsf");

        equipmentServer = new EquipmentServer(RomanRB);
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }


    @Override
    public ArrayList<IDaemonTask> getDaemonTasks(int compId) {
        ArrayList<IDaemonTask> daemons = super.getDaemonTasks(compId);

        Integer scalesComPort, scalesSpeed, scannerComPort;
        Boolean scannerSingleRead;
        try {
            DataSession session = createSession();
            scalesComPort = (Integer) RomanLM.scalesComPort.read(session, new DataObject(compId, LM.computer));
            scalesSpeed = (Integer) RomanLM.scalesSpeed.read(session, new DataObject(compId, LM.computer));
            scannerComPort = (Integer) RomanLM.scannerComPort.read(session, new DataObject(compId, LM.computer));
            scannerSingleRead = (Boolean) RomanLM.scannerSingleRead.read(session, new DataObject(compId, LM.computer));
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

    @Override
    public BusinessLogics getBL() {
        return this;
    }
}
