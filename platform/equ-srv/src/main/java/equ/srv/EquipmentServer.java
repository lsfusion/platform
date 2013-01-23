package equ.srv;

import equ.api.*;
import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.OrderedMap;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.classes.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
import platform.server.integration.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.BusinessLogicsBootstrap;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class EquipmentServer extends UnicastRemoteObject implements EquipmentServerInterface {
    BusinessLogics BL;
    ScriptingLogicsModule LM;

    public EquipmentServer(ScriptingLogicsModule LM) throws RemoteException {
        super(LM.getBL().getExportPort());
        this.BL = LM.getBL();
        this.LM = LM;
        String dbName = BL.getDbName() == null ? "default" : BL.getDbName();
        try {
            BusinessLogicsBootstrap.registry.bind(dbName + "/EquipmentServer", this);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (AlreadyBoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<TransactionInfo> readTransactionInfo(String equServerID) throws RemoteException, SQLException {
        try {

            DataSession session = BL.createSession();
            List<TransactionInfo> transactionList = new ArrayList<TransactionInfo>();

            LCP isMachineryPriceTransaction = LM.is(LM.findClassByCompoundName("machineryPriceTransaction"));
            ImRevMap<Object, KeyExpr> keys = isMachineryPriceTransaction.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);

            query.addProperty("dateTimeMPT", LM.findLCPByCompoundName("dateTimeMachineryPriceTransaction").getExpr(key));
            query.addProperty("groupMachineryMPT", LM.findLCPByCompoundName("groupMachineryMachineryPriceTransaction").getExpr(key));
            query.addProperty("snapshotMPT", LM.findLCPByCompoundName("groupMachineryMachineryPriceTransaction").getExpr(key));

            query.and(LM.findLCPByCompoundName("sidEquipmentServerMachineryPriceTransaction").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));
            query.and(LM.findLCPByCompoundName("processMachineryPriceTransaction").getExpr(key).getWhere());

            ImOrderMap<ImMap<Object, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object[]> transactionObjects = new ArrayList<Object[]>();
            for (int i=0,size=result.size();i<size;i++) {
                ImMap<Object, ObjectValue> value = result.getValue(i);
                DataObject dateTimeMPT = (DataObject) value.get("dateTimeMPT");
                DataObject groupMachineryMPT = (DataObject) value.get("groupMachineryMPT");
                DataObject transactionObject = result.getKey(i).singleValue();
                Boolean snapshotMPT = value.get("snapshotMPT") instanceof DataObject;
                transactionObjects.add(new Object[]{groupMachineryMPT, transactionObject, dateTimeCode((Timestamp) dateTimeMPT.getValue()), dateTimeMPT, snapshotMPT});
            }

            List<ItemInfo> skuTransactionList;
            for (Object[] transaction : transactionObjects) {

                DataObject groupObject = (DataObject) transaction[0];
                DataObject transactionObject = (DataObject) transaction[1];
                String dateTimeCode = (String) transaction[2];
                Date date = new Date(((Timestamp) ((DataObject) transaction[3]).getValue()).getTime());
                Boolean snapshotTransaction = (Boolean) transaction[4];

                skuTransactionList = new ArrayList<ItemInfo>();
                KeyExpr barcodeExpr = new KeyExpr("barcode");
                ImRevMap<Object, KeyExpr> skuKeys = MapFact.singletonRev((Object)"barcode", barcodeExpr);

                QueryBuilder<Object, Object> skuQuery = new QueryBuilder<Object, Object>(skuKeys);
                skuQuery.addProperty("idBarcode", LM.findLCPByCompoundName("idBarcode").getExpr(barcodeExpr));
                skuQuery.addProperty("name", LM.findLCPByCompoundName("nameMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("price", LM.findLCPByCompoundName("priceMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("daysExpiry", LM.findLCPByCompoundName("daysExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("hoursExpiry", LM.findLCPByCompoundName("hoursExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("expiryDate", LM.findLCPByCompoundName("expiryDataMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("labelFormat", LM.findLCPByCompoundName("labelFormatMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("composition", LM.findLCPByCompoundName("compositionMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("isWeight", LM.findLCPByCompoundName("isWeightMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("skuGroup", LM.findLCPByCompoundName("skuGroupMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));

                skuQuery.and(LM.findLCPByCompoundName("inMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr).getWhere());

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> skuResult = skuQuery.execute(session.sql);

                for (ImMap<Object, Object> row : skuResult.valueIt()) {
                    String barcode = (String) row.get("idBarcode");
                    String name = (String) row.get("name");
                    Double price = (Double) row.get("price");
                    Double daysExpiry = (Double) row.get("daysExpiry");
                    Integer hoursExpiry = (Integer) row.get("hoursExpiry");
                    Date expiryDate = (Date) row.get("expiryDate");
                    Integer labelFormat = (Integer) row.get("labelFormat");
                    String composition = (String) row.get("composition");
                    Boolean isWeight = row.get("isWeight") != null;
                    Integer numberSkuGroup = (Integer) row.get("skuGroup");
                    String canonicalNameSkuGroup = numberSkuGroup == null ? "" : (String) LM.findLCPByCompoundName("canonicalNameSkuGroup").read(session, new DataObject(numberSkuGroup, (ConcreteClass) LM.findClassByCompoundName("skuGroup")));

                    Integer cellScalesObject = composition == null ? null : (Integer) LM.findLCPByCompoundName("cellScalesGroupScalesComposition").read(session, groupObject, new DataObject(composition, TextClass.instance));
                    Integer compositionNumberCellScales = cellScalesObject == null ? null : (Integer) LM.findLCPByCompoundName("numberCellScales").read(session, new DataObject(cellScalesObject, (ConcreteClass) LM.findClassByCompoundName("cellScales")));

                    skuTransactionList.add(new ItemInfo(barcode.trim(), name.trim(), price, daysExpiry, hoursExpiry, expiryDate, labelFormat, composition, compositionNumberCellScales, isWeight, numberSkuGroup == null ? 0 : numberSkuGroup, canonicalNameSkuGroup.trim()));
                }

                if (transactionObject.objectClass.equals(LM.findClassByCompoundName("cashRegisterPriceTransaction"))) {
                    List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();
                    LCP<PropertyInterface> isCashRegister = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("cashRegister"));

                    ImRevMap<PropertyInterface, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                    KeyExpr cashRegisterKey = cashRegisterKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> cashRegisterQuery = new QueryBuilder<PropertyInterface, Object>(cashRegisterKeys);

                    cashRegisterQuery.addProperty("directoryCashRegister", LM.findLCPByCompoundName("directoryCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("portMachinery", LM.findLCPByCompoundName("portMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("nppMachinery", LM.findLCPByCompoundName("nppMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("numberCashRegister", LM.findLCPByCompoundName("numberCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("nameModelMachinery", LM.findLCPByCompoundName("nameModelMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("handlerModelMachinery", LM.findLCPByCompoundName("handlerModelMachinery").getExpr(cashRegisterKey));

                    cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                    cashRegisterQuery.and(LM.findLCPByCompoundName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare(groupObject, Compare.EQUALS));
                    if (snapshotTransaction)
                        cashRegisterQuery.and(LM.findLCPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), cashRegisterKey).getWhere());

                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                    for (ImMap<Object, Object> row : cashRegisterResult.valueIt()) {
                        String directoryCashRegister = (String) row.get("directoryCashRegister");
                        String portMachinery = (String) row.get("portMachinery");
                        Integer nppMachinery = (Integer) row.get("nppMachinery");
                        String numberCashRegister = (String) row.get("numberCashRegister");
                        String nameModel = (String) row.get("nameModelMachinery");
                        String handlerModel = (String) row.get("handlerModelMachinery");
                        cashRegisterInfoList.add(new CashRegisterInfo(nppMachinery, numberCashRegister, nameModel, handlerModel, portMachinery, directoryCashRegister, null));
                    }

                    transactionList.add(new TransactionCashRegisterInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, date, skuTransactionList, cashRegisterInfoList));

                } else if (transactionObject.objectClass.equals(LM.findClassByCompoundName("scalesPriceTransaction"))) {
                    List<ScalesInfo> scalesInfoList = new ArrayList<ScalesInfo>();
                    String directory = (String) LM.findLCPByCompoundName("directoryGroupScales").read(session, groupObject);
                    String pieceCodeGroupScales = (String) LM.findLCPByCompoundName("pieceCodeGroupScales").read(session, groupObject);
                    String weightCodeGroupScales = (String) LM.findLCPByCompoundName("weightCodeGroupScales").read(session, groupObject);

                    LCP<PropertyInterface> isScales = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("scales"));

                    ImRevMap<PropertyInterface, KeyExpr> scalesKeys = isScales.getMapKeys();
                    KeyExpr scalesKey = scalesKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> scalesQuery = new QueryBuilder<PropertyInterface, Object>(scalesKeys);

                    scalesQuery.addProperty("portMachinery", LM.findLCPByCompoundName("portMachinery").getExpr(scalesKey));
                    scalesQuery.addProperty("nppMachinery", LM.findLCPByCompoundName("nppMachinery").getExpr(scalesKey));
                    scalesQuery.addProperty("nameModelMachinery", LM.findLCPByCompoundName("nameModelMachinery").getExpr(scalesKey));
                    scalesQuery.addProperty("handlerModelMachinery", LM.findLCPByCompoundName("handlerModelMachinery").getExpr(scalesKey));
                    scalesQuery.and(isScales.property.getExpr(scalesKeys).getWhere());
                    scalesQuery.and(LM.findLCPByCompoundName("groupScalesScales").getExpr(scalesKey).compare(groupObject, Compare.EQUALS));
                    //if (snapshotTransaction)
                    //    scalesQuery.and(LM.findLCPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), scalesKey).getWhere());

                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> scalesResult = scalesQuery.execute(session.sql);

                    for (ImMap<Object, Object> values : scalesResult.valueIt()) {
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        String nameModel = (String) values.get("nameModelMachinery");
                        String handlerModel = (String) values.get("handlerModelMachinery");
                        scalesInfoList.add(new ScalesInfo(nppMachinery, nameModel, handlerModel, portMachinery, directory,
                                pieceCodeGroupScales, weightCodeGroupScales));
                    }

                    transactionList.add(new TransactionScalesInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, skuTransactionList, scalesInfoList, snapshotTransaction));

                } else if (transactionObject.objectClass.equals(LM.findClassByCompoundName("priceCheckerPriceTransaction"))) {
                    List<PriceCheckerInfo> priceCheckerInfoList = new ArrayList<PriceCheckerInfo>();
                    LCP<PropertyInterface> isCheck = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("check"));

                    ImRevMap<PropertyInterface, KeyExpr> checkKeys = isCheck.getMapKeys();
                    KeyExpr checkKey = checkKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> checkQuery = new QueryBuilder<PropertyInterface, Object>(checkKeys);

                    checkQuery.addProperty("portMachinery", LM.findLCPByCompoundName("portMachinery").getExpr(checkKey));
                    checkQuery.addProperty("nppMachinery", LM.findLCPByCompoundName("nppMachinery").getExpr(checkKey));
                    checkQuery.addProperty("nameCheckModelCheck", LM.findLCPByCompoundName("nameCheckModelCheck").getExpr(checkKey));
                    //checkQuery.addProperty("handlerCheckModelCheck", LM.findLCPByCompoundName("handlerCheckModelCheck").getExpr(checkKey));
                    checkQuery.and(isCheck.property.getExpr(checkKeys).getWhere());
                    checkQuery.and(LM.findLCPByCompoundName("groupPriceCheckerPriceChecker").getExpr(checkKey).compare(groupObject, Compare.EQUALS));

                    if (snapshotTransaction)
                        checkQuery.and(LM.findLCPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), checkKey).getWhere());

                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> checkResult = checkQuery.execute(session.sql);

                    for (ImMap<Object, Object> values : checkResult.valueIt()) {
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        String nameModel = (String) values.get("nameCheckModelCheck");
                        //String handlerModel = (String) values.get("handlerCheckModelCheck");
                        String handlerModel = null;
                        priceCheckerInfoList.add(new PriceCheckerInfo(nppMachinery, nameModel, handlerModel, portMachinery));
                    }
                    transactionList.add(new TransactionPriceCheckerInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, skuTransactionList, priceCheckerInfoList));


                } else if (transactionObject.objectClass.equals(LM.findClassByCompoundName("terminalPriceTransaction"))) {
                    List<TerminalInfo> terminalInfoList = new ArrayList<TerminalInfo>();
                    LCP<PropertyInterface> isTerminal = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("terminal"));

                    ImRevMap<PropertyInterface, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                    KeyExpr terminalKey = terminalKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> terminalQuery = new QueryBuilder<PropertyInterface, Object>(terminalKeys);

                    terminalQuery.addProperty("directoryTerminal", LM.findLCPByCompoundName("directoryTerminal").getExpr(terminalKey));
                    terminalQuery.addProperty("portMachinery", LM.findLCPByCompoundName("portMachinery").getExpr(terminalKey));
                    terminalQuery.addProperty("nppMachinery", LM.findLCPByCompoundName("nppMachinery").getExpr(terminalKey));
                    terminalQuery.addProperty("nameModelMachinery", LM.findLCPByCompoundName("nameModelMachinery").getExpr(terminalKey));
                    terminalQuery.addProperty("handlerModelMachinery", LM.findLCPByCompoundName("handlerModelMachinery").getExpr(terminalKey));
                    terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                    terminalQuery.and(LM.findLCPByCompoundName("groupTerminalTerminal").getExpr(terminalKey).compare(groupObject, Compare.EQUALS));

                    ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> terminalResult = terminalQuery.execute(session.sql);

                    for (ImMap<Object, Object> values : terminalResult.valueIt()) {
                        String directory = (String) values.get("directoryTerminal");
                        String portMachinery = (String) values.get("portMachinery");
                        Integer nppMachinery = (Integer) values.get("nppMachinery");
                        String nameModel = (String) values.get("nameModelMachinery");
                        String handlerModel = (String) values.get("handlerModelMachinery");
                        terminalInfoList.add(new TerminalInfo(directory, nppMachinery, nameModel, handlerModel, portMachinery));
                    }
                    transactionList.add(new TransactionTerminalInfo((Integer) transactionObject.getValue(),
                            dateTimeCode, skuTransactionList, terminalInfoList, snapshotTransaction));
                }
            }
            return transactionList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public List<CashRegisterInfo> readCashRegisterInfo(String equServerID) throws RemoteException, SQLException {
        try {
            DataSession session = BL.createSession();
            List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();

            LCP<PropertyInterface> isGroupMachinery = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("groupMachinery"));
            ImRevMap<PropertyInterface, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.addProperty("roundSalesGroupCashRegister", LM.findLCPByCompoundName("roundSalesGroupCashRegister").getExpr(key));
            query.and(LM.findLCPByCompoundName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

            ImOrderMap<ImMap<PropertyInterface, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object[]> groupMachineryObjects = new ArrayList<Object[]>();
            for (int i=0,size=result.size();i<size;i++) {
                DataObject groupMachineryObject = result.getKey(i).getValue(0);
                Integer roundSalesGroupCashRegister = (Integer) result.getValue(i).get("roundSalesGroupCashRegister").getValue();
                groupMachineryObjects.add(new Object[]{groupMachineryObject, roundSalesGroupCashRegister});
            }

            for (Object[] groupMachinery : groupMachineryObjects) {
                DataObject groupMachineryObject = (DataObject) groupMachinery[0];
                Integer roundSalesGroupCashRegister = (Integer) groupMachinery[1];

                LCP<PropertyInterface> isCashRegister = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("cashRegister"));

                ImRevMap<PropertyInterface, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                KeyExpr cashRegisterKey = cashRegisterKeys.singleValue();
                QueryBuilder<PropertyInterface, Object> cashRegisterQuery = new QueryBuilder<PropertyInterface, Object>(cashRegisterKeys);

                cashRegisterQuery.addProperty("directoryCashRegister", LM.findLCPByCompoundName("directoryCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("portMachinery", LM.findLCPByCompoundName("portMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("nppMachinery", LM.findLCPByCompoundName("nppMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("numberCashRegister", LM.findLCPByCompoundName("numberCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("nameModelMachinery", LM.findLCPByCompoundName("nameModelMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("handlerModelMachinery", LM.findLCPByCompoundName("handlerModelMachinery").getExpr(cashRegisterKey));

                cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                cashRegisterQuery.and(LM.findLCPByCompoundName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                for (ImMap<Object, Object> row : cashRegisterResult.values()) {
                    String directoryCashRegister = (String) row.get("directoryCashRegister");
                    String portMachinery = (String) row.get("portMachinery");
                    Integer nppMachinery = (Integer) row.get("nppMachinery");
                    String numberCashRegister = (String) row.get("numberCashRegister");
                    String nameModel = (String) row.get("nameModelMachinery");
                    String handlerModel = (String) row.get("handlerModelMachinery");
                    cashRegisterInfoList.add(new CashRegisterInfo(nppMachinery, numberCashRegister, nameModel, handlerModel, portMachinery, directoryCashRegister, roundSalesGroupCashRegister));
                }
            }
            return cashRegisterInfoList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public List<TerminalInfo> readTerminalInfo(String equServerID) throws RemoteException, SQLException {
        try {
            DataSession session = BL.createSession();
            List<TerminalInfo> terminalInfoList = new ArrayList<TerminalInfo>();

            LCP<PropertyInterface> isGroupMachinery = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("groupMachinery"));
            ImRevMap<PropertyInterface, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.and(LM.findLCPByCompoundName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

            ImOrderMap<ImMap<PropertyInterface, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object> groupMachineryObjects = new ArrayList<Object>();
            for (ImMap<PropertyInterface, DataObject> entry : result.keyIt()) {
                DataObject groupMachineryObject = entry.getValue(0);
                groupMachineryObjects.add(groupMachineryObject);
            }

            for (Object groupMachinery : groupMachineryObjects) {
                DataObject groupMachineryObject = (DataObject) groupMachinery;

                LCP<PropertyInterface> isTerminal = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("terminal"));

                ImRevMap<PropertyInterface, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                KeyExpr terminalKey = terminalKeys.singleValue();
                QueryBuilder<PropertyInterface, Object> terminalQuery = new QueryBuilder<PropertyInterface, Object>(terminalKeys);

                terminalQuery.addProperty("directoryTerminal", LM.findLCPByCompoundName("directoryTerminal").getExpr(terminalKey));
                terminalQuery.addProperty("portMachinery", LM.findLCPByCompoundName("portMachinery").getExpr(terminalKey));
                terminalQuery.addProperty("nppMachinery", LM.findLCPByCompoundName("nppMachinery").getExpr(terminalKey));
                terminalQuery.addProperty("nameModelMachinery", LM.findLCPByCompoundName("nameModelMachinery").getExpr(terminalKey));
                terminalQuery.addProperty("handlerModelMachinery", LM.findLCPByCompoundName("handlerModelMachinery").getExpr(terminalKey));

                terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                terminalQuery.and(LM.findLCPByCompoundName("groupTerminalTerminal").getExpr(terminalKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> terminalResult = terminalQuery.execute(session.sql);

                for (ImMap<Object, Object> row : terminalResult.valueIt()) {
                    String directoryTerminal = (String) row.get("directoryTerminal");
                    String portMachinery = (String) row.get("portMachinery");
                    Integer nppMachinery = (Integer) row.get("nppMachinery");
                    String nameModel = (String) row.get("nameModelMachinery");
                    String handlerModel = (String) row.get("handlerModelMachinery");
                    terminalInfoList.add(new TerminalInfo(directoryTerminal, nppMachinery, nameModel, handlerModel, portMachinery));
                }
            }
            return terminalInfoList;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public List<TerminalDocumentTypeInfo> readTerminalDocumentTypeInfo() throws RemoteException, SQLException {
        try {
            DataSession session = BL.createSession();

            List<LegalEntityInfo> legalEntityInfoList = new ArrayList<LegalEntityInfo>();

            LCP<PropertyInterface> isLegalEntity = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("legalEntity"));

            ImRevMap<PropertyInterface, KeyExpr> legalEntityKeys = isLegalEntity.getMapKeys();
            KeyExpr legalEntityKey = legalEntityKeys.singleValue();
            QueryBuilder<PropertyInterface, Object> legalEntityQuery = new QueryBuilder<PropertyInterface, Object>(legalEntityKeys);

            legalEntityQuery.addProperty("name", LM.baseLM.name.getExpr(legalEntityKey));
            legalEntityQuery.and(isLegalEntity.property.getExpr(legalEntityKeys).getWhere());
            ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> legalEntityResult = legalEntityQuery.execute(session.sql);
            for (int i=0,size=legalEntityResult.size();i<size;i++) {
                String id = String.valueOf(legalEntityResult.getKey(i).getValue(0));
                String name = (String) legalEntityResult.getValue(i).get("name");
                DataObject terminalHandbookTypeObject = ((StaticCustomClass) LM.findClassByCompoundName("terminalHandbookType")).getDataObject("terminalHandbookTypeLegalEntity");
                String type = (String) LM.findLCPByCompoundName("idTerminalHandbookType").read(session, terminalHandbookTypeObject);
                legalEntityInfoList.add(new LegalEntityInfo(id, name, type));
            }

            List<TerminalDocumentTypeInfo> terminalDocumentTypeInfoList = new ArrayList<TerminalDocumentTypeInfo>();
            LCP<PropertyInterface> isTerminalDocumentType = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("terminalDocumentType"));

            ImRevMap<PropertyInterface, KeyExpr> terminalDocumentTypeKeys = isTerminalDocumentType.getMapKeys();
            KeyExpr terminalDocumentTypeKey = terminalDocumentTypeKeys.singleValue();
            QueryBuilder<PropertyInterface, Object> terminalDocumentTypeQuery = new QueryBuilder<PropertyInterface, Object>(terminalDocumentTypeKeys);
            terminalDocumentTypeQuery.addProperty("idTerminalDocumentType", LM.findLCPByCompoundName("idTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("nameTerminalDocumentType", LM.findLCPByCompoundName("nameTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("nameInHandbook1TerminalDocumentType", LM.findLCPByCompoundName("nameInHandbook1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("idTerminalHandbookType1TerminalDocumentType", LM.findLCPByCompoundName("idTerminalHandbookType1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("nameInHandbook2TerminalDocumentType", LM.findLCPByCompoundName("nameInHandbook2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("idTerminalHandbookType2TerminalDocumentType", LM.findLCPByCompoundName("idTerminalHandbookType2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.and(isTerminalDocumentType.property.getExpr(terminalDocumentTypeKeys).getWhere());

            ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> terminalDocumentTypeResult = terminalDocumentTypeQuery.execute(session.sql);

            for (ImMap<Object, Object> values : terminalDocumentTypeResult.valueIt()) {
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
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public String sendSalesInfo(List<SalesInfo> salesInfoList, String equipmentServer) throws IOException, SQLException {
        try {
            DataSession session = BL.createSession();
            ImportField cashRegisterField = new ImportField(LM.findLCPByCompoundName("numberCashRegister"));
            ImportField zReportNumberField = new ImportField(LM.findLCPByCompoundName("numberZReport"));

            ImportField numberReceiptField = new ImportField(LM.findLCPByCompoundName("numberReceipt"));
            ImportField dateField = new ImportField(LM.findLCPByCompoundName("dateReceipt"));
            ImportField timeField = new ImportField(LM.findLCPByCompoundName("timeReceipt"));

            ImportField numberReceiptDetailField = new ImportField(LM.findLCPByCompoundName("numberReceiptDetail"));
            ImportField idBarcodeReceiptDetailField = new ImportField(LM.findLCPByCompoundName("idBarcodeReceiptDetail"));

            ImportField quantityReceiptSaleDetailField = new ImportField(LM.findLCPByCompoundName("quantityReceiptSaleDetail"));
            ImportField priceReceiptSaleDetailField = new ImportField(LM.findLCPByCompoundName("priceReceiptSaleDetail"));
            ImportField sumReceiptSaleDetailField = new ImportField(LM.findLCPByCompoundName("sumReceiptSaleDetail"));
            ImportField discountSumReceiptSaleDetailField = new ImportField(LM.findLCPByCompoundName("discountSumReceiptSaleDetail"));
            ImportField discountSumSaleReceiptField = new ImportField(LM.findLCPByCompoundName("discountSumSaleReceipt"));

            ImportField quantityReceiptReturnDetailField = new ImportField(LM.findLCPByCompoundName("quantityReceiptReturnDetail"));
            ImportField priceReceiptReturnDetailField = new ImportField(LM.findLCPByCompoundName("priceReceiptReturnDetail"));
            ImportField retailSumReceiptReturnDetailField = new ImportField(LM.findLCPByCompoundName("sumReceiptReturnDetail"));
            ImportField discountSumReceiptReturnDetailField = new ImportField(LM.findLCPByCompoundName("discountSumReceiptReturnDetail"));
            ImportField discountSumReturnReceiptField = new ImportField(LM.findLCPByCompoundName("discountSumReturnReceipt"));

            ImportField sidTypePaymentField = new ImportField(LM.findLCPByCompoundName("sidPaymentType"));
            ImportField sumPaymentField = new ImportField(LM.findLCPByCompoundName("POS.sumPayment"));
            ImportField numberPaymentField = new ImportField(LM.findLCPByCompoundName("POS.numberPayment"));

            ImportField seriesNumberDiscountCardField = new ImportField(LM.findLCPByCompoundName("seriesNumberObject"));

            List<ImportProperty<?>> saleProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> returnProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> paymentProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> zReportKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("zReportPosted"), LM.findLCPByCompoundName("numberNumberCashRegisterToZReportPosted").getMapping(zReportNumberField, cashRegisterField));
            ImportKey<?> cashRegisterKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("cashRegister"), LM.findLCPByCompoundName("cashRegisterNumber").getMapping(cashRegisterField));
            ImportKey<?> receiptKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("receipt"), LM.findLCPByCompoundName("zReportReceiptToReceipt").getMapping(zReportNumberField, numberReceiptField, cashRegisterField));
            ImportKey<?> skuKey = new ImportKey((CustomClass) LM.findClassByCompoundName("sku"), LM.findLCPByCompoundName("skuBarcodeIdDate").getMapping(idBarcodeReceiptDetailField, dateField));
            ImportKey<?> discountCardKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("discountCard"), LM.findLCPByCompoundName("discountCardSeriesNumber").getMapping(seriesNumberDiscountCardField, dateField));

            saleProperties.add(new ImportProperty(zReportNumberField, LM.findLCPByCompoundName("numberZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(cashRegisterField, LM.findLCPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                    LM.baseLM.object(LM.findClassByCompoundName("cashRegister")).getMapping(cashRegisterKey)));
            saleProperties.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dateZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(timeField, LM.findLCPByCompoundName("timeZReport").getMapping(zReportKey)));

            saleProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("numberReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dateReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(timeField, LM.findLCPByCompoundName("timeReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(discountSumSaleReceiptField, LM.findLCPByCompoundName("discountSumSaleReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(zReportNumberField, LM.findLCPByCompoundName("zReportReceipt").getMapping(receiptKey),
                    LM.baseLM.object(LM.findClassByCompoundName("zReport")).getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, LM.findLCPByCompoundName("seriesNumberObject").getMapping(discountCardKey)));
            saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, LM.findLCPByCompoundName("discountCardReceipt").getMapping(receiptKey),
                    LM.baseLM.object(LM.findClassByCompoundName("discountCard")).getMapping(discountCardKey)));

            ImportKey<?> receiptSaleDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("receiptSaleDetail"), LM.findLCPByCompoundName("zReportReceiptReceiptDetailToReceiptDetail").getMapping(zReportNumberField, numberReceiptField, numberReceiptDetailField, cashRegisterField));
            saleProperties.add(new ImportProperty(numberReceiptDetailField, LM.findLCPByCompoundName("numberReceiptDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, LM.findLCPByCompoundName("idBarcodeReceiptDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(quantityReceiptSaleDetailField, LM.findLCPByCompoundName("quantityReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(priceReceiptSaleDetailField, LM.findLCPByCompoundName("priceReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(sumReceiptSaleDetailField, LM.findLCPByCompoundName("sumReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(discountSumReceiptSaleDetailField, LM.findLCPByCompoundName("discountSumReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("receiptReceiptDetail").getMapping(receiptSaleDetailKey),
                    LM.baseLM.object(LM.findClassByCompoundName("receipt")).getMapping(receiptKey)));

            saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, LM.findLCPByCompoundName("skuReceiptSaleDetail").getMapping(receiptSaleDetailKey),
                    LM.baseLM.object(LM.findClassByCompoundName("sku")).getMapping(skuKey)));


            returnProperties.add(new ImportProperty(zReportNumberField, LM.findLCPByCompoundName("numberZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(cashRegisterField, LM.findLCPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                    LM.baseLM.object(LM.findClassByCompoundName("cashRegister")).getMapping(cashRegisterKey)));
            returnProperties.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dateZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(timeField, LM.findLCPByCompoundName("timeZReport").getMapping(zReportKey)));

            returnProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("numberReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(dateField, LM.findLCPByCompoundName("dateReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(timeField, LM.findLCPByCompoundName("timeReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(discountSumReturnReceiptField, LM.findLCPByCompoundName("discountSumReturnReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(zReportNumberField, LM.findLCPByCompoundName("zReportReceipt").getMapping(receiptKey),
                    LM.baseLM.object(LM.findClassByCompoundName("zReport")).getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, LM.findLCPByCompoundName("seriesNumberObject").getMapping(discountCardKey)));
            returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, LM.findLCPByCompoundName("discountCardReceipt").getMapping(receiptKey),
                    LM.baseLM.object(LM.findClassByCompoundName("discountCard")).getMapping(discountCardKey)));

            ImportKey<?> receiptReturnDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("receiptReturnDetail"), LM.findLCPByCompoundName("zReportReceiptReceiptDetailToReceiptDetail").getMapping(zReportNumberField, numberReceiptField, numberReceiptDetailField, cashRegisterField));
            returnProperties.add(new ImportProperty(numberReceiptDetailField, LM.findLCPByCompoundName("numberReceiptDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, LM.findLCPByCompoundName("idBarcodeReceiptDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(quantityReceiptReturnDetailField, LM.findLCPByCompoundName("quantityReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(priceReceiptReturnDetailField, LM.findLCPByCompoundName("priceReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(retailSumReceiptReturnDetailField, LM.findLCPByCompoundName("sumReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(discountSumReceiptReturnDetailField, LM.findLCPByCompoundName("discountSumReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("receiptReceiptDetail").getMapping(receiptReturnDetailKey),
                    LM.baseLM.object(LM.findClassByCompoundName("receipt")).getMapping(receiptKey)));

            returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, LM.findLCPByCompoundName("skuReceiptReturnDetail").getMapping(receiptReturnDetailKey),
                    LM.baseLM.object(LM.findClassByCompoundName("sku")).getMapping(skuKey)));

            List<List<Object>> dataSale = new ArrayList<List<Object>>();
            List<List<Object>> dataReturn = new ArrayList<List<Object>>();

            List<List<Object>> dataPayment = new ArrayList<List<Object>>();

            if (salesInfoList != null)
                for (SalesInfo sale : salesInfoList) {
                    if (sale.quantityReceiptDetail < 0)
                        dataReturn.add(Arrays.<Object>asList(sale.cashRegisterNumber, sale.zReportNumber, sale.date, sale.time, sale.receiptNumber,
                                sale.numberReceiptDetail, sale.barcodeItem, sale.quantityReceiptDetail, sale.priceReceiptDetail, sale.sumReceiptDetail,
                                sale.discountSumReceiptDetail, sale.discountSumReceipt, sale.seriesNumberDiscountCard));
                    else
                        dataSale.add(Arrays.<Object>asList(sale.cashRegisterNumber, sale.zReportNumber, sale.date, sale.time, sale.receiptNumber,
                                sale.numberReceiptDetail, sale.barcodeItem, sale.quantityReceiptDetail, sale.priceReceiptDetail, sale.sumReceiptDetail,
                                sale.discountSumReceiptDetail, sale.discountSumReceipt, sale.seriesNumberDiscountCard));
                    if (sale.sumCash != 0)
                        dataPayment.add(Arrays.<Object>asList(sale.zReportNumber, sale.receiptNumber, sale.cashRegisterNumber, "cash", sale.sumCash, 1));
                    if (sale.sumCard != 0)
                        dataPayment.add(Arrays.<Object>asList(sale.zReportNumber, sale.receiptNumber, sale.cashRegisterNumber, "card", sale.sumCard, 2));
                }

            List<ImportField> saleImportFields = Arrays.asList(cashRegisterField, zReportNumberField, dateField, timeField,
                    numberReceiptField, numberReceiptDetailField, idBarcodeReceiptDetailField, quantityReceiptSaleDetailField,
                    priceReceiptSaleDetailField, sumReceiptSaleDetailField, discountSumReceiptSaleDetailField,
                    discountSumSaleReceiptField, seriesNumberDiscountCardField);

            List<ImportField> returnImportFields = Arrays.asList(cashRegisterField, zReportNumberField, dateField, timeField,
                    numberReceiptField, numberReceiptDetailField, idBarcodeReceiptDetailField, quantityReceiptReturnDetailField,
                    priceReceiptReturnDetailField, retailSumReceiptReturnDetailField, discountSumReceiptReturnDetailField,
                    discountSumReturnReceiptField, seriesNumberDiscountCardField);


            new IntegrationService(session, new ImportTable(saleImportFields, dataSale), Arrays.asList(zReportKey, cashRegisterKey, receiptKey, receiptSaleDetailKey, skuKey, discountCardKey),
                    saleProperties).synchronize(true);

            new IntegrationService(session, new ImportTable(returnImportFields, dataReturn), Arrays.asList(zReportKey, cashRegisterKey, receiptKey, receiptReturnDetailKey, skuKey, discountCardKey),
                    returnProperties).synchronize(true);

            ImportKey<?> paymentKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("POS.payment"), LM.findLCPByCompoundName("zReportReceiptPaymentToPayment").getMapping(zReportNumberField, numberReceiptField, numberPaymentField, cashRegisterField));
            ImportKey<?> paymentTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("paymentType"), LM.findLCPByCompoundName("sidToTypePayment").getMapping(sidTypePaymentField));
            paymentProperties.add(new ImportProperty(sumPaymentField, LM.findLCPByCompoundName("POS.sumPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(numberPaymentField, LM.findLCPByCompoundName("numberPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(sidTypePaymentField, LM.findLCPByCompoundName("paymentTypePayment").getMapping(paymentKey),
                    LM.baseLM.object(LM.findClassByCompoundName("paymentType")).getMapping(paymentTypeKey)));
            paymentProperties.add(new ImportProperty(numberReceiptField, LM.findLCPByCompoundName("receiptPayment").getMapping(paymentKey),
                    LM.baseLM.object(LM.findClassByCompoundName("receipt")).getMapping(receiptKey)));

            List<ImportField> paymentImportFields = Arrays.asList(zReportNumberField, numberReceiptField, cashRegisterField, sidTypePaymentField,
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

                DataObject logObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("equipmentServerLog"));
                Object equipmentServerObject = LM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
                LM.findLCPByCompoundName("equipmentServerEquipmentServerLog").change(equipmentServerObject, session, logObject);
                LM.findLCPByCompoundName("dataEquipmentServerLog").change(message, session, logObject);
                LM.findLCPByCompoundName("dateEquipmentServerLog").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            new IntegrationService(session, new ImportTable(paymentImportFields, dataPayment), Arrays.asList(paymentKey, paymentTypeKey, receiptKey, cashRegisterKey),
                    paymentProperties).synchronize(true);

            return session.applyMessage(this.BL);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public String sendTerminalDocumentInfo(List<TerminalDocumentInfo> terminalDocumentInfoList, String equServerID) throws IOException, SQLException {
        try {
            DataSession session = BL.createSession();
            ImportField idTerminalDocumentField = new ImportField(LM.findLCPByCompoundName("idTerminalDocument"));
            ImportField typeTerminalDocumentField = new ImportField(LM.findLCPByCompoundName("idTerminalDocumentTypeTerminalDocument"));
            ImportField idTerminalHandbookType1TerminalDocumentField = new ImportField(LM.findLCPByCompoundName("idTerminalHandbookType1TerminalDocument"));
            ImportField idTerminalHandbookType2TerminalDocumentField = new ImportField(LM.findLCPByCompoundName("idTerminalHandbookType2TerminalDocument"));
            ImportField titleTerminalDocumentField = new ImportField(LM.findLCPByCompoundName("titleTerminalDocument"));
            ImportField quantityTerminalDocumentField = new ImportField(LM.findLCPByCompoundName("quantityTerminalDocument"));

            ImportField numberTerminalDocumentDetailField = new ImportField(LM.findLCPByCompoundName("numberTerminalDocumentDetail"));
            ImportField barcodeTerminalDocumentDetailField = new ImportField(LM.findLCPByCompoundName("barcodeTerminalDocumentDetail"));
            ImportField nameTerminalDocumentDetailField = new ImportField(LM.findLCPByCompoundName("nameTerminalDocumentDetail"));
            ImportField quantityTerminalDocumentDetailField = new ImportField(LM.findLCPByCompoundName("quantityTerminalDocumentDetail"));
            ImportField priceTerminalDocumentDetailField = new ImportField(LM.findLCPByCompoundName("priceTerminalDocumentDetail"));
            ImportField sumTerminalDocumentDetailField = new ImportField(LM.findLCPByCompoundName("sumTerminalDocumentDetail"));

            ImportField isNewTerminalDocumentDetailField = new ImportField(LM.findLCPByCompoundName("isNewTerminalDocumentDetail"));

            List<ImportProperty<?>> terminalDocumentProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> terminalDocumentDetailProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> terminalDocumentKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("terminalDocument"), LM.findLCPByCompoundName("terminalDocumentID").getMapping(idTerminalDocumentField));

            terminalDocumentProperties.add(new ImportProperty(idTerminalDocumentField, LM.findLCPByCompoundName("idTerminalDocument").getMapping(terminalDocumentKey)));
            //terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, LM.findLCPByCompoundName("typeTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(titleTerminalDocumentField, LM.findLCPByCompoundName("titleTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType1TerminalDocumentField, LM.findLCPByCompoundName("idTerminalHandbookType1TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType2TerminalDocumentField, LM.findLCPByCompoundName("idTerminalHandbookType2TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(quantityTerminalDocumentField, LM.findLCPByCompoundName("quantityTerminalDocument").getMapping(terminalDocumentKey)));

            ImportKey<?> terminalDocumentDetailKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("terminalDocumentDetail"), LM.findLCPByCompoundName("terminalDocumentDetailIDDocumentIDDetail").getMapping(idTerminalDocumentField, numberTerminalDocumentDetailField));

            terminalDocumentDetailProperties.add(new ImportProperty(numberTerminalDocumentDetailField, LM.findLCPByCompoundName("numberTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(barcodeTerminalDocumentDetailField, LM.findLCPByCompoundName("barcodeTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(nameTerminalDocumentDetailField, LM.findLCPByCompoundName("nameTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(quantityTerminalDocumentDetailField, LM.findLCPByCompoundName("quantityTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(priceTerminalDocumentDetailField, LM.findLCPByCompoundName("priceTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(sumTerminalDocumentDetailField, LM.findLCPByCompoundName("sumTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(isNewTerminalDocumentDetailField, LM.findLCPByCompoundName("isNewTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(idTerminalDocumentField, LM.findLCPByCompoundName("terminalDocumentTerminalDocumentDetail").getMapping(terminalDocumentDetailKey),
                    LM.baseLM.object(LM.findClassByCompoundName("terminalDocument")).getMapping(terminalDocumentKey)));


            ImportKey<?> terminalDocumentTypeKey = new ImportKey((ConcreteCustomClass) LM.findClassByCompoundName("terminalDocumentType"), LM.findLCPByCompoundName("terminalDocumentTypeID").getMapping(typeTerminalDocumentField));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, LM.findLCPByCompoundName("idTerminalDocumentType").getMapping(terminalDocumentTypeKey)));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, LM.findLCPByCompoundName("terminalDocumentTypeTerminalDocument").getMapping(terminalDocumentKey),
                    LM.baseLM.object(LM.findClassByCompoundName("terminalDocumentType")).getMapping(terminalDocumentTypeKey)));

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

                DataObject logObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("equipmentServerLog"));
                Object equipmentServerObject = LM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equServerID, StringClass.get(20)));
                LM.findLCPByCompoundName("equipmentServerEquipmentServerLog").change(equipmentServerObject, session, logObject);
                LM.findLCPByCompoundName("dataEquipmentServerLog").change(message, session, logObject);
                LM.findLCPByCompoundName("dateEquipmentServerLog").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            return session.applyMessage(this.BL);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public void succeedTransaction(Integer transactionID) throws RemoteException, SQLException {
        try {
            DataSession session = BL.createSession();
            LM.findLCPByCompoundName("succeededMachineryPriceTransaction").change(true, session,
                    session.getDataObject(transactionID, LM.findClassByCompoundName("machineryPriceTransaction").getType()));
            session.apply(this.BL);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public List<byte[][]> readLabelFormats(List<String> scalesModelsList) throws RemoteException, SQLException {
        try {
            DataSession session = BL.createSession();

            List<byte[][]> fileLabelFormats = new ArrayList<byte[][]>();

            for (String scalesModel : scalesModelsList) {

                DataObject scalesModelObject = new DataObject(LM.findLCPByCompoundName("scalesModelName").read(session, new DataObject(scalesModel)), (ConcreteClass) LM.findClassByCompoundName("scalesModel"));

                LCP<PropertyInterface> isLabelFormat = (LCP<PropertyInterface>) LM.is(LM.findClassByCompoundName("labelFormat"));

                ImRevMap<PropertyInterface, KeyExpr> labelFormatKeys = isLabelFormat.getMapKeys();
                KeyExpr labelFormatKey = labelFormatKeys.singleValue();
                QueryBuilder<PropertyInterface, Object> labelFormatQuery = new QueryBuilder<PropertyInterface, Object>(labelFormatKeys);

                labelFormatQuery.addProperty("fileLabelFormat", LM.findLCPByCompoundName("fileLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.addProperty("fileMessageLabelFormat", LM.findLCPByCompoundName("fileMessageLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.and(isLabelFormat.property.getExpr(labelFormatKeys).getWhere());
                labelFormatQuery.and(LM.findLCPByCompoundName("scalesModelLabelFormat").getExpr(labelFormatKey).compare((scalesModelObject).getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> labelFormatResult = labelFormatQuery.execute(session.sql);

                for (ImMap<Object, Object> row : labelFormatResult.valueIt()) {
                    byte[] fileLabelFormat = (byte[]) row.get("fileLabelFormat");
                    byte[] fileMessageLabelFormat = (byte[]) row.get("fileMessageLabelFormat");
                    fileLabelFormats.add(new byte[][]{fileLabelFormat, fileMessageLabelFormat});
                }
            }
            return fileLabelFormats;
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public void errorTransactionReport(Integer transactionID, Exception e) throws RemoteException, SQLException {
        try {
            DataSession session = BL.createSession();
            DataObject errorObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("machineryPriceTransactionError"));
            LM.findLCPByCompoundName("machineryPriceTransactionMachineryPriceTransactionError").change(transactionID, session, errorObject);
            LM.findLCPByCompoundName("dataMachineryPriceTransactionError").change(e.toString(), session, errorObject);
            LM.findLCPByCompoundName("dateMachineryPriceTransactionError").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(os));
            LM.findLCPByCompoundName("errorTraceMachineryPriceTransactionError").change(os.toString(), session, errorObject);

            session.apply(this.BL);
        } catch (ScriptingErrorLog.SemanticErrorException e2) {
            throw new RuntimeException(e2.toString());
        }
    }

    @Override
    public void errorEquipmentServerReport(String equipmentServer, Throwable exception) throws
            RemoteException, SQLException {
        try {
            DataSession session = BL.createSession();
            DataObject errorObject = session.addObject((ConcreteCustomClass) LM.findClassByCompoundName("equipmentServerError"));
            Object equipmentServerObject = LM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
            LM.findLCPByCompoundName("equipmentServerEquipmentServerError").change(equipmentServerObject, session, errorObject);
            LM.findLCPByCompoundName("dataEquipmentServerError").change(exception.toString(), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(os));
            LM.findLCPByCompoundName("erTraceEquipmentServerError").change(os.toString(), session, errorObject);

            LM.findLCPByCompoundName("dateEquipmentServerError").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);

            session.apply(this.BL);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public EquipmentServerSettings readEquipmentServerSettings(String equipmentServer) throws RemoteException, SQLException {
        try {
            DataSession session = BL.createSession();
            Integer equipmentServerID = (Integer) LM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
            Integer delay = (Integer) LM.findLCPByCompoundName("delayEquipmentServer").read(session, new DataObject(equipmentServerID, (ConcreteClass) LM.findClassByCompoundName("equipmentServer")));
            return new EquipmentServerSettings(delay);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
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
