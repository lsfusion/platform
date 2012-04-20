package retail;

import net.sf.jasperreports.engine.JRException;

import java.io.*;
import java.sql.Time;
import java.sql.Timestamp;

import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.DateConverter;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.auth.SecurityPolicy;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.sql.DataAdapter;
import platform.server.integration.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import retail.api.remote.*;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.ParseException;
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
                cashRegisterQuery.properties.put("numberCashRegister", retailLM.getLPByName("numberCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("nameCashRegisterModelCashRegister", retailLM.getLPByName("nameCashRegisterModelCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.properties.put("handlerCashRegisterModelCashRegister", retailLM.getLPByName("handlerCashRegisterModelCashRegister").getExpr(cashRegisterKey));

                cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                cashRegisterQuery.and(retailLM.getLPByName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare(new DataObject(groupID, (ConcreteClass) retailLM.getClassByName("groupCashRegister")), Compare.EQUALS));
                if (snapshotTransaction)
                    cashRegisterQuery.and(retailLM.getLPByName("inMachineryPriceTransactionMachinery").getExpr(transactionObject.getExpr(), cashRegisterKey).getWhere());

                OrderedMap<Map<Object, Object>, Map<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                for (Map.Entry<Map<Object, Object>, Map<Object, Object>> values : cashRegisterResult.entrySet()) {

                    String directoryCashRegister = (String) values.getValue().get("directoryCashRegister");
                    String portMachinery = (String) values.getValue().get("portMachinery");
                    Integer nppMachinery = (Integer) values.getValue().get("nppMachinery");
                    String numberCashRegister = (String) values.getValue().get("numberCashRegister");
                    String nameModel = (String) values.getValue().get("nameCashRegisterModelCashRegister");
                    String handlerModel = (String) values.getValue().get("handlerCashRegisterModelCashRegister");
                    cashRegisterInfoList.add(new CashRegisterInfo(nppMachinery, numberCashRegister, nameModel, handlerModel, portMachinery, directoryCashRegister));
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
    public List<CashRegisterInfo> readCashRegisterInfo(String equServerID) throws RemoteException, SQLException {
        DataSession session = getBL().createSession();
        List<CashRegisterInfo> cashRegisterInfoList = new ArrayList<CashRegisterInfo>();

        LP isGroupMachinery = retailLM.is(retailLM.getClassByName("groupMachinery"));
        Map<Object, KeyExpr> keys = isGroupMachinery.getMapKeys();
        KeyExpr key = BaseUtils.singleValue(keys);
        Query<Object, Object> query = new Query<Object, Object>(keys);
        query.and(retailLM.getLPByName("sidEquipmentServerGroupMachinery").getExpr(key).compare(new DataObject(equServerID, StringClass.get(20)), Compare.EQUALS));

        OrderedMap<Map<Object, DataObject>, Map<Object, ObjectValue>> result = query.executeClasses(session);
        List<Object> groupMachineryObjects = new ArrayList<Object>();
        for (Map.Entry<Map<Object, DataObject>, Map<Object, ObjectValue>> entry : result.entrySet()) {
            DataObject groupMachineryObject = entry.getKey().values().iterator().next();
            groupMachineryObjects.add(groupMachineryObject);
        }

        for (Object groupMachinery : groupMachineryObjects) {
            LP isCashRegister = LM.is(retailLM.getClassByName("cashRegister"));

            Map<Object, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
            KeyExpr cashRegisterKey = BaseUtils.singleValue(cashRegisterKeys);
            Query<Object, Object> cashRegisterQuery = new Query<Object, Object>(cashRegisterKeys);

            cashRegisterQuery.properties.put("directoryCashRegister", retailLM.getLPByName("directoryCashRegister").getExpr(cashRegisterKey));
            cashRegisterQuery.properties.put("portMachinery", retailLM.getLPByName("portMachinery").getExpr(cashRegisterKey));
            cashRegisterQuery.properties.put("nppMachinery", retailLM.getLPByName("nppMachinery").getExpr(cashRegisterKey));
            cashRegisterQuery.properties.put("numberCashRegister", retailLM.getLPByName("numberCashRegister").getExpr(cashRegisterKey));
            cashRegisterQuery.properties.put("nameCashRegisterModelCashRegister", retailLM.getLPByName("nameCashRegisterModelCashRegister").getExpr(cashRegisterKey));
            cashRegisterQuery.properties.put("handlerCashRegisterModelCashRegister", retailLM.getLPByName("handlerCashRegisterModelCashRegister").getExpr(cashRegisterKey));

            cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
            cashRegisterQuery.and(retailLM.getLPByName("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare(((DataObject) groupMachinery).getExpr(), Compare.EQUALS));

            OrderedMap<Map<Object, Object>, Map<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> values : cashRegisterResult.entrySet()) {

                String directoryCashRegister = (String) values.getValue().get("directoryCashRegister");
                String portMachinery = (String) values.getValue().get("portMachinery");
                Integer nppMachinery = (Integer) values.getValue().get("nppMachinery");
                String numberCashRegister = (String) values.getValue().get("numberCashRegister");
                String nameModel = (String) values.getValue().get("nameCashRegisterModelCashRegister");
                String handlerModel = (String) values.getValue().get("handlerCashRegisterModelCashRegister");
                cashRegisterInfoList.add(new CashRegisterInfo(nppMachinery, numberCashRegister, nameModel, handlerModel, portMachinery, directoryCashRegister));
            }
        }
        return cashRegisterInfoList;
    }

    @Override
    public String sendSalesInfo(List<SalesInfo> salesInfoList, String equipmentServer) throws IOException, SQLException {

        DataSession session = getBL().createSession();
        ImportField cashRegisterField = new ImportField(retailLM.getLPByName("numberCashRegister"));
        ImportField zReportNumberField = new ImportField(retailLM.getLPByName("numberZReport"));

        ImportField numberBillField = new ImportField(retailLM.getLPByName("numberBill"));
        ImportField dateField = new ImportField(retailLM.getLPByName("dateBill"));
        ImportField timeField = new ImportField(retailLM.getLPByName("timeBill"));

        ImportField numberBillDetailField = new ImportField(retailLM.getLPByName("numberBillDetail"));
        ImportField barcodeExBillDetailField = new ImportField(retailLM.getLPByName("barcodeExBillDetail"));
        ImportField quantityBillDetailField = new ImportField(retailLM.getLPByName("quantityBillDetail"));
        ImportField priceBillDetailField = new ImportField(retailLM.getLPByName("priceBillDetail"));
        ImportField sumBillDetailField = new ImportField(retailLM.getLPByName("sumBillDetail"));
        ImportField discountSumBillDetailField = new ImportField(retailLM.getLPByName("discountSumBillDetail"));

        ImportField sidTypePaymentField = new ImportField(retailLM.getLPByName("sidPaymentType"));
        ImportField sumPaymentField = new ImportField(retailLM.getLPByName("sumPayment"));
        ImportField numberPaymentField = new ImportField(retailLM.getLPByName("numberPayment"));

        List<ImportProperty<?>> properties = new ArrayList<ImportProperty<?>>();
        List<ImportProperty<?>> paymentProperties = new ArrayList<ImportProperty<?>>();

        ImportKey<?> zReportKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("zReport"), retailLM.getLPByName("numberToZReport").getMapping(zReportNumberField));
        ImportKey<?> cashRegisterKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("cashRegister"), retailLM.getLPByName("numberCashRegisterToCashRegister").getMapping(cashRegisterField));

        properties.add(new ImportProperty(zReportNumberField, retailLM.getLPByName("numberZReport").getMapping(zReportKey)));
        properties.add(new ImportProperty(cashRegisterField, retailLM.getLPByName("cashRegisterZReport").getMapping(zReportKey),
                LM.baseLM.object(retailLM.getClassByName("cashRegister")).getMapping(cashRegisterKey)));
        properties.add(new ImportProperty(dateField, retailLM.getLPByName("dateOpenZReport").getMapping(zReportKey)));

        ImportKey<?> billKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("bill"), retailLM.getLPByName("zReportBillToBill").getMapping(zReportNumberField, numberBillField));
        properties.add(new ImportProperty(numberBillField, retailLM.getLPByName("numberBill").getMapping(billKey)));
        properties.add(new ImportProperty(dateField, retailLM.getLPByName("dateBill").getMapping(billKey)));
        properties.add(new ImportProperty(timeField, retailLM.getLPByName("timeBill").getMapping(billKey)));
        properties.add(new ImportProperty(zReportNumberField, retailLM.getLPByName("zReportBill").getMapping(billKey),
                LM.baseLM.object(retailLM.getClassByName("zReport")).getMapping(zReportKey)));

        ImportKey<?> billDetailKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("billDetail"), retailLM.getLPByName("zReportBillBillDetailToBillDetail").getMapping(zReportNumberField, numberBillField, numberBillDetailField));
        properties.add(new ImportProperty(numberBillDetailField, retailLM.getLPByName("numberBillDetail").getMapping(billDetailKey)));
        properties.add(new ImportProperty(barcodeExBillDetailField, retailLM.getLPByName("barcodeExBillDetail").getMapping(billDetailKey)));
        properties.add(new ImportProperty(quantityBillDetailField, retailLM.getLPByName("quantityBillDetail").getMapping(billDetailKey)));
        properties.add(new ImportProperty(priceBillDetailField, retailLM.getLPByName("priceBillDetail").getMapping(billDetailKey)));
        properties.add(new ImportProperty(sumBillDetailField, retailLM.getLPByName("sumBillDetail").getMapping(billDetailKey)));
        properties.add(new ImportProperty(discountSumBillDetailField, retailLM.getLPByName("discountSumBillDetail").getMapping(billDetailKey)));
        properties.add(new ImportProperty(numberBillField, retailLM.getLPByName("billBillDetail").getMapping(billDetailKey),
                LM.baseLM.object(retailLM.getClassByName("bill")).getMapping(billKey)));

        ImportKey<?> itemKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("item"), retailLM.getLPByName("skuBarcodeStringDate").getMapping(barcodeExBillDetailField, dateField));
        properties.add(new ImportProperty(barcodeExBillDetailField, retailLM.getLPByName("itemBillDetail").getMapping(billDetailKey),
                LM.baseLM.object(retailLM.getClassByName("item")).getMapping(itemKey)));

        List<List<Object>> data = new ArrayList<List<Object>>();
        List<List<Object>> paymentData = new ArrayList<List<Object>>();

        for (SalesInfo sale : salesInfoList) {
            data.add(Arrays.<Object>asList(sale.cashRegisterNumber, sale.zReportNumber, sale.date, sale.time, sale.billNumber,
                    sale.numberBillDetail, sale.barcodeItem, sale.quantityBillDetail, sale.priceBillDetail, sale.sumBillDetail,
                    sale.discountSumBillDetail));
            paymentData.add(Arrays.<Object>asList(sale.zReportNumber, sale.billNumber, "cash", sale.sumBill, 1));
        }

        List<ImportField> importFields = Arrays.asList(cashRegisterField, zReportNumberField, dateField, timeField,
                numberBillField, numberBillDetailField, barcodeExBillDetailField, quantityBillDetailField,
                priceBillDetailField, sumBillDetailField, discountSumBillDetailField);
        new IntegrationService(session, new ImportTable(importFields, data), Arrays.asList(zReportKey, cashRegisterKey, billKey, billDetailKey, itemKey),
                properties).synchronize(true);

        ImportKey<?> paymentKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("payment"), retailLM.getLPByName("zReportBillPaymentToPayment").getMapping(zReportNumberField, numberBillField, numberPaymentField));
        ImportKey<?> paymentTypeKey = new ImportKey((ConcreteCustomClass) retailLM.getClassByName("paymentType"), retailLM.getLPByName("sidToTypePayment").getMapping(sidTypePaymentField));
        paymentProperties.add(new ImportProperty(sumPaymentField, retailLM.getLPByName("sumPayment").getMapping(paymentKey)));
        paymentProperties.add(new ImportProperty(numberPaymentField, retailLM.getLPByName("numberPayment").getMapping(paymentKey)));
        paymentProperties.add(new ImportProperty(sidTypePaymentField, retailLM.getLPByName("paymentTypePayment").getMapping(paymentKey),
                LM.baseLM.object(retailLM.getClassByName("paymentType")).getMapping(paymentTypeKey)));
        paymentProperties.add(new ImportProperty(numberBillField, retailLM.getLPByName("billPayment").getMapping(paymentKey),
                LM.baseLM.object(retailLM.getClassByName("bill")).getMapping(billKey)));

        List<ImportField> paymentImportFields = Arrays.asList(zReportNumberField, numberBillField, sidTypePaymentField,
                sumPaymentField, numberPaymentField);

        if (salesInfoList.size() != 0) {
            String message = "Загружено записей: " + data.size();
            List<String> cashRegisterNumbers = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            for(SalesInfo salesInfo : salesInfoList) {
                if(!cashRegisterNumbers.contains(salesInfo.cashRegisterNumber.trim()))
                    cashRegisterNumbers.add(salesInfo.cashRegisterNumber.trim());
                if(!fileNames.contains(salesInfo.filename.trim()))
                    fileNames.add(salesInfo.filename.trim());
            }
            message += "\nИз касс: ";
            for(String cashRegisterNumber : cashRegisterNumbers)
                message += cashRegisterNumber + ", ";
            message = message.substring(0, message.length()-2);

            message += "\nИз файлов: ";
            for(String filename : fileNames)
                message += filename + ", ";
            message = message.substring(0, message.length()-2);

            DataObject logObject = session.addObject((ConcreteCustomClass) retailLM.getClassByName("equipmentServerLog"), session.modifier);
            Object equipmentServerObject = retailLM.getLPByName("sidToEquipmentServer").read(session, session.modifier, new DataObject(equipmentServer, StringClass.get(20)));
            retailLM.getLPByName("equipmentServerEquipmentServerLog").execute(equipmentServerObject, session, logObject);
            retailLM.getLPByName("dataEquipmentServerLog").execute(message, session, logObject);
            retailLM.getLPByName("dateEquipmentServerLog").execute(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, logObject);
        }

        new IntegrationService(session, new ImportTable(paymentImportFields, paymentData), Arrays.asList(paymentKey, paymentTypeKey, billKey),
                paymentProperties).synchronize(true);

        return session.apply(getBL());
    }

    @Override
    public void succeedTransaction(Integer transactionID) throws SQLException {
        DataSession session = getBL().createSession();
        retailLM.getLPByName("succeededMachineryPriceTransaction").execute(true, session,
                session.getDataObject(transactionID, retailLM.getClassByName("machineryPriceTransaction").getType()));
        session.apply(getBL());
    }

    @Override
    public void errorTransactionReport(Integer transactionID, Exception e) throws RemoteException, SQLException {
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

    @Override
    public void errorEquipmentServerReport(String equipmentServer, String message) throws RemoteException, SQLException {
        DataSession session = getBL().createSession();
        DataObject errorObject = session.addObject((ConcreteCustomClass) retailLM.getClassByName("equipmentServerError"), session.modifier);
        Object equipmentServerObject = retailLM.getLPByName("sidToEquipmentServer").read(session, session.modifier, new DataObject(equipmentServer, StringClass.get(20)));
        retailLM.getLPByName("equipmentServerEquipmentServerError").execute(equipmentServerObject, session, errorObject);
        retailLM.getLPByName("dataEquipmentServerError").execute(message, session, errorObject);
        retailLM.getLPByName("dateEquipmentServerError").execute(DateConverter.dateToStamp(Calendar.getInstance().getTime()), session, errorObject);

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

