package retail.actions;

import org.apache.commons.lang.time.DateUtils;
import org.apache.poi.util.Internal;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.interop.action.MessageClientAction;
import platform.server.classes.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import retail.RetailBusinessLogics;
import retail.api.remote.SalesInfo;
import retail.api.remote.TerminalDocumentTypeInfo;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.Date;

public class GenerateZReport extends ScriptingActionProperty {
    public GenerateZReport(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {

        DataSession session = context.getSession();
        List<SalesInfo> salesInfoList = new ArrayList<SalesInfo>();
        try {
            Random r = new Random();
            Integer zReportCount = addDeviation((Integer) getLP("averageZReportCountGenerateZReport").read(session, session.modifier), 0.25, r);
            Integer billCount = (Integer) getLP("averageBillCountGenerateZReport").read(session, session.modifier);
            Integer billDetailCount = (Integer) getLP("averageBillDetailCountGenerateZReport").read(session, session.modifier);
            Date dateFrom = (Date) getLP("dateFromGenerateZReport").read(session, session.modifier);
            dateFrom = dateFrom == null ? new Date(System.currentTimeMillis()) : dateFrom;
            Date dateTo = (Date) getLP("dateToGenerateZReport").read(session, session.modifier);
            dateTo = dateTo == null ? new Date(System.currentTimeMillis()) : dateTo;

            KeyExpr departmentStoreExpr = new KeyExpr("departmentStore");
            KeyExpr itemExpr = new KeyExpr("item");
            Map<Object, KeyExpr> newKeys = new HashMap<Object, KeyExpr>();
            newKeys.put("departmentStore", departmentStoreExpr);
            newKeys.put("item", itemExpr);

            Query<Object, Object> query = new Query<Object, Object>(newKeys);
            query.properties.put("currentBalanceSkuLedger", getLP("currentBalanceSkuLedger").getExpr(itemExpr, departmentStoreExpr));
            query.properties.put("currentRetailPriceLedger", getLP("currentRetailPriceLedger").getExpr(itemExpr, departmentStoreExpr));
            query.and(getLP("currentBalanceSkuLedger").getExpr(itemExpr, departmentStoreExpr).getWhere());

            OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);

            List<ItemZReportInfo> itemZReportInfoList = new ArrayList<ItemZReportInfo>();
            List<Integer> departmentStoreList = new ArrayList<Integer>();
            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> rows : result.entrySet()) {
                DataObject itemObject = new DataObject(rows.getKey().get("item"), (ConcreteClass) getClass("item"));
                Double currentBalanceSkuLedger = (Double) rows.getValue().get("currentBalanceSkuLedger");
                if (currentBalanceSkuLedger > 0) {
                    Integer departmentStore = (Integer) rows.getKey().get("departmentStore");
                    if ((departmentStore != null) && (!departmentStoreList.contains(departmentStore)))
                        departmentStoreList.add(departmentStore);
                    Double currentRetailPriceLedger = (Double) rows.getValue().get("currentRetailPriceLedger");
                    String barcodeItem = (String) getLP("idBarcodeSku").read(session, session.modifier, itemObject);
                    itemZReportInfoList.add(new ItemZReportInfo(barcodeItem, currentBalanceSkuLedger, currentRetailPriceLedger, departmentStore));
                }
            }

            Map<Integer, Integer> groupCashRegisterDepartmentStoreMap = new HashMap<Integer, Integer>();
            for (Integer departmentStore : departmentStoreList) {

                LP isGroupCashRegister = LM.is(getClass("groupCashRegister"));

                Map<Object, KeyExpr> groupCashRegisterKeys = isGroupCashRegister.getMapKeys();
                KeyExpr groupCashRegisterKey = BaseUtils.singleValue(groupCashRegisterKeys);
                Query<Object, Object> groupCashRegisterQuery = new Query<Object, Object>(groupCashRegisterKeys);
                groupCashRegisterQuery.and(isGroupCashRegister.property.getExpr(groupCashRegisterKeys).getWhere());
                groupCashRegisterQuery.and(getLP("departmentStoreGroupMachinery").getExpr(groupCashRegisterKey).compare((new DataObject(departmentStore, (ConcreteClass) getClass("departmentStore"))).getExpr(), Compare.EQUALS));

                OrderedMap<Map<Object, Object>, Map<Object, Object>> groupCashRegisterResult = groupCashRegisterQuery.execute(session.sql);

                for (Map.Entry<Map<Object, Object>, Map<Object, Object>> rows : groupCashRegisterResult.entrySet()) {
                    Integer groupCashRegister = (Integer) rows.getKey().entrySet().iterator().next().getValue();
                    if ((groupCashRegister != null) && (!groupCashRegisterDepartmentStoreMap.containsKey(groupCashRegister)))
                        groupCashRegisterDepartmentStoreMap.put(groupCashRegister, departmentStore);
                }
            }

            Map<String, Integer> numberCashRegisterDepartmentStoreMap = new HashMap<String, Integer>();
            for (Map.Entry<Integer, Integer> groupCashRegisterDepartmentStore : groupCashRegisterDepartmentStoreMap.entrySet()) {

                LP isCashRegister = LM.is(getClass("cashRegister"));

                Map<Object, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                KeyExpr cashRegisterKey = BaseUtils.singleValue(cashRegisterKeys);
                Query<Object, Object> cashRegisterQuery = new Query<Object, Object>(cashRegisterKeys);
                cashRegisterQuery.properties.put("numberCashRegister", getLP("numberCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                cashRegisterQuery.and(getLP("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare((new DataObject(groupCashRegisterDepartmentStore.getKey(), (ConcreteClass) getClass("groupCashRegister"))).getExpr(), Compare.EQUALS));

                OrderedMap<Map<Object, Object>, Map<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                for (Map<Object, Object> values : cashRegisterResult.values()) {
                    String numberCashRegister = (String) values.get("numberCashRegister");
                    if ((numberCashRegister != null) && (!numberCashRegisterDepartmentStoreMap.containsKey(numberCashRegister)))
                        numberCashRegisterDepartmentStoreMap.put(numberCashRegister, groupCashRegisterDepartmentStore.getValue());
                }
            }

            if (!numberCashRegisterDepartmentStoreMap.isEmpty()) {
                Map<Integer, String> numberZReportCashRegisterMap = new HashMap<Integer, String>();

                for (int z = 1; z <= zReportCount; z++) {

                    Map.Entry<String, Integer> numberCashRegisterDepartmentStore = (Map.Entry<String, Integer>) (numberCashRegisterDepartmentStoreMap.entrySet().toArray()[r.nextInt(numberCashRegisterDepartmentStoreMap.size())/*1*/]);
                    String numberCashRegister = numberCashRegisterDepartmentStore.getKey();
                    Integer departmentStore = numberCashRegisterDepartmentStore.getValue();
                    Integer maxNumberZReport = (Integer) getLP("maxNumberZReport").read(session, session.modifier, new DataObject(getLP("numberCashRegisterToCashRegister").read(session, session.modifier, new DataObject(numberCashRegisterDepartmentStore.getKey(), StringClass.get(100))), (ConcreteClass) getClass("cashRegister")));
                    Integer numberZReport = null;
                    while (numberZReport == null || (numberZReportCashRegisterMap.containsKey(numberZReport) && numberZReportCashRegisterMap.containsValue(numberCashRegister)))
                        numberZReport = (maxNumberZReport == null ? 0 : maxNumberZReport) + (zReportCount < 1 ? 0 : r.nextInt(zReportCount)) + 1;
                    if (!numberZReportCashRegisterMap.containsKey(numberZReport))
                        numberZReportCashRegisterMap.put(numberZReport, numberCashRegister);
                    java.sql.Date date = new java.sql.Date(dateFrom.getTime() + Math.abs(r.nextLong() % (dateTo.getTime() - dateFrom.getTime())));
                    for (int billNumber = 1; billNumber <= addDeviation(billCount, 0.25, r); billNumber++) {

                        Integer numberBillDetail = 0;
                        Double sumBill = 0.0;
                        List<SalesInfo> billSalesInfoList = new ArrayList<SalesInfo>();

                        Time time = new Time(r.nextLong() % date.getTime());
                        Integer currentBillDetailCount = addDeviation(billDetailCount, 0.25, r);
                        for (ItemZReportInfo itemZReportInfo : itemZReportInfoList) {
                            Double currentBalanceSkuLedger = itemZReportInfo.count;
                            if ((currentBalanceSkuLedger > 0) && (departmentStore.equals(itemZReportInfo.departmentStore))) {
                                Double quantityBillDetail = (double) r.nextInt((int) Math.ceil(currentBalanceSkuLedger / 5));
                                if ((quantityBillDetail > 0) && (currentBillDetailCount >= numberBillDetail)) {
                                    Double sumBillDetail = quantityBillDetail * (itemZReportInfo.price == null ? 0 : itemZReportInfo.price);
                                    numberBillDetail++;
                                    sumBill += sumBillDetail;
                                    SalesInfo salesInfo = new SalesInfo(numberCashRegister, numberZReport,
                                            billNumber, date, time, null, itemZReportInfo.barcode == null ? null : itemZReportInfo.barcode.trim(),
                                            quantityBillDetail, itemZReportInfo.price, sumBillDetail, 0.0, numberBillDetail, null);
                                    billSalesInfoList.add(salesInfo);
                                    itemZReportInfo.count -= quantityBillDetail;
                                    billSalesInfoList.add(salesInfo);
                                    itemZReportInfo.count -= quantityBillDetail;
                                }
                            }
                        }
                        for (SalesInfo s : billSalesInfoList) {
                            s.sumBill = sumBill;
                        }
                        salesInfoList.addAll(billSalesInfoList);
                    }
                }
            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
        try {
            String res = ((RetailBusinessLogics) LM.getBL()).sendSalesInfo(salesInfoList, "equServer1");
            if (res != null) {
                throw new RuntimeException(res);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Integer addDeviation(Integer value, Double percent, Random r) {
        return value != null ? value + (int) (value * r.nextDouble() * percent * (r.nextDouble() > 0.5 ? 1 : -1)) : 1;
    }

    private class ItemZReportInfo {
        String barcode;
        Double count;
        Double price;
        Integer departmentStore;

        public ItemZReportInfo(String barcode, Double count, Double price, Integer departmentStore) {
            this.barcode = barcode;
            this.count = count;
            this.price = price;
            this.departmentStore = departmentStore;
        }
    }
}