package retail;

import net.sf.jasperreports.engine.JRException;

import java.io.*;
import java.sql.Timestamp;

import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.auth.SecurityPolicy;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import platform.server.session.Modifier;
import retail.api.remote.*;

import java.rmi.RemoteException;
import java.sql.SQLException;
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
        retailLM = new ScriptingLogicsModule(getClass().getResourceAsStream("/scripts/retail.lsf"), LM, this);
        addLogicsModule(retailLM);
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        policyManager.userPolicies.put(addUser("admin", "fusion").ID, new ArrayList<SecurityPolicy>(Arrays.asList(permitAllPolicy, allowConfiguratorPolicy)));
    }

    @Override
    public BusinessLogics getBL() {
        return this;
    }

    @Override
    public List<TransactionInfo> readTransactionInfo(String equServerID) throws SQLException {
        DataSession session = getBL().createSession();
        List<TransactionInfo> transactionList = new ArrayList<TransactionInfo>();

        LP isMachineryPriceTransaction = retailLM.is(retailLM.getClassByName("machineryPriceTransaction"));
        Map<Object, KeyExpr> keys = isMachineryPriceTransaction.getMapKeys();
        KeyExpr key = BaseUtils.singleValue(keys);
        Query<Object, Object> query = new Query<Object, Object>(keys);

        query.properties.put("dateTimeMPT", retailLM.getLPByName("dateTimeMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
        query.properties.put("groupMachineryMPT", retailLM.getLPByName("groupMachineryMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));
        query.properties.put("snapshotMPT", retailLM.getLPByName("snapshotMachineryPriceTransaction").getExpr(BaseUtils.singleValue(keys)));

        query.and(retailLM.getLPByName("sidEquipmentServerMachineryPriceTransaction").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));
        query.and(retailLM.getLPByName("processMachineryPriceTransaction").getExpr(key).getWhere());

        OrderedMap<Map<Object, DataObject>, Map<Object, ObjectValue>> result = query.executeClasses(session);
        List<Object[]> transactionObjects = new ArrayList<Object[]>();
        for (Map.Entry<Map<Object, DataObject>, Map<Object, ObjectValue>> entry : result.entrySet()) {
            DataObject dateTimeMPT = (DataObject) entry.getValue().get("dateTimeMPT");
            String groupMachineryMPT = entry.getValue().get("groupMachineryMPT").getValue().toString().trim();
            DataObject transactionObject = entry.getKey().values().iterator().next();
            Boolean snapshotMPT = entry.getValue().get("snapshotMPT") instanceof DataObject;
            transactionObjects.add(new Object[]{groupMachineryMPT, transactionObject, dateTimeCode((Timestamp) dateTimeMPT.getValue()), snapshotMPT});
        }

        List<ItemInfo> itemTransactionList;
        for (Object[] transaction : transactionObjects) {

            String groupID = (String) transaction[0];
            DataObject transactionObject = (DataObject) transaction[1];
            String dateTimeCode = (String) transaction[2];
            Boolean snapshotTransaction = (Boolean) transaction[3];

            itemTransactionList = new ArrayList<ItemInfo>();
            KeyExpr barcodeExpr = new KeyExpr("barcode");
            Map<Object, KeyExpr> itemKeys = new HashMap<Object, KeyExpr>();
            itemKeys.put("barcode", barcodeExpr);

            Query<Object, Object> itemQuery = new Query<Object, Object>(itemKeys);
            itemQuery.properties.put("barcodeEx", retailLM.getLPByName("barcodeEx").getExpr(barcodeExpr));
            itemQuery.properties.put("nameMachineryPriceTransactionBarcode", retailLM.getLPByName("nameMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));
            itemQuery.properties.put("priceMachineryPriceTransactionBarcode", retailLM.getLPByName("priceMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr));

            itemQuery.and(retailLM.getLPByName("inMachineryPriceTransactionBarcode").getExpr(transactionObject.getExpr(), barcodeExpr).getWhere());

            OrderedMap<Map<Object, Object>, Map<Object, Object>> itemResult = itemQuery.execute(session.sql);

            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> entry : itemResult.entrySet()) {
                String barcode = (String) entry.getValue().get("barcodeEx");
                String name = (String) entry.getValue().get("nameMachineryPriceTransactionBarcode");
                Double price = (Double) entry.getValue().get("priceMachineryPriceTransactionBarcode");
                itemTransactionList.add(new ItemInfo(barcode.trim(), name.trim(), price));
            }

            if (transactionObject.objectClass.equals(retailLM.getClassByName("cashRegisterPriceTransaction"))) {
                List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();
                LP isCashRegister = LM.is(retailLM.getClassByName("cashRegister"));

                Map<Object, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                KeyExpr cashRegisterKey = BaseUtils.singleValue(cashRegisterKeys);
                Query<Object, Object> cashRegisterQuery = new Query<Object, Object>(cashRegisterKeys);

                cashRegisterQuery.properties.put("directoryCashRegister", retailLM.getLPByName("directoryCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("portMachinery", retailLM.getLPByName("portMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("nppMachinery", retailLM.getLPByName("nppMachinery").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("nameCashRegisterModelCashRegister", retailLM.getLPByName("nameCashRegisterModelCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("handlerCashRegisterModelCashRegister", retailLM.getLPByName("handlerCashRegisterModelCashRegister").getExpr(cashRegisterKey));

                cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                cashRegisterQuery.and(retailLM.getLPByName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare(new DataObject(groupID, (ConcreteClass) retailLM.getClassByName("groupCashRegister")), Compare.EQUALS));
                if (snapshotTransaction)
                    cashRegisterQuery.and(retailLM.getLPByName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), cashRegisterKey).getWhere());

                OrderedMap<Map<Object, Object>, Map<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                for (Map<Object, Object> values : cashRegisterResult.values()) {
                    String directoryCashRegister = (String) values.get("directoryCashRegister");
                    String portMachinery = (String) values.get("portMachinery");
                    Integer nppMachinery = (Integer) values.get("nppMachinery");
                    String nameModel = (String) values.get("nameCashRegisterModelCashRegister");
                    String handlerModel = (String) values.get("handlerCashRegisterModelCashRegister");
                    cashRegisterInfoList.add(new CashRegisterInfo(nppMachinery, nameModel, handlerModel, portMachinery, directoryCashRegister));
                }

                transactionList.add(new TransactionCashRegisterInfo(groupID, (Integer) transactionObject.getValue(),
                        dateTimeCode, itemTransactionList, cashRegisterInfoList));

            } else if (transactionObject.objectClass.equals(retailLM.getClassByName("scalesPriceTransaction"))) {
                List<ScalesInfo> scalesInfoList = new ArrayList<ScalesInfo>();
                String directory = (String) retailLM.getLPByName("directoryGroupScales").read(session, new DataObject(groupID, (ConcreteClass) retailLM.getClassByName("groupScales")));

                LP isScales = LM.is(retailLM.getClassByName("scales"));

                Map<Object, KeyExpr> scalesKeys = isScales.getMapKeys();
                KeyExpr scalesKey = BaseUtils.singleValue(scalesKeys);
                Query<Object, Object> scalesQuery = new Query<Object, Object>(scalesKeys);

                scalesQuery.properties.put("portMachinery", retailLM.getLPByName("portMachinery").getExpr(scalesKey));
                scalesQuery.properties.put("nppMachinery", retailLM.getLPByName("nppMachinery").getExpr(scalesKey));
                scalesQuery.properties.put("nameScalesModelScales", retailLM.getLPByName("nameScalesModelScales").getExpr(scalesKey));
                scalesQuery.properties.put("handlerScalesModelScales", retailLM.getLPByName("handlerScalesModelScales").getExpr(scalesKey));
                scalesQuery.and(isScales.property.getExpr(scalesKeys).getWhere());
                scalesQuery.and(retailLM.getLPByName("groupScalesScales").getExpr(scalesKey).compare(new DataObject(groupID, (ConcreteClass) retailLM.getClassByName("groupScales")), Compare.EQUALS));
                if (snapshotTransaction)
                    scalesQuery.and(retailLM.getLPByName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), scalesKey).getWhere());

                OrderedMap<Map<Object, Object>, Map<Object, Object>> scalesResult = scalesQuery.execute(session.sql);

                for (Map<Object, Object> values : scalesResult.values()) {
                    String portMachinery = (String) values.get("portMachinery");
                    Integer nppMachinery = (Integer) values.get("nppMachinery");
                    String nameModel = (String) values.get("nameScalesModelScales");
                    String handlerModel = (String) values.get("handlerScalesModelScales");
                    scalesInfoList.add(new ScalesInfo(nppMachinery, nameModel, handlerModel, portMachinery, directory));
                }

                transactionList.add(new TransactionScalesInfo(groupID, (Integer) transactionObject.getValue(),
                        dateTimeCode, itemTransactionList, scalesInfoList));

            } else if (transactionObject.objectClass.equals(retailLM.getClassByName("checkPriceTransaction"))) {
                List<PriceCheckerInfo> priceCheckerInfoList = new ArrayList<PriceCheckerInfo>();
                LP isCheck = LM.is(retailLM.getClassByName("check"));

                Map<Object, KeyExpr> checkKeys = isCheck.getMapKeys();
                KeyExpr checkKey = BaseUtils.singleValue(checkKeys);
                Query<Object, Object> checkQuery = new Query<Object, Object>(checkKeys);

                checkQuery.properties.put("portMachinery", retailLM.getLPByName("portMachinery").getExpr(checkKey));
                checkQuery.properties.put("nppMachinery", retailLM.getLPByName("nppMachinery").getExpr(checkKey));
                checkQuery.properties.put("nameCheckModelCheck", retailLM.getLPByName("nameCheckModelCheck").getExpr(checkKey));
                //checkQuery.properties.put("handlerCheckModelCheck", retailLM.getLPByName("handlerCheckModelCheck").getExpr(checkKey));
                checkQuery.and(isCheck.property.getExpr(checkKeys).getWhere());
                checkQuery.and(retailLM.getLPByName("groupCheckCheck").getExpr(checkKey).compare(new DataObject(groupID, (ConcreteClass) retailLM.getClassByName("groupCheck")), Compare.EQUALS));

                if (snapshotTransaction)
                    checkQuery.and(retailLM.getLPByName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), checkKey).getWhere());

                OrderedMap<Map<Object, Object>, Map<Object, Object>> checkResult = checkQuery.execute(session.sql);

                for (Map<Object, Object> values : checkResult.values()) {
                    String portMachinery = (String) values.get("portMachinery");
                    Integer nppMachinery = (Integer) values.get("nppMachinery");
                    String nameModel = (String) values.get("nameCheckModelCheck");
                    //String handlerModel = (String) values.get("handlerCheckModelCheck");
                    String handlerModel = null;
                    priceCheckerInfoList.add(new PriceCheckerInfo(nppMachinery, nameModel, handlerModel, portMachinery));
                }
                transactionList.add(new TransactionPriceCheckerInfo(groupID, (Integer) transactionObject.getValue(),
                        dateTimeCode, itemTransactionList, priceCheckerInfoList));
            }

        }
        return transactionList;
    }

    @Override
    public void succeedTransaction(Integer transactionID) throws SQLException {
        DataSession session = getBL().createSession();
        retailLM.getLPByName("succeededMachineryPriceTransaction").execute(true, session,
                session.getDataObject(transactionID, retailLM.getClassByName("machineryPriceTransaction").getType()));
        session.apply(getBL());
    }

    @Override
    public void errorReport(Integer transactionID, Exception e) throws RemoteException, SQLException {
        DataSession session = getBL().createSession();
        DataObject errorObject = session.addObject((ConcreteCustomClass) retailLM.getClassByName("machineryPriceTransactionError"), session.modifier);
        retailLM.getLPByName("machineryPriceTransactionMachineryPriceTransactionError").execute(transactionID, session, errorObject);
        retailLM.getLPByName("dataMachineryPriceTransactionError").execute(e.toString(), session, errorObject);
        retailLM.getLPByName("dateMachineryPriceTransactionError").execute(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);
        OutputStream os = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(os));
        retailLM.getLPByName("errorTraceMachineryPriceTransactionError").execute(os.toString(), session, errorObject);

        session.apply(getBL());
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

