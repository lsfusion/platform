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
import platform.server.logics.linear.LP;
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

            LP isMachineryPriceTransaction = retailLM.is(retailLM.findClassByCompoundName("machineryPriceTransaction"));
            Map<Object, KeyExpr> keys = isMachineryPriceTransaction.getMapKeys();
            KeyExpr key = BaseUtils.singleValue(keys);
            Query<Object, Object> query = new Query<Object, Object>(keys);

            query.properties.put("dateTimeMPT", retailLM.findLPByCompoundName("dateTimeMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("groupMachineryMPT", retailLM.findLPByCompoundName("groupMachineryMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
            query.properties.put("snapshotMPT", retailLM.findLPByCompoundName("groupMachineryMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));

            query.and(retailLM.findLPByCompoundName("sidEquipmentServerMachineryPriceTransaction").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));
            query.and(retailLM.findLPByCompoundName("processMachineryPriceTransaction").getExpr(key).getWhere());

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
                itemQuery.properties.put("idBarcode", getLP("Barcode_idBarcode").getExpr(barcodeExpr));
                itemQuery.properties.put("name", retailLM.findLPByCompoundName("nameMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("price", retailLM.findLPByCompoundName("priceMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("daysExpiry", retailLM.findLPByCompoundName("daysExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("hoursExpiry", retailLM.findLPByCompoundName("hoursExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("expiryDate", retailLM.findLPByCompoundName("expiryDateSkuDepartmentStoreMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("labelFormat", retailLM.findLPByCompoundName("labelFormatMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("composition", retailLM.findLPByCompoundName("compositionMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("isWeight", retailLM.findLPByCompoundName("isWeightMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                itemQuery.properties.put("itemGroup", retailLM.findLPByCompoundName("itemGroupMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));

                itemQuery.and(retailLM.findLPByCompoundName("inMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr).getWhere());

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
                    String canonicalNameItemGroup = numberItemGroup == null ? "" : (String) retailLM.findLPByCompoundName("canonicalNameItemGroup").read(session, session.modifier, new DataObject(numberItemGroup, (ConcreteClass) retailLM.findClassByCompoundName("itemGroup")));

                    Integer cellScalesObject = (Integer) retailLM.findLPByCompoundName("groupScalesCompositionToCellScales").read(session, session.modifier, groupObject, new DataObject(composition, TextClass.instance));
                    Integer compositionNumberCellScales = cellScalesObject == null ? null : (Integer) retailLM.findLPByCompoundName("numberCellScales").read(session, session.modifier, new DataObject(cellScalesObject, (ConcreteClass) retailLM.findClassByCompoundName("cellScales")));

                    itemTransactionList.add(new ItemInfo(barcode.trim(), name.trim(), price, daysExpiry, hoursExpiry, expiryDate, labelFormat, composition, compositionNumberCellScales, isWeight, numberItemGroup == null ? 0 : numberItemGroup, canonicalNameItemGroup.trim()));
                }

                if (transactionObject.objectClass.equals(retailLM.findClassByCompoundName("cashRegisterPriceTransaction"))) {
                    List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();
                    LP isCashRegister = LM.is(retailLM.findClassByCompoundName("cashRegister"));

                    Map<Object, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                    KeyExpr cashRegisterKey = BaseUtils.singleValue(cashRegisterKeys);
                    Query<Object, Object> cashRegisterQuery = new Query<Object, Object>(cashRegisterKeys);

                    cashRegisterQuery.properties.put("directoryCashRegister", retailLM.findLPByCompoundName("directoryCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("portMachinery", retailLM.findLPByCompoundName("portMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("nppMachinery", retailLM.findLPByCompoundName("nppMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("numberCashRegister", retailLM.findLPByCompoundName("numberCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("nameModelMachinery", retailLM.findLPByCompoundName("nameModelMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.properties.put("handlerModelMachinery", retailLM.findLPByCompoundName("handlerModelMachinery").getExpr(cashRegisterKey));

                    cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                    cashRegisterQuery.and(retailLM.findLPByCompoundName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare(groupObject, Compare.EQUALS));
                    if (snapshotTransaction)
                        cashRegisterQuery.and(retailLM.findLPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), cashRegisterKey).getWhere());

                    OrderedMap<Map<Object, Object>, Map<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                    for (Map.Entry<Map<Object, Object>, Map<Object, Object>> values : cashRegisterResult.entrySet()) {

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
                    String directory = (String) retailLM.findLPByCompoundName("directoryGroupScales").read(session, groupObject);
                    String pieceItemCodeGroupScales = (String) retailLM.findLPByCompoundName("pieceItemCodeGroupScales").read(session, groupObject);
                    String weightItemCodeGroupScales = (String) retailLM.findLPByCompoundName("weightItemCodeGroupScales").read(session, groupObject);

                    LP isScales = LM.is(retailLM.findClassByCompoundName("scales"));

                    Map<Object, KeyExpr> scalesKeys = isScales.getMapKeys();
                    KeyExpr scalesKey = BaseUtils.singleValue(scalesKeys);
                    Query<Object, Object> scalesQuery = new Query<Object, Object>(scalesKeys);

                    scalesQuery.properties.put("portMachinery", retailLM.findLPByCompoundName("portMachinery").getExpr(scalesKey));
                    scalesQuery.properties.put("nppMachinery", retailLM.findLPByCompoundName("nppMachinery").getExpr(scalesKey));
                    scalesQuery.properties.put("nameModelMachinery", retailLM.findLPByCompoundName("nameModelMachinery").getExpr(scalesKey));
                    scalesQuery.properties.put("handlerModelMachinery", retailLM.findLPByCompoundName("handlerModelMachinery").getExpr(scalesKey));
                    scalesQuery.and(isScales.property.getExpr(scalesKeys).getWhere());
                    scalesQuery.and(retailLM.findLPByCompoundName("groupScalesScales").getExpr(scalesKey).compare(groupObject, Compare.EQUALS));
                    //if (snapshotTransaction)
                    //    scalesQuery.and(retailLM.findLPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), scalesKey).getWhere());

                    OrderedMap<Map<Object, Object>, Map<Object, Object>> scalesResult = scalesQuery.execute(session.sql);

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
                    LP isCheck = LM.is(retailLM.findClassByCompoundName("check"));

                    Map<Object, KeyExpr> checkKeys = isCheck.getMapKeys();
                    KeyExpr checkKey = BaseUtils.singleValue(checkKeys);
                    Query<Object, Object> checkQuery = new Query<Object, Object>(checkKeys);

                    checkQuery.properties.put("portMachinery", retailLM.findLPByCompoundName("portMachinery").getExpr(checkKey));
                    checkQuery.properties.put("nppMachinery", retailLM.findLPByCompoundName("nppMachinery").getExpr(checkKey));
                    checkQuery.properties.put("nameCheckModelCheck", retailLM.findLPByCompoundName("nameCheckModelCheck").getExpr(checkKey));
                    //checkQuery.properties.put("handlerCheckModelCheck", retailLM.findLPByCompoundName("handlerCheckModelCheck").getExpr(checkKey));
                    checkQuery.and(isCheck.property.getExpr(checkKeys).getWhere());
                    checkQuery.and(retailLM.findLPByCompoundName("groupPriceCheckerPriceChecker").getExpr(checkKey).compare(groupObject, Compare.EQUALS));

                    if (snapshotTransaction)
                        checkQuery.and(retailLM.findLPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), checkKey).getWhere());

                    OrderedMap<Map<Object, Object>, Map<Object, Object>> checkResult = checkQuery.execute(session.sql);

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
                    LP isTerminal = LM.is(retailLM.findClassByCompoundName("terminal"));

                    Map<Object, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                    KeyExpr terminalKey = BaseUtils.singleValue(terminalKeys);
                    Query<Object, Object> terminalQuery = new Query<Object, Object>(terminalKeys);

                    terminalQuery.properties.put("directoryTerminal", retailLM.findLPByCompoundName("directoryTerminal").getExpr(terminalKey));
                    terminalQuery.properties.put("portMachinery", retailLM.findLPByCompoundName("portMachinery").getExpr(terminalKey));
                    terminalQuery.properties.put("nppMachinery", retailLM.findLPByCompoundName("nppMachinery").getExpr(terminalKey));
                    terminalQuery.properties.put("nameModelMachinery", retailLM.findLPByCompoundName("nameModelMachinery").getExpr(terminalKey));
                    terminalQuery.properties.put("handlerModelMachinery", retailLM.findLPByCompoundName("handlerModelMachinery").getExpr(terminalKey));
                    terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                    terminalQuery.and(retailLM.findLPByCompoundName("groupTerminalTerminal").getExpr(terminalKey).compare(groupObject, Compare.EQUALS));

                    OrderedMap<Map<Object, Object>, Map<Object, Object>> terminalResult = terminalQuery.execute(session.sql);

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

            LP isGroupMachinery = retailLM.is(retailLM.findClassByCompoundName("groupMachinery"));
            Map<Object, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = BaseUtils.singleValue(keys);
            Query<Object, Object> query = new Query<Object, Object>(keys);
            query.properties.put("roundSalesGroupCashRegister", retailLM.findLPByCompoundName("roundSalesGroupCashRegister").getExpr(key));
            query.and(retailLM.findLPByCompoundName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

            OrderedMap<Map<Object, DataObject>, Map<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object[]> groupMachineryObjects = new ArrayList<Object[]>();
            for (Map.Entry<Map<Object, DataObject>, Map<Object, ObjectValue>> entry : result.entrySet()) {
                DataObject groupMachineryObject = entry.getKey().values().iterator().next();
                Integer roundSalesGroupCashRegister = (Integer) entry.getValue().get("roundSalesGroupCashRegister").getValue();
                groupMachineryObjects.add(new Object[]{groupMachineryObject, roundSalesGroupCashRegister});
            }

            for (Object[] groupMachinery : groupMachineryObjects) {
                DataObject groupMachineryObject = (DataObject) groupMachinery[0];
                Integer roundSalesGroupCashRegister = (Integer) groupMachinery[1];

                LP isCashRegister = LM.is(retailLM.findClassByCompoundName("cashRegister"));

                Map<Object, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                KeyExpr cashRegisterKey = BaseUtils.singleValue(cashRegisterKeys);
                Query<Object, Object> cashRegisterQuery = new Query<Object, Object>(cashRegisterKeys);

                cashRegisterQuery.properties.put("directoryCashRegister", retailLM.findLPByCompoundName("directoryCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("portMachinery", retailLM.findLPByCompoundName("portMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("nppMachinery", retailLM.findLPByCompoundName("nppMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("numberCashRegister", retailLM.findLPByCompoundName("numberCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("nameModelMachinery", retailLM.findLPByCompoundName("nameModelMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("handlerModelMachinery", retailLM.findLPByCompoundName("handlerModelMachinery").getExpr(cashRegisterKey));

                cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                cashRegisterQuery.and(retailLM.findLPByCompoundName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

                OrderedMap<Map<Object, Object>, Map<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                for (Map.Entry<Map<Object, Object>, Map<Object, Object>> values : cashRegisterResult.entrySet()) {

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

            LP isGroupMachinery = retailLM.is(retailLM.findClassByCompoundName("groupMachinery"));
            Map<Object, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = BaseUtils.singleValue(keys);
            Query<Object, Object> query = new Query<Object, Object>(keys);
            query.and(retailLM.findLPByCompoundName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

            OrderedMap<Map<Object, DataObject>, Map<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object> groupMachineryObjects = new ArrayList<Object>();
            for (Map.Entry<Map<Object, DataObject>, Map<Object, ObjectValue>> entry : result.entrySet()) {
                DataObject groupMachineryObject = entry.getKey().values().iterator().next();
                groupMachineryObjects.add(groupMachineryObject);
            }

            for (Object groupMachinery : groupMachineryObjects) {
                DataObject groupMachineryObject = (DataObject) groupMachinery;

                LP isTerminal = LM.is(retailLM.findClassByCompoundName("terminal"));

                Map<Object, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                KeyExpr terminalKey = BaseUtils.singleValue(terminalKeys);
                Query<Object, Object> terminalQuery = new Query<Object, Object>(terminalKeys);

                terminalQuery.properties.put("directoryTerminal", retailLM.findLPByCompoundName("directoryTerminal").getExpr(terminalKey));
                terminalQuery.properties.put("portMachinery", retailLM.findLPByCompoundName("portMachinery").getExpr(terminalKey));
                terminalQuery.properties.put("nppMachinery", retailLM.findLPByCompoundName("nppMachinery").getExpr(terminalKey));
                terminalQuery.properties.put("nameModelMachinery", retailLM.findLPByCompoundName("nameModelMachinery").getExpr(terminalKey));
                terminalQuery.properties.put("handlerModelMachinery", retailLM.findLPByCompoundName("handlerModelMachinery").getExpr(terminalKey));

                terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                terminalQuery.and(retailLM.findLPByCompoundName("groupTerminalTerminal").getExpr(terminalKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

                OrderedMap<Map<Object, Object>, Map<Object, Object>> terminalResult = terminalQuery.execute(session.sql);

                for (Map.Entry<Map<Object, Object>, Map<Object, Object>> values : terminalResult.entrySet()) {

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
            LP isSupplier = LM.is(retailLM.findClassByCompoundName("supplier"));

            Map<Object, KeyExpr> supplierKeys = isSupplier.getMapKeys();
            KeyExpr supplierKey = BaseUtils.singleValue(supplierKeys);
            Query<Object, Object> supplierQuery = new Query<Object, Object>(supplierKeys);

            supplierQuery.properties.put("name", retailLM.baseLM.name.getExpr(supplierKey));
            supplierQuery.and(isSupplier.property.getExpr(supplierKeys).getWhere());
            OrderedMap<Map<Object, Object>, Map<Object, Object>> supplierResult = supplierQuery.execute(session.sql);
            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entry : supplierResult.entrySet()) {
                String id = String.valueOf(entry.getKey().values().iterator().next());
                String name = (String) entry.getValue().get("name");
                DataObject terminalHandbookTypeObject = ((StaticCustomClass) retailLM.findClassByCompoundName("terminalHandbookType")).getDataObject("supplier");
                String type = (String) retailLM.findLPByCompoundName("idTerminalHandbookType").read(session, session.modifier, terminalHandbookTypeObject);
                legalEntityInfoList.add(new LegalEntityInfo(id, name, type));
            }

            LP isCustomer = LM.is(retailLM.findClassByCompoundName("customer"));

            Map<Object, KeyExpr> customerKeys = isCustomer.getMapKeys();
            KeyExpr customerKey = BaseUtils.singleValue(customerKeys);
            Query<Object, Object> customerQuery = new Query<Object, Object>(customerKeys);

            customerQuery.properties.put("name", retailLM.baseLM.name.getExpr(customerKey));
            customerQuery.and(isCustomer.property.getExpr(customerKeys).getWhere());
            OrderedMap<Map<Object, Object>, Map<Object, Object>> customerResult = customerQuery.execute(session.sql);
            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entry : customerResult.entrySet()) {
                String id = String.valueOf(entry.getKey().values().iterator().next());
                String name = (String) entry.getValue().get("name");
                DataObject terminalHandbookTypeObject = ((StaticCustomClass) retailLM.findClassByCompoundName("terminalHandbookType")).getDataObject("customer");
                String type = (String) retailLM.findLPByCompoundName("idTerminalHandbookType").read(session, session.modifier, terminalHandbookTypeObject);
                legalEntityInfoList.add(new LegalEntityInfo(id, name, type));
            }

            List<TerminalDocumentTypeInfo> terminalDocumentTypeInfoList = new ArrayList<TerminalDocumentTypeInfo>();
            LP isTerminalDocumentType = LM.is(retailLM.findClassByCompoundName("terminalDocumentType"));

            Map<Object, KeyExpr> terminalDocumentTypeKeys = isTerminalDocumentType.getMapKeys();
            KeyExpr terminalDocumentTypeKey = BaseUtils.singleValue(terminalDocumentTypeKeys);
            Query<Object, Object> terminalDocumentTypeQuery = new Query<Object, Object>(terminalDocumentTypeKeys);
            terminalDocumentTypeQuery.properties.put("idTerminalDocumentType", retailLM.findLPByCompoundName("idTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("nameTerminalDocumentType", retailLM.findLPByCompoundName("nameTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("nameInHandbook1TerminalDocumentType", retailLM.findLPByCompoundName("nameInHandbook1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("idTerminalHandbookType1TerminalDocumentType", retailLM.findLPByCompoundName("idTerminalHandbookType1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("nameInHandbook2TerminalDocumentType", retailLM.findLPByCompoundName("nameInHandbook2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.properties.put("idTerminalHandbookType2TerminalDocumentType", retailLM.findLPByCompoundName("idTerminalHandbookType2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.and(isTerminalDocumentType.property.getExpr(terminalDocumentTypeKeys).getWhere());

            OrderedMap<Map<Object, Object>, Map<Object, Object>> terminalDocumentTypeResult = terminalDocumentTypeQuery.execute(session.sql);

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
            ImportField cashRegisterField = new ImportField(retailLM.findLPByCompoundName("numberCashRegister"));
            ImportField zReportNumberField = new ImportField(retailLM.findLPByCompoundName("numberZReport"));

            ImportField numberBillField = new ImportField(retailLM.findLPByCompoundName("numberBill"));
            ImportField dateField = new ImportField(retailLM.findLPByCompoundName("dateBill"));
            ImportField timeField = new ImportField(retailLM.findLPByCompoundName("timeBill"));

            ImportField numberBillDetailField = new ImportField(retailLM.findLPByCompoundName("numberBillDetail"));
            ImportField idBarcodeBillDetailField = new ImportField(retailLM.findLPByCompoundName("idBarcodeBillDetail"));

            ImportField quantityBillSaleDetailField = new ImportField(retailLM.findLPByCompoundName("quantityBillSaleDetail"));
            ImportField retailPriceBillSaleDetailField = new ImportField(retailLM.findLPByCompoundName("retailPriceBillSaleDetail"));
            ImportField retailSumBillSaleDetailField = new ImportField(retailLM.findLPByCompoundName("retailSumBillSaleDetail"));
            ImportField discountSumBillSaleDetailField = new ImportField(retailLM.findLPByCompoundName("discountSumBillSaleDetail"));
            ImportField discountSumSaleBillField = new ImportField(retailLM.findLPByCompoundName("discountSumSaleBill"));

            ImportField quantityBillReturnDetailField = new ImportField(retailLM.findLPByCompoundName("quantityBillReturnDetail"));
            ImportField retailPriceBillReturnDetailField = new ImportField(retailLM.findLPByCompoundName("retailPriceBillReturnDetail"));
            ImportField retailSumBillReturnDetailField = new ImportField(retailLM.findLPByCompoundName("retailSumBillReturnDetail"));
            ImportField discountSumBillReturnDetailField = new ImportField(retailLM.findLPByCompoundName("discountSumBillReturnDetail"));
            ImportField discountSumReturnBillField = new ImportField(retailLM.findLPByCompoundName("discountSumReturnBill"));

            ImportField sidTypePaymentField = new ImportField(retailLM.findLPByCompoundName("sidPaymentType"));
            ImportField sumPaymentField = new ImportField(retailLM.findLPByCompoundName("sumPayment"));
            ImportField numberPaymentField = new ImportField(retailLM.findLPByCompoundName("numberPayment"));

            List<ImportProperty<?>> saleProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> returnProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> paymentProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> zReportKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("zReportPosted"), retailLM.findLPByCompoundName("numberNumberCashRegisterToZReportPosted").getMapping(zReportNumberField, cashRegisterField));
            ImportKey<?> cashRegisterKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("cashRegister"), retailLM.findLPByCompoundName("cashRegisterNumber").getMapping(cashRegisterField));
            ImportKey<?> billKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("bill"), retailLM.findLPByCompoundName("zReportBillToBill").getMapping(zReportNumberField, numberBillField, cashRegisterField));
            ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("item"), getLP("Barcode_skuBarcodeIdDate").getMapping(idBarcodeBillDetailField, dateField));

            saleProperties.add(new ImportProperty(zReportNumberField, retailLM.findLPByCompoundName("numberZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(cashRegisterField, retailLM.findLPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("cashRegister")).getMapping(cashRegisterKey)));
            saleProperties.add(new ImportProperty(dateField, retailLM.findLPByCompoundName("dateZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(timeField, retailLM.findLPByCompoundName("timeZReport").getMapping(zReportKey)));

            saleProperties.add(new ImportProperty(numberBillField, retailLM.findLPByCompoundName("numberBill").getMapping(billKey)));
            saleProperties.add(new ImportProperty(dateField, retailLM.findLPByCompoundName("dateBill").getMapping(billKey)));
            saleProperties.add(new ImportProperty(timeField, retailLM.findLPByCompoundName("timeBill").getMapping(billKey)));
            saleProperties.add(new ImportProperty(discountSumSaleBillField, retailLM.findLPByCompoundName("discountSumSaleBill").getMapping(billKey)));
            saleProperties.add(new ImportProperty(zReportNumberField, retailLM.findLPByCompoundName("zReportBill").getMapping(billKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("zReport")).getMapping(zReportKey)));

            ImportKey<?> billSaleDetailKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("billSaleDetail"), retailLM.findLPByCompoundName("zReportBillBillDetailToBillDetail").getMapping(zReportNumberField, numberBillField, numberBillDetailField, cashRegisterField));
            saleProperties.add(new ImportProperty(numberBillDetailField, retailLM.findLPByCompoundName("numberBillDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(idBarcodeBillDetailField, retailLM.findLPByCompoundName("idBarcodeBillDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(quantityBillSaleDetailField, retailLM.findLPByCompoundName("quantityBillSaleDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(retailPriceBillSaleDetailField, retailLM.findLPByCompoundName("retailPriceBillSaleDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(retailSumBillSaleDetailField, retailLM.findLPByCompoundName("retailSumBillSaleDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(discountSumBillSaleDetailField, retailLM.findLPByCompoundName("discountSumBillSaleDetail").getMapping(billSaleDetailKey)));
            saleProperties.add(new ImportProperty(numberBillField, retailLM.findLPByCompoundName("billBillDetail").getMapping(billSaleDetailKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("bill")).getMapping(billKey)));

            saleProperties.add(new ImportProperty(idBarcodeBillDetailField, retailLM.findLPByCompoundName("itemBillSaleDetail").getMapping(billSaleDetailKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("item")).getMapping(itemKey)));


            returnProperties.add(new ImportProperty(zReportNumberField, retailLM.findLPByCompoundName("numberZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(cashRegisterField, retailLM.findLPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("cashRegister")).getMapping(cashRegisterKey)));
            returnProperties.add(new ImportProperty(dateField, retailLM.findLPByCompoundName("dateZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(timeField, retailLM.findLPByCompoundName("timeZReport").getMapping(zReportKey)));

            returnProperties.add(new ImportProperty(numberBillField, retailLM.findLPByCompoundName("numberBill").getMapping(billKey)));
            returnProperties.add(new ImportProperty(dateField, retailLM.findLPByCompoundName("dateBill").getMapping(billKey)));
            returnProperties.add(new ImportProperty(timeField, retailLM.findLPByCompoundName("timeBill").getMapping(billKey)));
            returnProperties.add(new ImportProperty(discountSumReturnBillField, retailLM.findLPByCompoundName("discountSumReturnBill").getMapping(billKey)));
            returnProperties.add(new ImportProperty(zReportNumberField, retailLM.findLPByCompoundName("zReportBill").getMapping(billKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("zReport")).getMapping(zReportKey)));

            ImportKey<?> billReturnDetailKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("billReturnDetail"), retailLM.findLPByCompoundName("zReportBillBillDetailToBillDetail").getMapping(zReportNumberField, numberBillField, numberBillDetailField, cashRegisterField));
            returnProperties.add(new ImportProperty(numberBillDetailField, retailLM.findLPByCompoundName("numberBillDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(idBarcodeBillDetailField, retailLM.findLPByCompoundName("idBarcodeBillDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(quantityBillReturnDetailField, retailLM.findLPByCompoundName("quantityBillReturnDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(retailPriceBillReturnDetailField, retailLM.findLPByCompoundName("retailPriceBillReturnDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(retailSumBillReturnDetailField, retailLM.findLPByCompoundName("retailSumBillReturnDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(discountSumBillReturnDetailField, retailLM.findLPByCompoundName("discountSumBillReturnDetail").getMapping(billReturnDetailKey)));
            returnProperties.add(new ImportProperty(numberBillField, retailLM.findLPByCompoundName("billBillDetail").getMapping(billReturnDetailKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("bill")).getMapping(billKey)));

            returnProperties.add(new ImportProperty(idBarcodeBillDetailField, retailLM.findLPByCompoundName("itemBillReturnDetail").getMapping(billReturnDetailKey),
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

            ImportKey<?> paymentKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("payment"), retailLM.findLPByCompoundName("zReportBillPaymentToPayment").getMapping(zReportNumberField, numberBillField, numberPaymentField, cashRegisterField));
            ImportKey<?> paymentTypeKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("paymentType"), retailLM.findLPByCompoundName("sidToTypePayment").getMapping(sidTypePaymentField));
            paymentProperties.add(new ImportProperty(sumPaymentField, retailLM.findLPByCompoundName("sumPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(numberPaymentField, retailLM.findLPByCompoundName("numberPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(sidTypePaymentField, retailLM.findLPByCompoundName("paymentTypePayment").getMapping(paymentKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("paymentType")).getMapping(paymentTypeKey)));
            paymentProperties.add(new ImportProperty(numberBillField, retailLM.findLPByCompoundName("billPayment").getMapping(paymentKey),
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

                DataObject logObject = session.addObject((ConcreteCustomClass) retailLM.findClassByCompoundName("equipmentServerLog"), session.modifier);
                Object equipmentServerObject = retailLM.findLPByCompoundName("sidToEquipmentServer").read(session, session.modifier, new DataObject(equipmentServer, StringClass.get(20)));
                retailLM.findLPByCompoundName("equipmentServerEquipmentServerLog").execute(equipmentServerObject, session, logObject);
                retailLM.findLPByCompoundName("dataEquipmentServerLog").execute(message, session, logObject);
                retailLM.findLPByCompoundName("dateEquipmentServerLog").execute(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            new IntegrationService(session, new ImportTable(paymentImportFields, dataPayment), Arrays.asList(paymentKey, paymentTypeKey, billKey, cashRegisterKey),
                    paymentProperties).synchronize(true);

            return session.apply(this);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public String sendTerminalDocumentInfo(List<TerminalDocumentInfo> terminalDocumentInfoList, String equServerID) throws IOException, SQLException {
        try {
            DataSession session = createSession();
            ImportField idTerminalDocumentField = new ImportField(retailLM.findLPByCompoundName("idTerminalDocument"));
            ImportField typeTerminalDocumentField = new ImportField(retailLM.findLPByCompoundName("idTerminalDocumentTypeTerminalDocument"));
            ImportField idTerminalHandbookType1TerminalDocumentField = new ImportField(retailLM.findLPByCompoundName("idTerminalHandbookType1TerminalDocument"));
            ImportField idTerminalHandbookType2TerminalDocumentField = new ImportField(retailLM.findLPByCompoundName("idTerminalHandbookType2TerminalDocument"));
            ImportField titleTerminalDocumentField = new ImportField(retailLM.findLPByCompoundName("titleTerminalDocument"));
            ImportField quantityTerminalDocumentField = new ImportField(retailLM.findLPByCompoundName("quantityTerminalDocument"));

            ImportField numberTerminalDocumentDetailField = new ImportField(retailLM.findLPByCompoundName("numberTerminalDocumentDetail"));
            ImportField barcodeTerminalDocumentDetailField = new ImportField(retailLM.findLPByCompoundName("barcodeTerminalDocumentDetail"));
            ImportField nameTerminalDocumentDetailField = new ImportField(retailLM.findLPByCompoundName("nameTerminalDocumentDetail"));
            ImportField quantityTerminalDocumentDetailField = new ImportField(retailLM.findLPByCompoundName("quantityTerminalDocumentDetail"));
            ImportField priceTerminalDocumentDetailField = new ImportField(retailLM.findLPByCompoundName("priceTerminalDocumentDetail"));
            ImportField sumTerminalDocumentDetailField = new ImportField(retailLM.findLPByCompoundName("sumTerminalDocumentDetail"));

            ImportField isNewTerminalDocumentDetailField = new ImportField(retailLM.findLPByCompoundName("isNewTerminalDocumentDetail"));

            List<ImportProperty<?>> terminalDocumentProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> terminalDocumentDetailProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> terminalDocumentKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("terminalDocument"), retailLM.findLPByCompoundName("terminalDocumentID").getMapping(idTerminalDocumentField));

            terminalDocumentProperties.add(new ImportProperty(idTerminalDocumentField, retailLM.findLPByCompoundName("idTerminalDocument").getMapping(terminalDocumentKey)));
            //terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, retailLM.findLPByCompoundName("typeTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(titleTerminalDocumentField, retailLM.findLPByCompoundName("titleTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType1TerminalDocumentField, retailLM.findLPByCompoundName("idTerminalHandbookType1TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType2TerminalDocumentField, retailLM.findLPByCompoundName("idTerminalHandbookType2TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(quantityTerminalDocumentField, retailLM.findLPByCompoundName("quantityTerminalDocument").getMapping(terminalDocumentKey)));

            ImportKey<?> terminalDocumentDetailKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("terminalDocumentDetail"), retailLM.findLPByCompoundName("terminalDocumentDetailIDDocumentIDDetail").getMapping(idTerminalDocumentField, numberTerminalDocumentDetailField));

            terminalDocumentDetailProperties.add(new ImportProperty(numberTerminalDocumentDetailField, retailLM.findLPByCompoundName("numberTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(barcodeTerminalDocumentDetailField, retailLM.findLPByCompoundName("barcodeTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(nameTerminalDocumentDetailField, retailLM.findLPByCompoundName("nameTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(quantityTerminalDocumentDetailField, retailLM.findLPByCompoundName("quantityTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(priceTerminalDocumentDetailField, retailLM.findLPByCompoundName("priceTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(sumTerminalDocumentDetailField, retailLM.findLPByCompoundName("sumTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(isNewTerminalDocumentDetailField, retailLM.findLPByCompoundName("isNewTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(idTerminalDocumentField, retailLM.findLPByCompoundName("terminalDocumentTerminalDocumentDetail").getMapping(terminalDocumentDetailKey),
                    LM.baseLM.object(retailLM.findClassByCompoundName("terminalDocument")).getMapping(terminalDocumentKey)));


            ImportKey<?> terminalDocumentTypeKey = new ImportKey((ConcreteCustomClass) retailLM.findClassByCompoundName("terminalDocumentType"), retailLM.findLPByCompoundName("terminalDocumentTypeID").getMapping(typeTerminalDocumentField));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, retailLM.findLPByCompoundName("idTerminalDocumentType").getMapping(terminalDocumentTypeKey)));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, retailLM.findLPByCompoundName("terminalDocumentTypeTerminalDocument").getMapping(terminalDocumentKey),
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

                DataObject logObject = session.addObject((ConcreteCustomClass) retailLM.findClassByCompoundName("equipmentServerLog"), session.modifier);
                Object equipmentServerObject = retailLM.findLPByCompoundName("sidToEquipmentServer").read(session, session.modifier, new DataObject(equServerID, StringClass.get(20)));
                retailLM.findLPByCompoundName("equipmentServerEquipmentServerLog").execute(equipmentServerObject, session, logObject);
                retailLM.findLPByCompoundName("dataEquipmentServerLog").execute(message, session, logObject);
                retailLM.findLPByCompoundName("dateEquipmentServerLog").execute(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            return session.apply(this);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public void succeedTransaction(Integer transactionID) throws RemoteException, SQLException {
        try {
            DataSession session = createSession();
            retailLM.findLPByCompoundName("succeededMachineryPriceTransaction").execute(true, session,
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

                DataObject scalesModelObject = new DataObject(retailLM.findLPByCompoundName("scalesModelName").read(session, session.modifier, new DataObject(scalesModel)), (ConcreteClass) retailLM.findClassByCompoundName("scalesModel"));

                LP isLabelFormat = LM.is(retailLM.findClassByCompoundName("labelFormat"));

                Map<Object, KeyExpr> labelFormatKeys = isLabelFormat.getMapKeys();
                KeyExpr labelFormatKey = BaseUtils.singleValue(labelFormatKeys);
                Query<Object, Object> labelFormatQuery = new Query<Object, Object>(labelFormatKeys);

                labelFormatQuery.properties.put("fileLabelFormat", retailLM.findLPByCompoundName("fileLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.properties.put("fileMessageLabelFormat", retailLM.findLPByCompoundName("fileMessageLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.and(isLabelFormat.property.getExpr(labelFormatKeys).getWhere());
                labelFormatQuery.and(retailLM.findLPByCompoundName("scalesModelLabelFormat").getExpr(labelFormatKey).compare((scalesModelObject).getExpr(), Compare.EQUALS));

                OrderedMap<Map<Object, Object>, Map<Object, Object>> labelFormatResult = labelFormatQuery.execute(session.sql);

                for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entry : labelFormatResult.entrySet()) {
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
            DataObject errorObject = session.addObject((ConcreteCustomClass) retailLM.findClassByCompoundName("machineryPriceTransactionError"), session.modifier);
            retailLM.findLPByCompoundName("machineryPriceTransactionMachineryPriceTransactionError").execute(transactionID, session, errorObject);
            retailLM.findLPByCompoundName("dataMachineryPriceTransactionError").execute(e.toString(), session, errorObject);
            retailLM.findLPByCompoundName("dateMachineryPriceTransactionError").execute(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(os));
            retailLM.findLPByCompoundName("errorTraceMachineryPriceTransactionError").execute(os.toString(), session, errorObject);

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
            DataObject errorObject = session.addObject((ConcreteCustomClass) retailLM.findClassByCompoundName("equipmentServerError"), session.modifier);
            Object equipmentServerObject = retailLM.findLPByCompoundName("sidToEquipmentServer").read(session, session.modifier, new DataObject(equipmentServer, StringClass.get(20)));
            retailLM.findLPByCompoundName("equipmentServerEquipmentServerError").execute(equipmentServerObject, session, errorObject);
            retailLM.findLPByCompoundName("dataEquipmentServerError").execute(exception.toString(), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(os));
            retailLM.findLPByCompoundName("erTraceEquipmentServerError").execute(os.toString(), session, errorObject);

            retailLM.findLPByCompoundName("dateEquipmentServerError").execute(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);

            session.apply(this);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RemoteException(e.toString());
        }
    }

    @Override
    public EquipmentServerSettings readEquipmentServerSettings(String equipmentServer) throws RemoteException, SQLException {
        try {
            DataSession session = createSession();
            Integer equipmentServerID = (Integer) retailLM.findLPByCompoundName("sidToEquipmentServer").read(session, session.modifier, new DataObject(equipmentServer, StringClass.get(20)));
            Integer delay = (Integer) retailLM.findLPByCompoundName("delayEquipmentServer").read(session, session.modifier, new DataObject(equipmentServerID, (ConcreteClass) retailLM.findClassByCompoundName("equipmentServer")));
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

