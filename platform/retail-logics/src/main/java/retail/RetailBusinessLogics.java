package retail;

import net.sf.jasperreports.engine.JRException;
import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.auth.SecurityPolicy;
import platform.server.classes.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.integration.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import retail.api.remote.*;

import java.io.*;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * User: DAle
 * Date: 05.01.12
 * Time: 15:34
 */


public class RetailBusinessLogics extends BusinessLogics<RetailBusinessLogics> implements RetailRemoteInterface {
    ScriptingLogicsModule retailLM;

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
                "/scripts/Supplier.lsf",
                "/scripts/Sales.lsf",
                "/scripts/PriceChange.lsf",
                "/scripts/Machinery.lsf",
                "/scripts/CashRegister.lsf",
                "/scripts/Scales.lsf",
                "/scripts/PriceChecker.lsf",
                "/scripts/Terminal.lsf",
                "/scripts/Label.lsf",
                "/scripts/Default.lsf"
        );
        retailLM = addModuleFromResource("/scripts/retail.lsf");
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }

    @Override
    public List<TransactionInfo> readTransactionInfo(String equServerID) throws RemoteException, SQLException {
        try {
            DataSession session = createSession();
            List<TransactionInfo> transactionList = new ArrayList<TransactionInfo>();

            LCP isMachineryPriceTransaction = retailLM.is(retailLM.findClassByCompoundName("machineryPriceTransaction"));
            Map<Object, KeyExpr> keys = isMachineryPriceTransaction.getMapKeys();
            KeyExpr key = BaseUtils.singleValue(keys);
            Query<Object, Object> query = new Query<Object, Object>(keys);

            query.properties.put("dateTimeMPT", retailLM.findLCPByCompoundName("dateTimeMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("groupMachineryMPT", retailLM.findLCPByCompoundName("groupMachineryMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("snapshotMPT", retailLM.findLCPByCompoundName("groupMachineryMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));

            query.and(retailLM.findLCPByCompoundName("sidEquipmentServerMachineryPriceTransaction").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));
            query.and(retailLM.findLCPByCompoundName("processMachineryPriceTransaction").getExpr(key).getWhere());

            OrderedMap<Map<Object, DataObject>, Map<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object[]> transactionObjects = new ArrayList<Object[]>();
            for (Map.Entry<Map<Object, DataObject>, Map<Object, ObjectValue>> entry : result.entrySet()) {
                DataObject dateTimeMPT = (DataObject) entry.getValue().get("dateTimeMPT");
                DataObject groupMachineryMPT = (DataObject) entry.getValue().get("groupMachineryMPT");
                DataObject transactionObject = entry.getKey().values().iterator().next();
                Boolean snapshotMPT = entry.getValue().get("snapshotMPT") instanceof DataObject;
                transactionObjects.add(new Object[]{groupMachineryMPT, transactionObject, dateTimeCode((Timestamp) dateTimeMPT.getValue()), dateTimeMPT, snapshotMPT});
            }

            List<ItemInfo> itemTransactionList;
            for (Object[] transaction : transactionObjects) {

                DataObject groupObject = (DataObject) transaction[0];
                DataObject transactionObject = (DataObject) transaction[1];
                String dateTimeCode = (String) transaction[2];
                Date date = new Date(((Timestamp) ((DataObject) transaction[3]).getValue()).getTime());
                Boolean snapshotTransaction = (Boolean) transaction[4];

                itemTransactionList = new ArrayList<ItemInfo>();
                KeyExpr barcodeExpr = new KeyExpr("barcode");
                Map<Object, KeyExpr> itemKeys = new HashMap<Object, KeyExpr>();
                itemKeys.put("barcode", barcodeExpr);

                Query<Object, Object> itemQuery = new Query<Object, Object>(itemKeys);
                itemQuery.properties.put("idBarcode", getLCP("Barcode_idBarcode").getExpr(barcodeExpr));
                itemQuery.properties.put("name", retailLM.findLCPByCompoundName("nameMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("price", retailLM.findLCPByCompoundName("priceMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("daysExpiry", retailLM.findLCPByCompoundName("daysExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("hoursExpiry", retailLM.findLCPByCompoundName("hoursExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("expiryDate", retailLM.findLCPByCompoundName("expiryDateSkuDepartmentStoreMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("labelFormat", retailLM.findLCPByCompoundName("labelFormatMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("composition", retailLM.findLCPByCompoundName("compositionMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("isWeight", retailLM.findLCPByCompoundName("isWeightMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("itemGroup", retailLM.findLCPByCompoundName("itemGroupMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));

                itemQuery.and(retailLM.findLCPByCompoundName("inMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr).getWhere());

                OrderedMap<Map<Object, Object>, Map<Object, Object>> itemResult = itemQuery.execute(session.sql);

                for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entry : itemResult.entrySet()) {
                    String barcode = (String) entry.getValue().get("idBarcode");
                    String name = (String) entry.getValue().get("name");
                    Double price = (Double) entry.getValue().get("price");
                    Double daysExpiry = (Double) entry.getValue().get("daysExpiry");
                    Integer hoursExpiry = (Integer) entry.getValue().get("hoursExpiry");
                    Date expiryDate = (Date) entry.getValue().get("expiryDate");
                    Integer labelFormat = (Integer) entry.getValue().get("labelFormat");
                    String composition = (String) entry.getValue().get("composition");
                    Boolean isWeight = entry.getValue().get("isWeight") != null;
                    Integer numberItemGroup = (Integer) entry.getValue().get("itemGroup");
                    String canonicalNameItemGroup = numberItemGroup == null ? "" : (String) retailLM.findLCPByCompoundName("canonicalNameItemGroup").read(session, new DataObject(numberItemGroup, (ConcreteClass) retailLM.findClassByCompoundName("itemGroup")));

                    Integer cellScalesObject = (Integer) retailLM.findLCPByCompoundName("groupScalesCompositionToCellScales").read(session, groupObject, new DataObject(composition, TextClass.instance));
                    Integer compositionNumberCellScales = cellScalesObject == null ? null : (Integer) retailLM.findLCPByCompoundName("numberCellScales").read(session, new DataObject(cellScalesObject, (ConcreteClass) retailLM.findClassByCompoundName("cellScales")));

                    itemTransactionList.add(new ItemInfo(barcode.trim(), name.trim(), price, daysExpiry, hoursExpiry, expiryDate, labelFormat, composition, compositionNumberCellScales, isWeight, numberItemGroup == null ? 0 : numberItemGroup, canonicalNameItemGroup.trim()));
                }

                if (transactionObject.objectClass.equals(retailLM.findClassByCompoundName("cashRegisterPriceTransaction"))) {
                    List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();
                    LCP<PropertyInterface> isCashRegister = LM.is(retailLM.findClassByCompoundName("cashRegister"));

                    Map<PropertyInterface, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                    KeyExpr cashRegisterKey = BaseUtils.singleValue(cashRegisterKeys);
                    Query<PropertyInterface, Object> cashRegisterQuery = new Query<PropertyInterface, Object>(cashRegisterKeys);

                    cashRegisterQuery.properties.put("directoryCashRegister", retailLM.findLCPByCompoundName("directoryCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("portMachinery", retailLM.findLCPByCompoundName("portMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("nppMachinery", retailLM.findLCPByCompoundName("nppMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("numberCashRegister", retailLM.findLCPByCompoundName("numberCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("nameModelMachinery", retailLM.findLCPByCompoundName("nameModelMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("handlerModelMachinery", retailLM.findLCPByCompoundName("handlerModelMachinery").getExpr(cashRegisterKey));

                    cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                    cashRegisterQuery.and(retailLM.findLCPByCompoundName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare(groupObject, Compare.EQUALS));
                    if (snapshotTransaction)
                        cashRegisterQuery.and(retailLM.findLCPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), cashRegisterKey).getWhere());

                    OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                    for (Map.Entry<Map<PropertyInterface, Object>, Map<Object, Object>> values : cashRegisterResult.entrySet()) {

                        String directoryCashRegister = (String) values.getValue().get("directoryCashRegister");
                        String portMachinery = (String) values.getValue().get("portMachinery");
                        Integer nppMachinery = (Integer) values.getValue().get("nppMachinery");
                        String numberCashRegister = (String) values.getValue().get("numberCashRegister");
                        String nameModel = (String) values.getValue().get("nameModelMachinery");
                        String handlerModel = (String) values.getValue().get("handlerModelMachinery");
                        cashRegisterInfoList.add(new CashRegisterInfo(nppMachinery, numberCashRegister, nameModel, handlerModel, portMachinery, directoryCashRegister, null));
                    }

                    transactionList.add(new TransactionCashRegisterInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, date, itemTransactionList, cashRegisterInfoList));

                } else if (transactionObject.objectClass.equals(retailLM.findClassByCompoundName("scalesPriceTransaction"))) {
                    List<ScalesInfo> scalesInfoList = new ArrayList<ScalesInfo>();
                    String directory = (String) retailLM.findLCPByCompoundName("directoryGroupScales").read(session, groupObject);
                    String pieceItemCodeGroupScales = (String) retailLM.findLCPByCompoundName("pieceItemCodeGroupScales").read(session, groupObject);
                    String weightItemCodeGroupScales = (String) retailLM.findLCPByCompoundName("weightItemCodeGroupScales").read(session, groupObject);

                    LCP<PropertyInterface> isScales = LM.is(retailLM.findClassByCompoundName("scales"));

                    Map<PropertyInterface, KeyExpr> scalesKeys = isScales.getMapKeys();
                    KeyExpr scalesKey = BaseUtils.singleValue(scalesKeys);
                    Query<PropertyInterface, Object> scalesQuery = new Query<PropertyInterface, Object>(scalesKeys);

                    scalesQuery.properties.put("portMachinery", retailLM.findLCPByCompoundName("portMachinery").getExpr(scalesKey));
                    scalesQuery.properties.put("nppMachinery", retailLM.findLCPByCompoundName("nppMachinery").getExpr(scalesKey));
                    scalesQuery.properties.put("nameModelMachinery", retailLM.findLCPByCompoundName("nameModelMachinery").getExpr(scalesKey));
                    scalesQuery.properties.put("handlerModelMachinery", retailLM.findLCPByCompoundName("handlerModelMachinery").getExpr(scalesKey));
                    scalesQuery.and(isScales.property.getExpr(scalesKeys).getWhere());
                    scalesQuery.and(retailLM.findLCPByCompoundName("groupScalesScales").getExpr(scalesKey).compare(groupObject, Compare.EQUALS));
                    //if (snapshotTransaction)
                    //    scalesQuery.and(retailLM.findLCPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), scalesKey).getWhere());

                    OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> scalesResult = scalesQuery.execute(session.sql);

                    for (Map<Object, Object> values : scalesResult.values()) {
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        String nameModel = (String) values.get("nameModelMachinery");
                        String handlerModel = (String) values.get("handlerModelMachinery");
                        scalesInfoList.add(new ScalesInfo(nppMachinery, nameModel, handlerModel, portMachinery, directory,
                                pieceItemCodeGroupScales, weightItemCodeGroupScales));
                    }

                    transactionList.add(new TransactionScalesInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, itemTransactionList, scalesInfoList, snapshotTransaction));

                } else if (transactionObject.objectClass.equals(retailLM.findClassByCompoundName("priceCheckerPriceTransaction"))) {
                    List<PriceCheckerInfo> priceCheckerInfoList = new ArrayList<PriceCheckerInfo>();
                    LCP<PropertyInterface> isCheck = LM.is(retailLM.findClassByCompoundName("check"));

                    Map<PropertyInterface, KeyExpr> checkKeys = isCheck.getMapKeys();
                    KeyExpr checkKey = BaseUtils.singleValue(checkKeys);
                    Query<PropertyInterface, Object> checkQuery = new Query<PropertyInterface, Object>(checkKeys);

                    checkQuery.properties.put("portMachinery", retailLM.findLCPByCompoundName("portMachinery").getExpr(checkKey));
                    checkQuery.properties.put("nppMachinery", retailLM.findLCPByCompoundName("nppMachinery").getExpr(checkKey));
                    checkQuery.properties.put("nameCheckModelCheck", retailLM.findLCPByCompoundName("nameCheckModelCheck").getExpr(checkKey));
                    //checkQuery.properties.put("handlerCheckModelCheck", retailLM.findLCPByCompoundName("handlerCheckModelCheck").getExpr(checkKey));
                    checkQuery.and(isCheck.property.getExpr(checkKeys).getWhere());
                    checkQuery.and(retailLM.findLCPByCompoundName("groupPriceCheckerPriceChecker").getExpr(checkKey).compare(groupObject, Compare.EQUALS));

                    if (snapshotTransaction)
                        checkQuery.and(retailLM.findLCPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), checkKey).getWhere());

                    OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> checkResult = checkQuery.execute(session.sql);

                    for (Map<Object, Object> values : checkResult.values()) {
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        String nameModel = (String) values.get("nameCheckModelCheck");
                        //String handlerModel = (String) values.get("handlerCheckModelCheck");
                        String handlerModel = null;
                        priceCheckerInfoList.add(new PriceCheckerInfo(nppMachinery, nameModel, handlerModel, portMachinery));
                    }
                    transactionList.add(new TransactionPriceCheckerInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, itemTransactionList, priceCheckerInfoList));


                } else if (transactionObject.objectClass.equals(retailLM.findClassByCompoundName("terminalPriceTransaction"))) {
                    List<TerminalInfo> terminalInfoList = new ArrayList<TerminalInfo>();
                    LCP<PropertyInterface> isTerminal = LM.is(retailLM.findClassByCompoundName("terminal"));

                    OrderedMap<PropertyInterface, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                    KeyExpr terminalKey = BaseUtils.singleValue(terminalKeys);
                    Query<PropertyInterface, Object> terminalQuery = new Query<PropertyInterface, Object>(terminalKeys);

                    terminalQuery.properties.put("directoryTerminal", retailLM.findLCPByCompoundName("directoryTerminal").getExpr(terminalKey));
                    terminalQuery.properties.put("portMachinery", retailLM.findLCPByCompoundName("portMachinery").getExpr(terminalKey));
                    terminalQuery.properties.put("nppMachinery", retailLM.findLCPByCompoundName("nppMachinery").getExpr(terminalKey));
                    terminalQuery.properties.put("nameModelMachinery", retailLM.findLCPByCompoundName("nameModelMachinery").getExpr(terminalKey));
                    terminalQuery.properties.put("handlerModelMachinery", retailLM.findLCPByCompoundName("handlerModelMachinery").getExpr(terminalKey));
                    terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                    terminalQuery.and(retailLM.findLCPByCompoundName("groupTerminalTerminal").getExpr(terminalKey).compare(groupObject, Compare.EQUALS));

                    OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> terminalResult = terminalQuery.execute(session.sql);

                    for (Map<Object, Object> values : terminalResult.values()) {
                        String directory = (String) values.get("directoryTerminal");
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        String nameModel = (String) values.get("nameModelMachinery");
                        String handlerModel = (String) values.get("handlerModelMachinery");
                        terminalInfoList.add(new TerminalInfo(directory, nppMachinery, nameModel, handlerModel, portMachinery));
                    }
                    transactionList.add(new TransactionTerminalInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, itemTransactionList, terminalInfoList, snapshotTransaction));
                }
            }
            return transactionList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public List<CashRegisterInfo> readCashRegisterInfo(String equServerID) throws RemoteException, SQLException {
        try {
            DataSession session = createSession();
            List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();

            LCP<PropertyInterface> isGroupMachinery = retailLM.is(retailLM.findClassByCompoundName("groupMachinery"));
            OrderedMap<PropertyInterface, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = BaseUtils.singleValue(keys);
            Query<PropertyInterface, Object> query = new Query<PropertyInterface, Object>(keys);
            query.properties.put("roundSalesGroupCashRegister", retailLM.findLCPByCompoundName("roundSalesGroupCashRegister").getExpr(key));
            query.and(retailLM.findLCPByCompoundName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

            OrderedMap<Map<PropertyInterface, DataObject>, Map<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object[]> groupMachineryObjects = new ArrayList<Object[]>();
            for (Map.Entry<Map<PropertyInterface, DataObject>, Map<Object, ObjectValue>> entry : result.entrySet()) {
                DataObject groupMachineryObject = entry.getKey().values().iterator().next();
                Integer roundSalesGroupCashRegister = (Integer) entry.getValue().get("roundSalesGroupCashRegister").getValue();
                groupMachineryObjects.add(new Object[]{groupMachineryObject, roundSalesGroupCashRegister});
            }

            for (Object[] groupMachinery : groupMachineryObjects) {
                DataObject groupMachineryObject = (DataObject) groupMachinery[0];
                Integer roundSalesGroupCashRegister = (Integer) groupMachinery[1];

                LCP<PropertyInterface> isCashRegister = LM.is(retailLM.findClassByCompoundName("cashRegister"));

                OrderedMap<PropertyInterface, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                KeyExpr cashRegisterKey = BaseUtils.singleValue(cashRegisterKeys);
                Query<PropertyInterface, Object> cashRegisterQuery = new Query<PropertyInterface, Object>(cashRegisterKeys);

                cashRegisterQuery.properties.put("directoryCashRegister", retailLM.findLCPByCompoundName("directoryCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("portMachinery", retailLM.findLCPByCompoundName("portMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("nppMachinery", retailLM.findLCPByCompoundName("nppMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("numberCashRegister", retailLM.findLCPByCompoundName("numberCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("nameModelMachinery", retailLM.findLCPByCompoundName("nameModelMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("handlerModelMachinery", retailLM.findLCPByCompoundName("handlerModelMachinery").getExpr(cashRegisterKey));

                cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                cashRegisterQuery.and(retailLM.findLCPByCompoundName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

                OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                for (Map.Entry<Map<PropertyInterface, Object>, Map<Object, Object>> values : cashRegisterResult.entrySet()) {

                    String directoryCashRegister = (String) values.getValue().get("directoryCashRegister");
                    String portMachinery = (String) values.getValue().get("portMachinery");
                    Integer nppMachinery = (Integer) values.getValue().get("nppMachinery");
                    String numberCashRegister = (String) values.getValue().get("numberCashRegister");
                    String nameModel = (String) values.getValue().get("nameModelMachinery");
                    String handlerModel = (String) values.getValue().get("handlerModelMachinery");
                    cashRegisterInfoList.add(new CashRegisterInfo(nppMachinery, numberCashRegister, nameModel, handlerModel, portMachinery, directoryCashRegister, roundSalesGroupCashRegister));
                }
            }
            return cashRegisterInfoList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public List<TerminalInfo> readTerminalInfo(String equServerID) throws RemoteException, SQLException {
        try {
            DataSession session = createSession();
            List<TerminalInfo> terminalInfoList = new ArrayList<TerminalInfo>();

            LCP<PropertyInterface> isGroupMachinery = retailLM.is(retailLM.findClassByCompoundName("groupMachinery"));
            OrderedMap<PropertyInterface, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = BaseUtils.singleValue(keys);
            Query<PropertyInterface, Object> query = new Query<PropertyInterface, Object>(keys);
            query.and(retailLM.findLCPByCompoundName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

            OrderedMap<Map<PropertyInterface, DataObject>, Map<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object> groupMachineryObjects = new ArrayList<Object>();
            for (Map.Entry<Map<PropertyInterface, DataObject>, Map<Object, ObjectValue>> entry : result.entrySet()) {
                DataObject groupMachineryObject = entry.getKey().values().iterator().next();
                groupMachineryObjects.add(groupMachineryObject);
            }

            for (Object groupMachinery : groupMachineryObjects) {
                DataObject groupMachineryObject = (DataObject) groupMachinery;

                LCP<PropertyInterface> isTerminal = LM.is(retailLM.findClassByCompoundName("terminal"));

                OrderedMap<PropertyInterface, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                KeyExpr terminalKey = BaseUtils.singleValue(terminalKeys);
                Query<PropertyInterface, Object> terminalQuery = new Query<PropertyInterface, Object>(terminalKeys);

                terminalQuery.properties.put("directoryTerminal", retailLM.findLCPByCompoundName("directoryTerminal").getExpr(terminalKey));
                terminalQuery.properties.put("portMachinery", retailLM.findLCPByCompoundName("portMachinery").getExpr(terminalKey));
                terminalQuery.properties.put("nppMachinery", retailLM.findLCPByCompoundName("nppMachinery").getExpr(terminalKey));
                terminalQuery.properties.put("nameModelMachinery", retailLM.findLCPByCompoundName("nameModelMachinery").getExpr(terminalKey));
                terminalQuery.properties.put("handlerModelMachinery", retailLM.findLCPByCompoundName("handlerModelMachinery").getExpr(terminalKey));

                terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                terminalQuery.and(retailLM.findLCPByCompoundName("groupTerminalTerminal").getExpr(terminalKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

                OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> terminalResult = terminalQuery.execute(session.sql);

                for (Map.Entry<Map<PropertyInterface, Object>, Map<Object, Object>> values : terminalResult.entrySet()) {

                    String directoryTerminal = (String) values.getValue().get("directoryTerminal");
                    String portMachinery = (String) values.getValue().get("portMachinery");
                    Integer nppMachinery = (Integer) values.getValue().get("nppMachinery");
                    String nameModel = (String) values.getValue().get("nameModelMachinery");
                    String handlerModel = (String) values.getValue().get("handlerModelMachinery");
                    terminalInfoList.add(new TerminalInfo(directoryTerminal, nppMachinery, nameModel, handlerModel, portMachinery));
                }
            }
            return terminalInfoList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public List<TerminalDocumentTypeInfo> readTerminalDocumentTypeInfo() throws RemoteException, SQLException {
        try {
            DataSession session = createSession();

            List<LegalEntityInfo> legalEntityInfoList = new ArrayList<LegalEntityInfo>();
            LCP<PropertyInterface> isSupplier = LM.is(retailLM.findClassByCompoundName("supplier"));

            OrderedMap<PropertyInterface, KeyExpr> supplierKeys = isSupplier.getMapKeys();
            KeyExpr supplierKey = BaseUtils.singleValue(supplierKeys);
            Query<PropertyInterface, Object> supplierQuery = new Query<PropertyInterface, Object>(supplierKeys);

            supplierQuery.properties.put("name", retailLM.baseLM.name.getExpr(supplierKey));
            supplierQuery.and(isSupplier.property.getExpr(supplierKeys).getWhere());
            OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> supplierResult = supplierQuery.execute(session.sql);
            for (Map.Entry<Map<PropertyInterface, Object>, Map<Object, Object>> entry : supplierResult.entrySet()) {
                String id = String.valueOf(entry.getKey().values().iterator().next());
                String name = (String) entry.getValue().get("name");
                DataObject terminalHandbookTypeObject = ((StaticCustomClass) retailLM.findClassByCompoundName("terminalHandbookType")).getDataObject("supplier");
                String type = (String) retailLM.findLCPByCompoundName("idTerminalHandbookType").read(session, terminalHandbookTypeObject);
                legalEntityInfoList.add(new LegalEntityInfo(id, name, type));
            }

            LCP<PropertyInterface> isCustomer = LM.is(retailLM.findClassByCompoundName("customer"));

            Map<PropertyInterface, KeyExpr> customerKeys = isCustomer.getMapKeys();
            KeyExpr customerKey = BaseUtils.singleValue(customerKeys);
            Query<PropertyInterface, Object> customerQuery = new Query<PropertyInterface, Object>(customerKeys);

            customerQuery.properties.put("name", retailLM.baseLM.name.getExpr(customerKey));
            customerQuery.and(isCustomer.property.getExpr(customerKeys).getWhere());
            OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> customerResult = customerQuery.execute(session.sql);
            for (Map.Entry<Map<PropertyInterface, Object>, Map<Object, Object>> entry : customerResult.entrySet()) {
                String id = String.valueOf(entry.getKey().values().iterator().next());
                String name = (String) entry.getValue().get("name");
                DataObject terminalHandbookTypeObject = ((StaticCustomClass) retailLM.findClassByCompoundName("terminalHandbookType")).getDataObject("customer");
                String type = (String) retailLM.findLCPByCompoundName("idTerminalHandbookType").read(session, terminalHandbookTypeObject);
                legalEntityInfoList.add(new LegalEntityInfo(id, name, type));
            }

            List<TerminalDocumentTypeInfo> terminalDocumentTypeInfoList = new ArrayList<TerminalDocumentTypeInfo>();
            LCP<PropertyInterface> isTerminalDocumentType = LM.is(retailLM.findClassByCompoundName("terminalDocumentType"));

            Map<PropertyInterface, KeyExpr> terminalDocumentTypeKeys = isTerminalDocumentType.getMapKeys();
            KeyExpr terminalDocumentTypeKey = BaseUtils.singleValue(terminalDocumentTypeKeys);
            Query<PropertyInterface, Object> terminalDocumentTypeQuery = new Query<PropertyInterface, Object>(terminalDocumentTypeKeys);
            terminalDocumentTypeQuery.properties.put("idTerminalDocumentType", retailLM.findLCPByCompoundName("idTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("nameTerminalDocumentType", retailLM.findLCPByCompoundName("nameTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("nameInHandbook1TerminalDocumentType", retailLM.findLCPByCompoundName("nameInHandbook1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("idTerminalHandbookType1TerminalDocumentType", retailLM.findLCPByCompoundName("idTerminalHandbookType1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("nameInHandbook2TerminalDocumentType", retailLM.findLCPByCompoundName("nameInHandbook2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("idTerminalHandbookType2TerminalDocumentType", retailLM.findLCPByCompoundName("idTerminalHandbookType2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.and(isTerminalDocumentType.property.getExpr(terminalDocumentTypeKeys).getWhere());

            OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> terminalDocumentTypeResult = terminalDocumentTypeQuery.execute(session.sql);

            for (Map<Object, Object> values : terminalDocumentTypeResult.values()) {
                String id = (String) values.get("idTerminalDocumentType");
                String name = (String) values.get("nameTerminalDocumentType");
                String nameInHandbook1 = (String) values.get("nameInHandbook1TerminalDocumentType");
                String idTerminalHandbookType1 = (String) values.get("idTerminalHandbookType1TerminalDocumentType");
                String nameInHandbook2 = (String) values.get("nameInHandbook2TerminalDocumentType");
                String idTerminalHandbookType2 = (String) values.get("idTerminalHandbookType1TerminalDocumentType");
                terminalDocumentTypeInfoList.add(new TerminalDocumentTypeInfo(id, name, nameInHandbook1, idTerminalHandbookType1,
                        nameInHandbook2, idTerminalHandbookType2, legalEntityInfoList));
            }
            return terminalDocumentTypeInfoList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public String sendSalesInfo(List<SalesInfo> salesInfoList, String equipmentServer) throws IOException, SQLException {
        try {
            DataSession session = createSession();
            ImportField cashRegisterField = new ImportField(retailLM.findLCPByCompoundName("numberCashRegister"));
            ImportField zReportNumberField = new ImportField(retailLM.findLCPByCompoundName("numberZReport"));

            ImportField numberBillField = new ImportField(retailLM.findLCPByCompoundName("numberBill"));
            ImportField dateField = new ImportField(retailLM.findLCPByCompoundName("dateBill"));
            ImportField timeField = new ImportField(retailLM.findLCPByCompoundName("timeBill"));

            ImportField numberBillDetailField = new ImportField(retailLM.findLCPByCompoundName("numberBillDetail"));
            ImportField idBarcodeBillDetailField = new ImportField(retailLM.findLCPByCompoundName("idBarcodeBillDetail"));

            ImportField quantityBillSaleDetailField = new ImportField(retailLM.findLCPByCompoundName("quantityBillSaleDetail"));
            ImportField retailPriceBillSaleDetailField = new ImportField(retailLM.findLCPByCompoundName("retailPriceBillSaleDetail"));
            ImportField retailSumBillSaleDetailField = new ImportField(retailLM.findLCPByCompoundName("retailSumBillSaleDetail"));
            ImportField discountSumBillSaleDetailField = new ImportField(retailLM.findLCPByCompoundName("discountSumBillSaleDetail"));
            ImportField discountSumSaleBillField = new ImportField(retailLM.findLCPByCompoundName("discountSumSaleBill"));

            ImportField quantityBillReturnDetailField = new ImportField(retailLM.findLCPByCompoundName("quantityBillReturnDetail"));
            ImportField retailPriceBillReturnDetailField = new ImportField(retailLM.findLCPByCompoundName("retailPriceBillReturnDetail"));
            ImportField retailSumBillReturnDetailField = new ImportField(retailLM.findLCPByCompoundName("retailSumBillReturnDetail"));
            ImportField discountSumBillReturnDetailField = new ImportField(retailLM.findLCPByCompoundName("discountSumBillReturnDetail"));
            ImportField discountSumReturnBillField = new ImportField(retailLM.findLCPByCompoundName("discountSumReturnBill"));

            ImportField sidTypePaymentField = new ImportField(retailLM.findLCPByCompoundName("sidPaymentType"));
            ImportField sumPaymentField = new ImportField(retailLM.findLCPByCompoundName("sumPayment"));
            ImportField numberPaymentField = new ImportField(retailLM.findLCPByCompoundName("numberPayment"));

            List<ImportProperty<?>> saleProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> returnProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> paymentProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> zReportKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("zReportPosted"), retailLM.findLCPByCompoundName("numberNumberCashRegisterToZReportPosted").getMapping(zReportNumberField, cashRegisterField));
            ImportKey<?> cashRegisterKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("cashRegister"), retailLM.findLCPByCompoundName("cashRegisterNumber").getMapping(cashRegisterField));
            ImportKey<?> billKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("bill"), retailLM.findLCPByCompoundName("zReportBillToBill").getMapping(zReportNumberField, numberBillField, cashRegisterField));
            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("item"), getLCP("Barcode_skuBarcodeIdDate").getMapping(idBarcodeBillDetailField, dateField));

            saleProperties.add(new ImportProperty(zReportNumberField, retailLM.findLCPByCompoundName("numberZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(cashRegisterField, retailLM.findLCPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("cashRegister")).getMapping(cashRegisterKey)));
            saleProperties.add(new ImportProperty(dateField, retailLM.findLCPByCompoundName("dateZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(timeField, retailLM.findLCPByCompoundName("timeZReport").getMapping(zReportKey)));

            saleProperties.add(new ImportProperty(numberBillField, retailLM.findLCPByCompoundName("numberBill").getMapping(billKey)));
            saleProperties.add(new ImportProperty(dateField, retailLM.findLCPByCompoundName("dateBill").getMapping(billKey)));
            saleProperties.add(new ImportProperty(timeField, retailLM.findLCPByCompoundName("timeBill").getMapping(billKey)));
            saleProperties.add(new ImportProperty(discountSumSaleBillField, retailLM.findLCPByCompoundName("discountSumSaleBill").getMapping(billKey)));
            saleProperties.add(new ImportProperty(zReportNumberField, retailLM.findLCPByCompoundName("zReportBill").getMapping(billKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("zReport")).getMapping(zReportKey)));

            ImportKey<?> billSaleDetailKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("billSaleDetail"), retailLM.findLCPByCompoundName("zReportBillBillDetailToBillDetail").getMapping(zReportNumberField, numberBillField, numberBillDetailField, cashRegisterField));
            saleProperties.add(new ImportProperty(numberBillDetailField, retailLM.findLCPByCompoundName("numberBillDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(idBarcodeBillDetailField, retailLM.findLCPByCompoundName("idBarcodeBillDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(quantityBillSaleDetailField, retailLM.findLCPByCompoundName("quantityBillSaleDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(retailPriceBillSaleDetailField, retailLM.findLCPByCompoundName("retailPriceBillSaleDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(retailSumBillSaleDetailField, retailLM.findLCPByCompoundName("retailSumBillSaleDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(discountSumBillSaleDetailField, retailLM.findLCPByCompoundName("discountSumBillSaleDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(numberBillField, retailLM.findLCPByCompoundName("billBillDetail").getMapping(billSaleDetailKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("bill")).getMapping(billKey)));

            saleProperties.add(new ImportProperty(idBarcodeBillDetailField, retailLM.findLCPByCompoundName("itemBillSaleDetail").getMapping(billSaleDetailKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("item")).getMapping(itemKey)));


            returnProperties.add(new ImportProperty(zReportNumberField, retailLM.findLCPByCompoundName("numberZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(cashRegisterField, retailLM.findLCPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("cashRegister")).getMapping(cashRegisterKey)));
            returnProperties.add(new ImportProperty(dateField, retailLM.findLCPByCompoundName("dateZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(timeField, retailLM.findLCPByCompoundName("timeZReport").getMapping(zReportKey)));

            returnProperties.add(new ImportProperty(numberBillField, retailLM.findLCPByCompoundName("numberBill").getMapping(billKey)));
            returnProperties.add(new ImportProperty(dateField, retailLM.findLCPByCompoundName("dateBill").getMapping(billKey)));
            returnProperties.add(new ImportProperty(timeField, retailLM.findLCPByCompoundName("timeBill").getMapping(billKey)));
            returnProperties.add(new ImportProperty(discountSumReturnBillField, retailLM.findLCPByCompoundName("discountSumReturnBill").getMapping(billKey)));
            returnProperties.add(new ImportProperty(zReportNumberField, retailLM.findLCPByCompoundName("zReportBill").getMapping(billKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("zReport")).getMapping(zReportKey)));

            ImportKey<?> billReturnDetailKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("billReturnDetail"), retailLM.findLCPByCompoundName("zReportBillBillDetailToBillDetail").getMapping(zReportNumberField, numberBillField, numberBillDetailField, cashRegisterField));
            returnProperties.add(new ImportProperty(numberBillDetailField, retailLM.findLCPByCompoundName("numberBillDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(idBarcodeBillDetailField, retailLM.findLCPByCompoundName("idBarcodeBillDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(quantityBillReturnDetailField, retailLM.findLCPByCompoundName("quantityBillReturnDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(retailPriceBillReturnDetailField, retailLM.findLCPByCompoundName("retailPriceBillReturnDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(retailSumBillReturnDetailField, retailLM.findLCPByCompoundName("retailSumBillReturnDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(discountSumBillReturnDetailField, retailLM.findLCPByCompoundName("discountSumBillReturnDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(numberBillField, retailLM.findLCPByCompoundName("billBillDetail").getMapping(billReturnDetailKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("bill")).getMapping(billKey)));

            returnProperties.add(new ImportProperty(idBarcodeBillDetailField, retailLM.findLCPByCompoundName("itemBillReturnDetail").getMapping(billReturnDetailKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("item")).getMapping(itemKey)));

            List<List<Object>> dataSale = new ArrayList<List<Object>>();
            List<List<Object>> dataReturn = new ArrayList<List<Object>>();

            List<List<Object>> dataPayment = new ArrayList<List<Object>>();

            if (salesInfoList != null)
                for (SalesInfo sale : salesInfoList) {
                    if (sale.quantityBillDetail < 0)
                        dataReturn.add(Arrays.<Object>asList(sale.cashRegisterNumber, sale.zReportNumber, sale.date, sale.time, sale.billNumber,
                                sale.numberBillDetail, sale.barcodeItem, sale.quantityBillDetail, sale.priceBillDetail, sale.sumBillDetail,
                                sale.discountSumBillDetail, sale.discountSumBill));
                    else
                        dataSale.add(Arrays.<Object>asList(sale.cashRegisterNumber, sale.zReportNumber, sale.date, sale.time, sale.billNumber,
                                sale.numberBillDetail, sale.barcodeItem, sale.quantityBillDetail, sale.priceBillDetail, sale.sumBillDetail,
                                sale.discountSumBillDetail, sale.discountSumBill));
                    if (sale.sumCash != 0)
                        dataPayment.add(Arrays.<Object>asList(sale.zReportNumber, sale.billNumber, sale.cashRegisterNumber, "cash", sale.sumCash, 1));
                    if (sale.sumCard != 0)
                        dataPayment.add(Arrays.<Object>asList(sale.zReportNumber, sale.billNumber, sale.cashRegisterNumber, "card", sale.sumCard, 2));
                }

            List<ImportField> saleImportFields = Arrays.asList(cashRegisterField, zReportNumberField, dateField, timeField,
                    numberBillField, numberBillDetailField, idBarcodeBillDetailField, quantityBillSaleDetailField,
                    retailPriceBillSaleDetailField, retailSumBillSaleDetailField, discountSumBillSaleDetailField,
                    discountSumSaleBillField);

            List<ImportField> returnImportFields = Arrays.asList(cashRegisterField, zReportNumberField, dateField, timeField,
                    numberBillField, numberBillDetailField, idBarcodeBillDetailField, quantityBillReturnDetailField,
                    retailPriceBillReturnDetailField, retailSumBillReturnDetailField, discountSumBillReturnDetailField,
                    discountSumReturnBillField);


            new IntegrationService(session, new ImportTable(saleImportFields, dataSale), Arrays.asList(zReportKey, cashRegisterKey, billKey, billSaleDetailKey, itemKey),
                    saleProperties).synchronize(true);

            new IntegrationService(session, new ImportTable(returnImportFields, dataReturn), Arrays.asList(zReportKey, cashRegisterKey, billKey, billReturnDetailKey, itemKey),
                    returnProperties).synchronize(true);

            ImportKey<?> paymentKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("payment"), retailLM.findLCPByCompoundName("zReportBillPaymentToPayment").getMapping(zReportNumberField, numberBillField, numberPaymentField, cashRegisterField));
            ImportKey<?> paymentTypeKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("paymentType"), retailLM.findLCPByCompoundName("sidToTypePayment").getMapping(sidTypePaymentField));
            paymentProperties.add(new ImportProperty(sumPaymentField, retailLM.findLCPByCompoundName("sumPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(numberPaymentField, retailLM.findLCPByCompoundName("numberPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(sidTypePaymentField, retailLM.findLCPByCompoundName("paymentTypePayment").getMapping(paymentKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("paymentType")).getMapping(paymentTypeKey)));
            paymentProperties.add(new ImportProperty(numberBillField, retailLM.findLCPByCompoundName("billPayment").getMapping(paymentKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("bill")).getMapping(billKey)));

            List<ImportField> paymentImportFields = Arrays.asList(zReportNumberField, numberBillField, cashRegisterField, sidTypePaymentField,
                    sumPaymentField, numberPaymentField);

            if (salesInfoList != null && salesInfoList.size() != 0) {
                String message = "Загружено записей: " + (dataSale.size() + dataReturn.size());
                List<String> cashRegisterNumbers = new ArrayList<String>();
                List<String> fileNames = new ArrayList<String>();
                for (SalesInfo salesInfo : salesInfoList) {
                    if (!cashRegisterNumbers.contains(salesInfo.cashRegisterNumber.trim()))
                        cashRegisterNumbers.add(salesInfo.cashRegisterNumber.trim());
                    if ((salesInfo.filename != null) && (!fileNames.contains(salesInfo.filename.trim())))
                        fileNames.add(salesInfo.filename.trim());
                }
                message += "\nИз касс: ";
                for (String cashRegisterNumber : cashRegisterNumbers)
                    message += cashRegisterNumber + ", ";
                message = message.substring(0, message.length() - 2);

                message += "\nИз файлов: ";
                for (String filename : fileNames)
                    message += filename + ", ";
                message = message.substring(0, message.length() - 2);

                DataObject logObject = session.addObject((ConcreteCustomClass) retailLM.findClassByCompoundName("equipmentServerLog"));
                Object equipmentServerObject = retailLM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
                retailLM.findLCPByCompoundName("equipmentServerEquipmentServerLog").change(equipmentServerObject, session, logObject);
                retailLM.findLCPByCompoundName("dataEquipmentServerLog").change(message, session, logObject);
                retailLM.findLCPByCompoundName("dateEquipmentServerLog").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            new IntegrationService(session, new ImportTable(paymentImportFields, dataPayment), Arrays.asList(paymentKey, paymentTypeKey, billKey, cashRegisterKey),
                    paymentProperties).synchronize(true);

            return session.applyMessage(this);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public String sendTerminalDocumentInfo(List<TerminalDocumentInfo> terminalDocumentInfoList, String equServerID) throws IOException, SQLException {
        try {
            DataSession session = createSession();
            ImportField idTerminalDocumentField = new ImportField(retailLM.findLCPByCompoundName("idTerminalDocument"));
            ImportField typeTerminalDocumentField = new ImportField(retailLM.findLCPByCompoundName("idTerminalDocumentTypeTerminalDocument"));
            ImportField idTerminalHandbookType1TerminalDocumentField = new ImportField(retailLM.findLCPByCompoundName("idTerminalHandbookType1TerminalDocument"));
            ImportField idTerminalHandbookType2TerminalDocumentField = new ImportField(retailLM.findLCPByCompoundName("idTerminalHandbookType2TerminalDocument"));
            ImportField titleTerminalDocumentField = new ImportField(retailLM.findLCPByCompoundName("titleTerminalDocument"));
            ImportField quantityTerminalDocumentField = new ImportField(retailLM.findLCPByCompoundName("quantityTerminalDocument"));

            ImportField numberTerminalDocumentDetailField = new ImportField(retailLM.findLCPByCompoundName("numberTerminalDocumentDetail"));
            ImportField barcodeTerminalDocumentDetailField = new ImportField(retailLM.findLCPByCompoundName("barcodeTerminalDocumentDetail"));
            ImportField nameTerminalDocumentDetailField = new ImportField(retailLM.findLCPByCompoundName("nameTerminalDocumentDetail"));
            ImportField quantityTerminalDocumentDetailField = new ImportField(retailLM.findLCPByCompoundName("quantityTerminalDocumentDetail"));
            ImportField priceTerminalDocumentDetailField = new ImportField(retailLM.findLCPByCompoundName("priceTerminalDocumentDetail"));
            ImportField sumTerminalDocumentDetailField = new ImportField(retailLM.findLCPByCompoundName("sumTerminalDocumentDetail"));

            ImportField isNewTerminalDocumentDetailField = new ImportField(retailLM.findLCPByCompoundName("isNewTerminalDocumentDetail"));

            List<ImportProperty<?>> terminalDocumentProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> terminalDocumentDetailProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> terminalDocumentKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("terminalDocument"), retailLM.findLCPByCompoundName("terminalDocumentID").getMapping(idTerminalDocumentField));

            terminalDocumentProperties.add(new ImportProperty(idTerminalDocumentField, retailLM.findLCPByCompoundName("idTerminalDocument").getMapping(terminalDocumentKey)));
            //terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, retailLM.findLCPByCompoundName("typeTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(titleTerminalDocumentField, retailLM.findLCPByCompoundName("titleTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType1TerminalDocumentField, retailLM.findLCPByCompoundName("idTerminalHandbookType1TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType2TerminalDocumentField, retailLM.findLCPByCompoundName("idTerminalHandbookType2TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(quantityTerminalDocumentField, retailLM.findLCPByCompoundName("quantityTerminalDocument").getMapping(terminalDocumentKey)));

            ImportKey<?> terminalDocumentDetailKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("terminalDocumentDetail"), retailLM.findLCPByCompoundName("terminalDocumentDetailIDDocumentIDDetail").getMapping(idTerminalDocumentField, numberTerminalDocumentDetailField));

            terminalDocumentDetailProperties.add(new ImportProperty(numberTerminalDocumentDetailField, retailLM.findLCPByCompoundName("numberTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(barcodeTerminalDocumentDetailField, retailLM.findLCPByCompoundName("barcodeTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(nameTerminalDocumentDetailField, retailLM.findLCPByCompoundName("nameTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(quantityTerminalDocumentDetailField, retailLM.findLCPByCompoundName("quantityTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(priceTerminalDocumentDetailField, retailLM.findLCPByCompoundName("priceTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(sumTerminalDocumentDetailField, retailLM.findLCPByCompoundName("sumTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(isNewTerminalDocumentDetailField, retailLM.findLCPByCompoundName("isNewTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(idTerminalDocumentField, retailLM.findLCPByCompoundName("terminalDocumentTerminalDocumentDetail").getMapping(terminalDocumentDetailKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("terminalDocument")).getMapping(terminalDocumentKey)));


            ImportKey<?> terminalDocumentTypeKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("terminalDocumentType"), retailLM.findLCPByCompoundName("terminalDocumentTypeID").getMapping(typeTerminalDocumentField));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, retailLM.findLCPByCompoundName("idTerminalDocumentType").getMapping(terminalDocumentTypeKey)));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, retailLM.findLCPByCompoundName("terminalDocumentTypeTerminalDocument").getMapping(terminalDocumentKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("terminalDocumentType")).getMapping(terminalDocumentTypeKey)));

            List<List<Object>> dataTerminalDocument = new ArrayList<List<Object>>();
            List<List<Object>> dataTerminalDocumentDetail = new ArrayList<List<Object>>();

            for (TerminalDocumentInfo docInfo : terminalDocumentInfoList) {
                dataTerminalDocument.add(Arrays.<Object>asList(docInfo.idDocument, docInfo.typeDocument, docInfo.handbook1,
                        docInfo.handbook2, docInfo.title, docInfo.quantity));
                Integer number = 1;
                for (TerminalDocumentDetailInfo docDetailInfo : docInfo.terminalDocumentDetailInfoList) {
                    dataTerminalDocumentDetail.add(Arrays.<Object>asList(number, docDetailInfo.barcode, docDetailInfo.name,
                            docDetailInfo.isNew, docDetailInfo.quantity, docDetailInfo.price, docDetailInfo.sum, docInfo.idDocument));
                    number++;
                }
            }

            List<ImportField> terminalDocumentImportFields = Arrays.asList(idTerminalDocumentField, typeTerminalDocumentField,
                    idTerminalHandbookType1TerminalDocumentField, idTerminalHandbookType2TerminalDocumentField,
                    titleTerminalDocumentField, quantityTerminalDocumentField);

            new IntegrationService(session, new ImportTable(terminalDocumentImportFields, dataTerminalDocument),
                    Arrays.asList(terminalDocumentKey, terminalDocumentTypeKey),
                    terminalDocumentProperties).synchronize(true);

            List<ImportField> terminalDocumentDetailImportFields = Arrays.asList(numberTerminalDocumentDetailField,
                    barcodeTerminalDocumentDetailField, nameTerminalDocumentDetailField, isNewTerminalDocumentDetailField,
                    quantityTerminalDocumentDetailField, priceTerminalDocumentDetailField, sumTerminalDocumentDetailField,
                    idTerminalDocumentField);


            new IntegrationService(session, new ImportTable(terminalDocumentDetailImportFields, dataTerminalDocumentDetail),
                    Arrays.asList(terminalDocumentDetailKey, terminalDocumentKey), terminalDocumentDetailProperties).synchronize(true);


            if (terminalDocumentInfoList.size() != 0) {
                String message = "Загружено записей: " + dataTerminalDocument.size();

                DataObject logObject = session.addObject((ConcreteCustomClass) retailLM.findClassByCompoundName("equipmentServerLog"));
                Object equipmentServerObject = retailLM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equServerID, StringClass.get(20)));
                retailLM.findLCPByCompoundName("equipmentServerEquipmentServerLog").change(equipmentServerObject, session, logObject);
                retailLM.findLCPByCompoundName("dataEquipmentServerLog").change(message, session, logObject);
                retailLM.findLCPByCompoundName("dateEquipmentServerLog").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            return session.applyMessage(this);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public void succeedTransaction(Integer transactionID) throws RemoteException, SQLException {
        try {
            DataSession session = createSession();
            retailLM.findLCPByCompoundName("succeededMachineryPriceTransaction").change(true, session,
                                                                                        session.getDataObject(transactionID, retailLM.findClassByCompoundName("machineryPriceTransaction").getType()));
            session.apply(this);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public List<byte[][]> readLabelFormats(List<String> scalesModelsList) throws RemoteException, SQLException {
        try {
            DataSession session = createSession();

            List<byte[][]> fileLabelFormats = new ArrayList<byte[][]>();

            for (String scalesModel : scalesModelsList) {

                DataObject scalesModelObject = new DataObject(retailLM.findLCPByCompoundName("scalesModelName").read(session, new DataObject(scalesModel)), (ConcreteClass) retailLM.findClassByCompoundName("scalesModel"));

                LCP<PropertyInterface> isLabelFormat = LM.is(retailLM.findClassByCompoundName("labelFormat"));

                OrderedMap<PropertyInterface, KeyExpr> labelFormatKeys = isLabelFormat.getMapKeys();
                KeyExpr labelFormatKey = BaseUtils.singleValue(labelFormatKeys);
                Query<PropertyInterface, Object> labelFormatQuery = new Query<PropertyInterface, Object>(labelFormatKeys);

                labelFormatQuery.properties.put("fileLabelFormat", retailLM.findLCPByCompoundName("fileLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.properties.put("fileMessageLabelFormat", retailLM.findLCPByCompoundName("fileMessageLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.and(isLabelFormat.property.getExpr(labelFormatKeys).getWhere());
                labelFormatQuery.and(retailLM.findLCPByCompoundName("scalesModelLabelFormat").getExpr(labelFormatKey).compare((scalesModelObject).getExpr(), Compare.EQUALS));

                OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> labelFormatResult = labelFormatQuery.execute(session.sql);

                for (Map.Entry<Map<PropertyInterface, Object>, Map<Object, Object>> entry : labelFormatResult.entrySet()) {
                    byte[] fileLabelFormat = (byte[]) entry.getValue().get("fileLabelFormat");
                    byte[] fileMessageLabelFormat = (byte[]) entry.getValue().get("fileMessageLabelFormat");
                    fileLabelFormats.add(new byte[][]{fileLabelFormat, fileMessageLabelFormat});
                }
            }
            return fileLabelFormats;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public void errorTransactionReport(Integer transactionID, Exception e) throws RemoteException, SQLException {
        try {
            DataSession session = createSession();
            DataObject errorObject = session.addObject((ConcreteCustomClass) retailLM.findClassByCompoundName("machineryPriceTransactionError"));
            retailLM.findLCPByCompoundName("machineryPriceTransactionMachineryPriceTransactionError").change(transactionID, session, errorObject);
            retailLM.findLCPByCompoundName("dataMachineryPriceTransactionError").change(e.toString(), session, errorObject);
            retailLM.findLCPByCompoundName("dateMachineryPriceTransactionError").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(os));
            retailLM.findLCPByCompoundName("errorTraceMachineryPriceTransactionError").change(os.toString(), session, errorObject);

            session.apply(this);
        } catch (ScriptingErrorLog.SemanticErrorException e2) {
            throw new RemoteException(e2.toString());
        }
    }

    @Override
    public void errorEquipmentServerReport(String equipmentServer, Throwable exception) throws
            RemoteException, SQLException {
        try {
            DataSession session = createSession();
            DataObject errorObject = session.addObject((ConcreteCustomClass) retailLM.findClassByCompoundName("equipmentServerError"));
            Object equipmentServerObject = retailLM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
            retailLM.findLCPByCompoundName("equipmentServerEquipmentServerError").change(equipmentServerObject, session, errorObject);
            retailLM.findLCPByCompoundName("dataEquipmentServerError").change(exception.toString(), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(os));
            retailLM.findLCPByCompoundName("erTraceEquipmentServerError").change(os.toString(), session, errorObject);

            retailLM.findLCPByCompoundName("dateEquipmentServerError").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);

            session.apply(this);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public EquipmentServerSettings readEquipmentServerSettings(String equipmentServer) throws RemoteException, SQLException {
        try {
            DataSession session = createSession();
            Integer equipmentServerID = (Integer) retailLM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
            Integer delay = (Integer) retailLM.findLCPByCompoundName("delayEquipmentServer").read(session, new DataObject(equipmentServerID, (ConcreteClass) retailLM.findClassByCompoundName("equipmentServer")));
            return new EquipmentServerSettings(delay);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    private String dateTimeCode(Timestamp timeStamp) {
        String result = "";
        long time = timeStamp.getTime() / 1000;
        while (time > 26) {
            result = (char) (time % 26 + 97) + result;
            time = time / 26;
        }
        result = (char) (time + 97) + result;
        return result;
    }
}

