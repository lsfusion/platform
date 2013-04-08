package equ.srv;

import equ.api.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import platform.base.DateConverter;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.classes.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.integration.*;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;
import platform.server.logics.*;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class EquipmentServer extends LifecycleAdapter implements EquipmentServerInterface, InitializingBean {
    private static final Logger logger = Logger.getLogger(EquipmentServer.class);

    private RMIManager rmiManager;

    private BusinessLogics businessLogics;

    private DBManager dbManager;

    private ScriptingLogicsModule equLM;

    private boolean started = false;

    public void setRmiManager(RMIManager rmiManager) {
        this.rmiManager = rmiManager;
    }

    public void setBusinessLogics(BusinessLogics businessLogics) {
        this.businessLogics = businessLogics;
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(rmiManager, "rmiManager must be specified");
        Assert.notNull(businessLogics, "businessLogics must be specified");
        Assert.notNull(dbManager, "dbManager must be specified");
    }

    @Override
    protected void onInit(LifecycleEvent event) {
        equLM = (ScriptingLogicsModule) businessLogics.getModule("Equipment");
        Assert.notNull(equLM, "can't find Equipment module");
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        logger.info("Binding Equipment Server.");
        try {
            rmiManager.bindAndExport(getExportName(), this);
            started = true;
        } catch (Exception e) {
            throw new RuntimeException("Error exporting Equipment Server: ", e);
        }
    }

    @Override
    protected void onStopping(LifecycleEvent event) {
        if (started) {
            logger.info("Stopping Equipment Server.");
            try {
                rmiManager.unbindAndUnexport(getExportName(), this);
            } catch (Exception e) {
                throw new RuntimeException("Error stopping Equipment Server: ", e);
            }
        }
    }

    private String getExportName() {
        return rmiManager.getDbName() + "/EquipmentServer";
    }

    @Override
    public List<TransactionInfo> readTransactionInfo(String equServerID) throws RemoteException, SQLException {
        try {

            DataSession session = dbManager.createSession();
            List<TransactionInfo> transactionList = new ArrayList<TransactionInfo>();

            LCP isMachineryPriceTransaction = equLM.is(equLM.findClassByCompoundName("MachineryPriceTransaction"));
            ImRevMap<Object, KeyExpr> keys = isMachineryPriceTransaction.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<Object, Object> query = new QueryBuilder<Object, Object>(keys);

            query.addProperty("dateTimeMPT", equLM.findLCPByCompoundName("dateTimeMachineryPriceTransaction").getExpr(key));
            query.addProperty("groupMachineryMPT", equLM.findLCPByCompoundName("groupMachineryMachineryPriceTransaction").getExpr(key));
            query.addProperty("snapshotMPT", equLM.findLCPByCompoundName("groupMachineryMachineryPriceTransaction").getExpr(key));

            query.and(equLM.findLCPByCompoundName("sidEquipmentServerMachineryPriceTransaction").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));
            query.and(equLM.findLCPByCompoundName("processMachineryPriceTransaction").getExpr(key).getWhere());

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
                skuQuery.addProperty("idBarcode", equLM.findLCPByCompoundName("idBarcode").getExpr(barcodeExpr));
                skuQuery.addProperty("name", equLM.findLCPByCompoundName("nameMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("price", equLM.findLCPByCompoundName("priceMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("daysExpiry", equLM.findLCPByCompoundName("daysExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("hoursExpiry", equLM.findLCPByCompoundName("hoursExpiryMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("expiryDate", equLM.findLCPByCompoundName("expiryDataMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("labelFormat", equLM.findLCPByCompoundName("labelFormatMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("composition", equLM.findLCPByCompoundName("compositionMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("isWeight", equLM.findLCPByCompoundName("isWeightMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
                skuQuery.addProperty("skuGroup", equLM.findLCPByCompoundName("skuGroupMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));

                skuQuery.and(equLM.findLCPByCompoundName("inMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr).getWhere());

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
                    String canonicalNameSkuGroup = numberSkuGroup == null ? "" : (String) equLM.findLCPByCompoundName("canonicalNameSkuGroup").read(session, new DataObject(numberSkuGroup, (ConcreteClass) equLM.findClassByCompoundName("SkuGroup")));

                    Integer cellScalesObject = composition == null ? null : (Integer) equLM.findLCPByCompoundName("cellScalesGroupScalesComposition").read(session, groupObject, new DataObject(composition, TextClass.instance));
                    Integer compositionNumberCellScales = cellScalesObject == null ? null : (Integer) equLM.findLCPByCompoundName("numberCellScales").read(session, new DataObject(cellScalesObject, (ConcreteClass) equLM.findClassByCompoundName("CellScales")));

                    skuTransactionList.add(new ItemInfo(barcode.trim(), name.trim(), price, daysExpiry, hoursExpiry, expiryDate, labelFormat, composition, compositionNumberCellScales, isWeight, numberSkuGroup == null ? 0 : numberSkuGroup, canonicalNameSkuGroup.trim()));
                }

                if (transactionObject.objectClass.equals(equLM.findClassByCompoundName("CashRegisterPriceTransaction"))) {
                    List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();
                    LCP<PropertyInterface> isCashRegister = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("CashRegister"));

                    ImRevMap<PropertyInterface, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                    KeyExpr cashRegisterKey = cashRegisterKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> cashRegisterQuery = new QueryBuilder<PropertyInterface, Object>(cashRegisterKeys);

                    cashRegisterQuery.addProperty("directoryCashRegister", equLM.findLCPByCompoundName("directoryCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("portMachinery", equLM.findLCPByCompoundName("portMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("nppMachinery", equLM.findLCPByCompoundName("nppMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("numberCashRegister", equLM.findLCPByCompoundName("numberCashRegister").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundName("nameModelMachinery").getExpr(cashRegisterKey));
                    cashRegisterQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundName("handlerModelMachinery").getExpr(cashRegisterKey));

                    cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                    cashRegisterQuery.and(equLM.findLCPByCompoundName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare(groupObject, Compare.EQUALS));
                    if (snapshotTransaction)
                        cashRegisterQuery.and(equLM.findLCPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), cashRegisterKey).getWhere());

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

                } else if (transactionObject.objectClass.equals(equLM.findClassByCompoundName("ScalesPriceTransaction"))) {
                    List<ScalesInfo> scalesInfoList = new ArrayList<ScalesInfo>();
                    String directory = (String) equLM.findLCPByCompoundName("directoryGroupScales").read(session, groupObject);
                    String pieceCodeGroupScales = (String) equLM.findLCPByCompoundName("pieceCodeGroupScales").read(session, groupObject);
                    String weightCodeGroupScales = (String) equLM.findLCPByCompoundName("weightCodeGroupScales").read(session, groupObject);

                    LCP<PropertyInterface> isScales = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("Scales"));

                    ImRevMap<PropertyInterface, KeyExpr> scalesKeys = isScales.getMapKeys();
                    KeyExpr scalesKey = scalesKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> scalesQuery = new QueryBuilder<PropertyInterface, Object>(scalesKeys);

                    scalesQuery.addProperty("portMachinery", equLM.findLCPByCompoundName("portMachinery").getExpr(scalesKey));
                    scalesQuery.addProperty("nppMachinery", equLM.findLCPByCompoundName("nppMachinery").getExpr(scalesKey));
                    scalesQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundName("nameModelMachinery").getExpr(scalesKey));
                    scalesQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundName("handlerModelMachinery").getExpr(scalesKey));
                    scalesQuery.and(isScales.property.getExpr(scalesKeys).getWhere());
                    scalesQuery.and(equLM.findLCPByCompoundName("groupScalesScales").getExpr(scalesKey).compare(groupObject, Compare.EQUALS));
                    //if (snapshotTransaction)
                    //    scalesQuery.and(equLM.findLCPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), scalesKey).getWhere());

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

                } else if (transactionObject.objectClass.equals(equLM.findClassByCompoundName("PriceCheckerPriceTransaction"))) {
                    List<PriceCheckerInfo> priceCheckerInfoList = new ArrayList<PriceCheckerInfo>();
                    LCP<PropertyInterface> isCheck = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("Check"));

                    ImRevMap<PropertyInterface, KeyExpr> checkKeys = isCheck.getMapKeys();
                    KeyExpr checkKey = checkKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> checkQuery = new QueryBuilder<PropertyInterface, Object>(checkKeys);

                    checkQuery.addProperty("portMachinery", equLM.findLCPByCompoundName("portMachinery").getExpr(checkKey));
                    checkQuery.addProperty("nppMachinery", equLM.findLCPByCompoundName("nppMachinery").getExpr(checkKey));
                    checkQuery.addProperty("nameCheckModelCheck", equLM.findLCPByCompoundName("nameCheckModelCheck").getExpr(checkKey));
                    //checkQuery.addProperty("handlerCheckModelCheck", equLM.findLCPByCompoundName("handlerCheckModelCheck").getExpr(checkKey));
                    checkQuery.and(isCheck.property.getExpr(checkKeys).getWhere());
                    checkQuery.and(equLM.findLCPByCompoundName("groupPriceCheckerPriceChecker").getExpr(checkKey).compare(groupObject, Compare.EQUALS));

                    if (snapshotTransaction)
                        checkQuery.and(equLM.findLCPByCompoundName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), checkKey).getWhere());

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


                } else if (transactionObject.objectClass.equals(equLM.findClassByCompoundName("TerminalPriceTransaction"))) {
                    List<TerminalInfo> terminalInfoList = new ArrayList<TerminalInfo>();
                    LCP<PropertyInterface> isTerminal = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("Terminal"));

                    ImRevMap<PropertyInterface, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                    KeyExpr terminalKey = terminalKeys.singleValue();
                    QueryBuilder<PropertyInterface, Object> terminalQuery = new QueryBuilder<PropertyInterface, Object>(terminalKeys);

                    terminalQuery.addProperty("directoryTerminal", equLM.findLCPByCompoundName("directoryTerminal").getExpr(terminalKey));
                    terminalQuery.addProperty("portMachinery", equLM.findLCPByCompoundName("portMachinery").getExpr(terminalKey));
                    terminalQuery.addProperty("nppMachinery", equLM.findLCPByCompoundName("nppMachinery").getExpr(terminalKey));
                    terminalQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundName("nameModelMachinery").getExpr(terminalKey));
                    terminalQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundName("handlerModelMachinery").getExpr(terminalKey));
                    terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                    terminalQuery.and(equLM.findLCPByCompoundName("groupTerminalTerminal").getExpr(terminalKey).compare(groupObject, Compare.EQUALS));

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
            DataSession session = dbManager.createSession();
            List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();

            LCP<PropertyInterface> isGroupMachinery = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("GroupMachinery"));
            ImRevMap<PropertyInterface, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.addProperty("roundSalesGroupCashRegister", equLM.findLCPByCompoundName("roundSalesGroupCashRegister").getExpr(key));
            query.and(equLM.findLCPByCompoundName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

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

                LCP<PropertyInterface> isCashRegister = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("CashRegister"));

                ImRevMap<PropertyInterface, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                KeyExpr cashRegisterKey = cashRegisterKeys.singleValue();
                QueryBuilder<PropertyInterface, Object> cashRegisterQuery = new QueryBuilder<PropertyInterface, Object>(cashRegisterKeys);

                cashRegisterQuery.addProperty("directoryCashRegister", equLM.findLCPByCompoundName("directoryCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("portMachinery", equLM.findLCPByCompoundName("portMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("nppMachinery", equLM.findLCPByCompoundName("nppMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("numberCashRegister", equLM.findLCPByCompoundName("numberCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundName("nameModelMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundName("handlerModelMachinery").getExpr(cashRegisterKey));

                cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                cashRegisterQuery.and(equLM.findLCPByCompoundName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

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
            DataSession session = dbManager.createSession();
            List<TerminalInfo> terminalInfoList = new ArrayList<TerminalInfo>();

            LCP<PropertyInterface> isGroupMachinery = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("GroupMachinery"));
            ImRevMap<PropertyInterface, KeyExpr> keys = isGroupMachinery.getMapKeys();
            KeyExpr key = keys.singleValue();
            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
            query.and(equLM.findLCPByCompoundName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

            ImOrderMap<ImMap<PropertyInterface, DataObject>, ImMap<Object, ObjectValue>> result = query.executeClasses(session);
            List<Object> groupMachineryObjects = new ArrayList<Object>();
            for (ImMap<PropertyInterface, DataObject> entry : result.keyIt()) {
                DataObject groupMachineryObject = entry.getValue(0);
                groupMachineryObjects.add(groupMachineryObject);
            }

            for (Object groupMachinery : groupMachineryObjects) {
                DataObject groupMachineryObject = (DataObject) groupMachinery;

                LCP<PropertyInterface> isTerminal = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("Terminal"));

                ImRevMap<PropertyInterface, KeyExpr> terminalKeys = isTerminal.getMapKeys();
                KeyExpr terminalKey = terminalKeys.singleValue();
                QueryBuilder<PropertyInterface, Object> terminalQuery = new QueryBuilder<PropertyInterface, Object>(terminalKeys);

                terminalQuery.addProperty("directoryTerminal", equLM.findLCPByCompoundName("directoryTerminal").getExpr(terminalKey));
                terminalQuery.addProperty("portMachinery", equLM.findLCPByCompoundName("portMachinery").getExpr(terminalKey));
                terminalQuery.addProperty("nppMachinery", equLM.findLCPByCompoundName("nppMachinery").getExpr(terminalKey));
                terminalQuery.addProperty("nameModelMachinery", equLM.findLCPByCompoundName("nameModelMachinery").getExpr(terminalKey));
                terminalQuery.addProperty("handlerModelMachinery", equLM.findLCPByCompoundName("handlerModelMachinery").getExpr(terminalKey));

                terminalQuery.and(isTerminal.property.getExpr(terminalKeys).getWhere());
                terminalQuery.and(equLM.findLCPByCompoundName("groupTerminalTerminal").getExpr(terminalKey).compare((groupMachineryObject).getExpr(), Compare.EQUALS));

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
            DataSession session = dbManager.createSession();

            List<LegalEntityInfo> legalEntityInfoList = new ArrayList<LegalEntityInfo>();

            LCP<PropertyInterface> isLegalEntity = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("LegalEntity"));

            ImRevMap<PropertyInterface, KeyExpr> legalEntityKeys = isLegalEntity.getMapKeys();
            KeyExpr legalEntityKey = legalEntityKeys.singleValue();
            QueryBuilder<PropertyInterface, Object> legalEntityQuery = new QueryBuilder<PropertyInterface, Object>(legalEntityKeys);

            legalEntityQuery.addProperty("name", equLM.baseLM.name.getExpr(legalEntityKey));
            legalEntityQuery.and(isLegalEntity.property.getExpr(legalEntityKeys).getWhere());
            ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> legalEntityResult = legalEntityQuery.execute(session.sql);
            for (int i=0,size=legalEntityResult.size();i<size;i++) {
                String id = String.valueOf(legalEntityResult.getKey(i).getValue(0));
                String name = (String) legalEntityResult.getValue(i).get("name");
                DataObject terminalHandbookTypeObject = ((ConcreteCustomClass) equLM.findClassByCompoundName("TerminalHandbookType")).getDataObject("terminalHandbookTypeLegalEntity");
                String type = (String) equLM.findLCPByCompoundName("idTerminalHandbookType").read(session, terminalHandbookTypeObject);
                legalEntityInfoList.add(new LegalEntityInfo(id, name, type));
            }

            List<TerminalDocumentTypeInfo> terminalDocumentTypeInfoList = new ArrayList<TerminalDocumentTypeInfo>();
            LCP<PropertyInterface> isTerminalDocumentType = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("TerminalDocumentType"));

            ImRevMap<PropertyInterface, KeyExpr> terminalDocumentTypeKeys = isTerminalDocumentType.getMapKeys();
            KeyExpr terminalDocumentTypeKey = terminalDocumentTypeKeys.singleValue();
            QueryBuilder<PropertyInterface, Object> terminalDocumentTypeQuery = new QueryBuilder<PropertyInterface, Object>(terminalDocumentTypeKeys);
            terminalDocumentTypeQuery.addProperty("idTerminalDocumentType", equLM.findLCPByCompoundName("idTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("nameTerminalDocumentType", equLM.findLCPByCompoundName("nameTerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("nameInHandbook1TerminalDocumentType", equLM.findLCPByCompoundName("nameInHandbook1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("idTerminalHandbookType1TerminalDocumentType", equLM.findLCPByCompoundName("idTerminalHandbookType1TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("nameInHandbook2TerminalDocumentType", equLM.findLCPByCompoundName("nameInHandbook2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
            terminalDocumentTypeQuery.addProperty("idTerminalHandbookType2TerminalDocumentType", equLM.findLCPByCompoundName("idTerminalHandbookType2TerminalDocumentType").getExpr(terminalDocumentTypeKey));
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
            DataSession session = dbManager.createSession();
            ImportField cashRegisterField = new ImportField(equLM.findLCPByCompoundName("numberCashRegister"));
            ImportField zReportNumberField = new ImportField(equLM.findLCPByCompoundName("numberZReport"));

            ImportField numberReceiptField = new ImportField(equLM.findLCPByCompoundName("numberReceipt"));
            ImportField dateField = new ImportField(equLM.findLCPByCompoundName("dateReceipt"));
            ImportField timeField = new ImportField(equLM.findLCPByCompoundName("timeReceipt"));

            ImportField numberReceiptDetailField = new ImportField(equLM.findLCPByCompoundName("numberReceiptDetail"));
            ImportField idBarcodeReceiptDetailField = new ImportField(equLM.findLCPByCompoundName("idBarcodeReceiptDetail"));

            ImportField quantityReceiptSaleDetailField = new ImportField(equLM.findLCPByCompoundName("quantityReceiptSaleDetail"));
            ImportField priceReceiptSaleDetailField = new ImportField(equLM.findLCPByCompoundName("priceReceiptSaleDetail"));
            ImportField sumReceiptSaleDetailField = new ImportField(equLM.findLCPByCompoundName("sumReceiptSaleDetail"));
            ImportField discountSumReceiptSaleDetailField = new ImportField(equLM.findLCPByCompoundName("discountSumReceiptSaleDetail"));
            ImportField discountSumSaleReceiptField = new ImportField(equLM.findLCPByCompoundName("discountSumSaleReceipt"));

            ImportField quantityReceiptReturnDetailField = new ImportField(equLM.findLCPByCompoundName("quantityReceiptReturnDetail"));
            ImportField priceReceiptReturnDetailField = new ImportField(equLM.findLCPByCompoundName("priceReceiptReturnDetail"));
            ImportField retailSumReceiptReturnDetailField = new ImportField(equLM.findLCPByCompoundName("sumReceiptReturnDetail"));
            ImportField discountSumReceiptReturnDetailField = new ImportField(equLM.findLCPByCompoundName("discountSumReceiptReturnDetail"));
            ImportField discountSumReturnReceiptField = new ImportField(equLM.findLCPByCompoundName("discountSumReturnReceipt"));

            ImportField sidTypePaymentField = new ImportField(equLM.findLCPByCompoundName("sidPaymentType"));
            ImportField sumPaymentField = new ImportField(equLM.findLCPByCompoundName("POS.sumPayment"));
            ImportField numberPaymentField = new ImportField(equLM.findLCPByCompoundName("POS.numberPayment"));

            ImportField seriesNumberDiscountCardField = new ImportField(equLM.findLCPByCompoundName("seriesNumberObject"));

            List<ImportProperty<?>> saleProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> returnProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> paymentProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> zReportKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("ZReportPosted"), equLM.findLCPByCompoundName("numberNumberCashRegisterToZReportPosted").getMapping(zReportNumberField, cashRegisterField));
            ImportKey<?> cashRegisterKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("CashRegister"), equLM.findLCPByCompoundName("cashRegisterNumber").getMapping(cashRegisterField));
            ImportKey<?> receiptKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("Receipt"), equLM.findLCPByCompoundName("zReportReceiptToReceipt").getMapping(zReportNumberField, numberReceiptField, cashRegisterField));
            ImportKey<?> skuKey = new ImportKey((CustomClass) equLM.findClassByCompoundName("Sku"), equLM.findLCPByCompoundName("skuBarcodeIdDate").getMapping(idBarcodeReceiptDetailField, dateField));
            ImportKey<?> discountCardKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("DiscountCard"), equLM.findLCPByCompoundName("discountCardSeriesNumber").getMapping(seriesNumberDiscountCardField, dateField));

            saleProperties.add(new ImportProperty(zReportNumberField, equLM.findLCPByCompoundName("numberZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(cashRegisterField, equLM.findLCPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("CashRegister")).getMapping(cashRegisterKey)));
            saleProperties.add(new ImportProperty(dateField, equLM.findLCPByCompoundName("dateZReport").getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(timeField, equLM.findLCPByCompoundName("timeZReport").getMapping(zReportKey)));

            saleProperties.add(new ImportProperty(numberReceiptField, equLM.findLCPByCompoundName("numberReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(dateField, equLM.findLCPByCompoundName("dateReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(timeField, equLM.findLCPByCompoundName("timeReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(discountSumSaleReceiptField, equLM.findLCPByCompoundName("discountSumSaleReceipt").getMapping(receiptKey)));
            saleProperties.add(new ImportProperty(zReportNumberField, equLM.findLCPByCompoundName("zReportReceipt").getMapping(receiptKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("ZReport")).getMapping(zReportKey)));
            saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, equLM.findLCPByCompoundName("seriesNumberObject").getMapping(discountCardKey)));
            saleProperties.add(new ImportProperty(seriesNumberDiscountCardField, equLM.findLCPByCompoundName("discountCardReceipt").getMapping(receiptKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("DiscountCard")).getMapping(discountCardKey)));

            ImportKey<?> receiptSaleDetailKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("ReceiptSaleDetail"), equLM.findLCPByCompoundName("zReportReceiptReceiptDetailToReceiptDetail").getMapping(zReportNumberField, numberReceiptField, numberReceiptDetailField, cashRegisterField));
            saleProperties.add(new ImportProperty(numberReceiptDetailField, equLM.findLCPByCompoundName("numberReceiptDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, equLM.findLCPByCompoundName("idBarcodeReceiptDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(quantityReceiptSaleDetailField, equLM.findLCPByCompoundName("quantityReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(priceReceiptSaleDetailField, equLM.findLCPByCompoundName("priceReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(sumReceiptSaleDetailField, equLM.findLCPByCompoundName("sumReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(discountSumReceiptSaleDetailField, equLM.findLCPByCompoundName("discountSumReceiptSaleDetail").getMapping(receiptSaleDetailKey)));
            saleProperties.add(new ImportProperty(numberReceiptField, equLM.findLCPByCompoundName("receiptReceiptDetail").getMapping(receiptSaleDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Receipt")).getMapping(receiptKey)));

            saleProperties.add(new ImportProperty(idBarcodeReceiptDetailField, equLM.findLCPByCompoundName("skuReceiptSaleDetail").getMapping(receiptSaleDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Sku")).getMapping(skuKey)));


            returnProperties.add(new ImportProperty(zReportNumberField, equLM.findLCPByCompoundName("numberZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(cashRegisterField, equLM.findLCPByCompoundName("cashRegisterZReport").getMapping(zReportKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("CashRegister")).getMapping(cashRegisterKey)));
            returnProperties.add(new ImportProperty(dateField, equLM.findLCPByCompoundName("dateZReport").getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(timeField, equLM.findLCPByCompoundName("timeZReport").getMapping(zReportKey)));

            returnProperties.add(new ImportProperty(numberReceiptField, equLM.findLCPByCompoundName("numberReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(dateField, equLM.findLCPByCompoundName("dateReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(timeField, equLM.findLCPByCompoundName("timeReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(discountSumReturnReceiptField, equLM.findLCPByCompoundName("discountSumReturnReceipt").getMapping(receiptKey)));
            returnProperties.add(new ImportProperty(zReportNumberField, equLM.findLCPByCompoundName("zReportReceipt").getMapping(receiptKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("ZReport")).getMapping(zReportKey)));
            returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, equLM.findLCPByCompoundName("seriesNumberObject").getMapping(discountCardKey)));
            returnProperties.add(new ImportProperty(seriesNumberDiscountCardField, equLM.findLCPByCompoundName("discountCardReceipt").getMapping(receiptKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("DiscountCard")).getMapping(discountCardKey)));

            ImportKey<?> receiptReturnDetailKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("ReceiptReturnDetail"), equLM.findLCPByCompoundName("zReportReceiptReceiptDetailToReceiptDetail").getMapping(zReportNumberField, numberReceiptField, numberReceiptDetailField, cashRegisterField));
            returnProperties.add(new ImportProperty(numberReceiptDetailField, equLM.findLCPByCompoundName("numberReceiptDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, equLM.findLCPByCompoundName("idBarcodeReceiptDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(quantityReceiptReturnDetailField, equLM.findLCPByCompoundName("quantityReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(priceReceiptReturnDetailField, equLM.findLCPByCompoundName("priceReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(retailSumReceiptReturnDetailField, equLM.findLCPByCompoundName("sumReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(discountSumReceiptReturnDetailField, equLM.findLCPByCompoundName("discountSumReceiptReturnDetail").getMapping(receiptReturnDetailKey)));
            returnProperties.add(new ImportProperty(numberReceiptField, equLM.findLCPByCompoundName("receiptReceiptDetail").getMapping(receiptReturnDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Receipt")).getMapping(receiptKey)));

            returnProperties.add(new ImportProperty(idBarcodeReceiptDetailField, equLM.findLCPByCompoundName("skuReceiptReturnDetail").getMapping(receiptReturnDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Sku")).getMapping(skuKey)));

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

            ImportKey<?> paymentKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("POS.Payment"), equLM.findLCPByCompoundName("zReportReceiptPaymentToPayment").getMapping(zReportNumberField, numberReceiptField, numberPaymentField, cashRegisterField));
            ImportKey<?> paymentTypeKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("PaymentType"), equLM.findLCPByCompoundName("sidToTypePayment").getMapping(sidTypePaymentField));
            paymentProperties.add(new ImportProperty(sumPaymentField, equLM.findLCPByCompoundName("POS.sumPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(numberPaymentField, equLM.findLCPByCompoundName("numberPayment").getMapping(paymentKey)));
            paymentProperties.add(new ImportProperty(sidTypePaymentField, equLM.findLCPByCompoundName("paymentTypePayment").getMapping(paymentKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("PaymentType")).getMapping(paymentTypeKey)));
            paymentProperties.add(new ImportProperty(numberReceiptField, equLM.findLCPByCompoundName("receiptPayment").getMapping(paymentKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("Receipt")).getMapping(receiptKey)));

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

                DataObject logObject = session.addObject((ConcreteCustomClass) equLM.findClassByCompoundName("EquipmentServerLog"));
                Object equipmentServerObject = equLM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
                equLM.findLCPByCompoundName("equipmentServerEquipmentServerLog").change(equipmentServerObject, session, logObject);
                equLM.findLCPByCompoundName("dataEquipmentServerLog").change(message, session, logObject);
                equLM.findLCPByCompoundName("dateEquipmentServerLog").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            new IntegrationService(session, new ImportTable(paymentImportFields, dataPayment), Arrays.asList(paymentKey, paymentTypeKey, receiptKey, cashRegisterKey),
                    paymentProperties).synchronize(true);

            return session.applyMessage(businessLogics);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public String sendTerminalDocumentInfo(List<TerminalDocumentInfo> terminalDocumentInfoList, String equServerID) throws IOException, SQLException {
        try {
            DataSession session = dbManager.createSession();
            ImportField idTerminalDocumentField = new ImportField(equLM.findLCPByCompoundName("idTerminalDocument"));
            ImportField typeTerminalDocumentField = new ImportField(equLM.findLCPByCompoundName("idTerminalDocumentTypeTerminalDocument"));
            ImportField idTerminalHandbookType1TerminalDocumentField = new ImportField(equLM.findLCPByCompoundName("idTerminalHandbookType1TerminalDocument"));
            ImportField idTerminalHandbookType2TerminalDocumentField = new ImportField(equLM.findLCPByCompoundName("idTerminalHandbookType2TerminalDocument"));
            ImportField titleTerminalDocumentField = new ImportField(equLM.findLCPByCompoundName("titleTerminalDocument"));
            ImportField quantityTerminalDocumentField = new ImportField(equLM.findLCPByCompoundName("quantityTerminalDocument"));

            ImportField numberTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundName("numberTerminalDocumentDetail"));
            ImportField barcodeTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundName("barcodeTerminalDocumentDetail"));
            ImportField nameTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundName("nameTerminalDocumentDetail"));
            ImportField quantityTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundName("quantityTerminalDocumentDetail"));
            ImportField priceTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundName("priceTerminalDocumentDetail"));
            ImportField sumTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundName("sumTerminalDocumentDetail"));

            ImportField isNewTerminalDocumentDetailField = new ImportField(equLM.findLCPByCompoundName("isNewTerminalDocumentDetail"));

            List<ImportProperty<?>> terminalDocumentProperties = new ArrayList<ImportProperty<?>>();
            List<ImportProperty<?>> terminalDocumentDetailProperties = new ArrayList<ImportProperty<?>>();

            ImportKey<?> terminalDocumentKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("TerminalDocument"), equLM.findLCPByCompoundName("terminalDocumentID").getMapping(idTerminalDocumentField));

            terminalDocumentProperties.add(new ImportProperty(idTerminalDocumentField, equLM.findLCPByCompoundName("idTerminalDocument").getMapping(terminalDocumentKey)));
            //terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, equLM.findLCPByCompoundName("typeTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(titleTerminalDocumentField, equLM.findLCPByCompoundName("titleTerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType1TerminalDocumentField, equLM.findLCPByCompoundName("idTerminalHandbookType1TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(idTerminalHandbookType2TerminalDocumentField, equLM.findLCPByCompoundName("idTerminalHandbookType2TerminalDocument").getMapping(terminalDocumentKey)));
            terminalDocumentProperties.add(new ImportProperty(quantityTerminalDocumentField, equLM.findLCPByCompoundName("quantityTerminalDocument").getMapping(terminalDocumentKey)));

            ImportKey<?> terminalDocumentDetailKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("TerminalDocumentDetail"), equLM.findLCPByCompoundName("terminalDocumentDetailIDDocumentIDDetail").getMapping(idTerminalDocumentField, numberTerminalDocumentDetailField));

            terminalDocumentDetailProperties.add(new ImportProperty(numberTerminalDocumentDetailField, equLM.findLCPByCompoundName("numberTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(barcodeTerminalDocumentDetailField, equLM.findLCPByCompoundName("barcodeTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(nameTerminalDocumentDetailField, equLM.findLCPByCompoundName("nameTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(quantityTerminalDocumentDetailField, equLM.findLCPByCompoundName("quantityTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(priceTerminalDocumentDetailField, equLM.findLCPByCompoundName("priceTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(sumTerminalDocumentDetailField, equLM.findLCPByCompoundName("sumTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(isNewTerminalDocumentDetailField, equLM.findLCPByCompoundName("isNewTerminalDocumentDetail").getMapping(terminalDocumentDetailKey)));
            terminalDocumentDetailProperties.add(new ImportProperty(idTerminalDocumentField, equLM.findLCPByCompoundName("terminalDocumentTerminalDocumentDetail").getMapping(terminalDocumentDetailKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("TerminalDocument")).getMapping(terminalDocumentKey)));


            ImportKey<?> terminalDocumentTypeKey = new ImportKey((ConcreteCustomClass) equLM.findClassByCompoundName("TerminalDocumentType"), equLM.findLCPByCompoundName("terminalDocumentTypeID").getMapping(typeTerminalDocumentField));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, equLM.findLCPByCompoundName("idTerminalDocumentType").getMapping(terminalDocumentTypeKey)));
            terminalDocumentProperties.add(new ImportProperty(typeTerminalDocumentField, equLM.findLCPByCompoundName("terminalDocumentTypeTerminalDocument").getMapping(terminalDocumentKey),
                    equLM.baseLM.object(equLM.findClassByCompoundName("TerminalDocumentType")).getMapping(terminalDocumentTypeKey)));

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

                DataObject logObject = session.addObject((ConcreteCustomClass) equLM.findClassByCompoundName("EquipmentServerLog"));
                Object equipmentServerObject = equLM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equServerID, StringClass.get(20)));
                equLM.findLCPByCompoundName("equipmentServerEquipmentServerLog").change(equipmentServerObject, session, logObject);
                equLM.findLCPByCompoundName("dataEquipmentServerLog").change(message, session, logObject);
                equLM.findLCPByCompoundName("dateEquipmentServerLog").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
            }

            return session.applyMessage(businessLogics);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public void succeedTransaction(Integer transactionID) throws RemoteException, SQLException {
        try {
            DataSession session = dbManager.createSession();
            equLM.findLCPByCompoundName("succeededMachineryPriceTransaction").change(true, session,
                    session.getDataObject(equLM.findClassByCompoundName("MachineryPriceTransaction"), transactionID));
            session.apply(businessLogics);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public List<byte[][]> readLabelFormats(List<String> scalesModelsList) throws RemoteException, SQLException {
        try {
            DataSession session = dbManager.createSession();

            List<byte[][]> fileLabelFormats = new ArrayList<byte[][]>();

            for (String scalesModel : scalesModelsList) {

                DataObject scalesModelObject = new DataObject(equLM.findLCPByCompoundName("scalesModelName").read(session, new DataObject(scalesModel)), (ConcreteClass) equLM.findClassByCompoundName("scalesModel"));

                LCP<PropertyInterface> isLabelFormat = (LCP<PropertyInterface>) equLM.is(equLM.findClassByCompoundName("LabelFormat"));

                ImRevMap<PropertyInterface, KeyExpr> labelFormatKeys = isLabelFormat.getMapKeys();
                KeyExpr labelFormatKey = labelFormatKeys.singleValue();
                QueryBuilder<PropertyInterface, Object> labelFormatQuery = new QueryBuilder<PropertyInterface, Object>(labelFormatKeys);

                labelFormatQuery.addProperty("fileLabelFormat", equLM.findLCPByCompoundName("fileLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.addProperty("fileMessageLabelFormat", equLM.findLCPByCompoundName("fileMessageLabelFormat").getExpr(labelFormatKey));
                labelFormatQuery.and(isLabelFormat.property.getExpr(labelFormatKeys).getWhere());
                labelFormatQuery.and(equLM.findLCPByCompoundName("scalesModelLabelFormat").getExpr(labelFormatKey).compare((scalesModelObject).getExpr(), Compare.EQUALS));

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
            DataSession session = dbManager.createSession();
            DataObject errorObject = session.addObject((ConcreteCustomClass) equLM.findClassByCompoundName("MachineryPriceTransactionError"));
            equLM.findLCPByCompoundName("machineryPriceTransactionMachineryPriceTransactionError").change(transactionID, session, errorObject);
            equLM.findLCPByCompoundName("dataMachineryPriceTransactionError").change(e.toString(), session, errorObject);
            equLM.findLCPByCompoundName("dateMachineryPriceTransactionError").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(os));
            equLM.findLCPByCompoundName("errorTraceMachineryPriceTransactionError").change(os.toString(), session, errorObject);

            session.apply(businessLogics);
        } catch (ScriptingErrorLog.SemanticErrorException e2) {
            throw new RuntimeException(e2.toString());
        }
    }

    @Override
    public void errorEquipmentServerReport(String equipmentServer, Throwable exception) throws
            RemoteException, SQLException {
        try {
            DataSession session = dbManager.createSession();
            DataObject errorObject = session.addObject((ConcreteCustomClass) equLM.findClassByCompoundName("EquipmentServerError"));
            Object equipmentServerObject = equLM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
            equLM.findLCPByCompoundName("equipmentServerEquipmentServerError").change(equipmentServerObject, session, errorObject);
            equLM.findLCPByCompoundName("dataEquipmentServerError").change(exception.toString(), session, errorObject);
            OutputStream os = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(os));
            equLM.findLCPByCompoundName("erTraceEquipmentServerError").change(os.toString(), session, errorObject);

            equLM.findLCPByCompoundName("dateEquipmentServerError").change(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);

            session.apply(businessLogics);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e.toString());
        }
    }

    @Override
    public EquipmentServerSettings readEquipmentServerSettings(String equipmentServer) throws RemoteException, SQLException {
        try {
            DataSession session = dbManager.createSession();
            Integer equipmentServerID = (Integer) equLM.findLCPByCompoundName("sidToEquipmentServer").read(session, new DataObject(equipmentServer, StringClass.get(20)));
            Integer delay = (Integer) equLM.findLCPByCompoundName("delayEquipmentServer").read(session, new DataObject(equipmentServerID, (ConcreteClass) equLM.findClassByCompoundName("equipmentServer")));
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
