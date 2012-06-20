package retail.actions;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.classes.ConcreteClass;
import platform.server.classes.StringClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;
import retail.RetailBusinessLogics;
import retail.api.remote.SalesInfo;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Time;
import java.util.*;

public class GenerateZReport extends ScriptingActionProperty {
    public GenerateZReport(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {

        DataSession session = context.getSession();
        List<SalesInfo> salesInfoList = new ArrayList<SalesInfo>();
        try {
            Random r = new Random();
            Integer zReportCount = addDeviation((Integer) getLCP("averageZReportCountGenerateZReport").read(context), 0.25, r);
            Integer billCount = (Integer) getLCP("averageBillCountGenerateZReport").read(context);
            Integer billDetailCount = (Integer) getLCP("averageBillDetailCountGenerateZReport").read(context);
            Date dateFrom = (Date) getLCP("dateFromGenerateZReport").read(context);
            dateFrom = dateFrom == null ? new Date(System.currentTimeMillis()) : dateFrom;
            Date dateTo = (Date) getLCP("dateToGenerateZReport").read(context);
            dateTo = dateTo == null ? new Date(System.currentTimeMillis()) : dateTo;

            KeyExpr departmentStoreExpr = new KeyExpr("departmentStore");
            KeyExpr itemExpr = new KeyExpr("item");
            Map<Object, KeyExpr> newKeys = new HashMap<Object, KeyExpr>();
            newKeys.put("departmentStore", departmentStoreExpr);
            newKeys.put("item", itemExpr);

            Query<Object, Object> query = new Query<Object, Object>(newKeys);
            query.properties.put("currentBalanceSkuStock", getLCP("currentBalanceSkuStock").getExpr(itemExpr, departmentStoreExpr));
            query.properties.put("currentRetailPriceLedger", getLCP("currentRetailPriceLedger").getExpr(itemExpr, departmentStoreExpr));
            query.and(getLCP("currentBalanceSkuStock").getExpr(itemExpr, departmentStoreExpr).getWhere());

            OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);

            List<ItemZReportInfo> itemZReportInfoList = new ArrayList<ItemZReportInfo>();
            List<Integer> departmentStoreList = new ArrayList<Integer>();
            for (Map.Entry<Map<Object, Object>, Map<Object, Object>> rows : result.entrySet()) {
                DataObject itemObject = new DataObject(rows.getKey().get("item"), (ConcreteClass) getClass("item"));
                Double currentBalanceSkuStock = (Double) rows.getValue().get("currentBalanceSkuStock");
                if (currentBalanceSkuStock > 0) {
                    Integer departmentStore = (Integer) rows.getKey().get("departmentStore");
                    if ((departmentStore != null) && (!departmentStoreList.contains(departmentStore)))
                        departmentStoreList.add(departmentStore);
                    Double currentRetailPriceLedger = (Double) rows.getValue().get("currentRetailPriceLedger");
                    String barcodeItem = (String) getLCP("idBarcodeSku").read(session, itemObject);
                    Boolean isWeightItem = (Boolean) getLCP("isWeightItem").read(session, itemObject);
                    itemZReportInfoList.add(new ItemZReportInfo(barcodeItem, currentBalanceSkuStock, currentRetailPriceLedger, isWeightItem != null, departmentStore));
                }
            }

            Map<Integer, Integer> groupCashRegisterDepartmentStoreMap = new HashMap<Integer, Integer>();
            for (Integer departmentStore : departmentStoreList) {

                LCP<PropertyInterface> isGroupCashRegister = LM.is(getClass("groupCashRegister"));

                Map<PropertyInterface, KeyExpr> groupCashRegisterKeys = isGroupCashRegister.getMapKeys();
                KeyExpr groupCashRegisterKey = BaseUtils.singleValue(groupCashRegisterKeys);
                Query<PropertyInterface, Object> groupCashRegisterQuery = new Query<PropertyInterface, Object>(groupCashRegisterKeys);
                groupCashRegisterQuery.and(isGroupCashRegister.property.getExpr(groupCashRegisterKeys).getWhere());
                groupCashRegisterQuery.and(getLCP("departmentStoreGroupCashRegister").getExpr(groupCashRegisterKey).compare((new DataObject(departmentStore, (ConcreteClass) getClass("departmentStore"))).getExpr(), Compare.EQUALS));

                OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> groupCashRegisterResult = groupCashRegisterQuery.execute(session.sql);

                for (Map.Entry<Map<PropertyInterface, Object>, Map<Object, Object>> rows : groupCashRegisterResult.entrySet()) {
                    Integer groupCashRegister = (Integer) rows.getKey().entrySet().iterator().next().getValue();
                    if ((groupCashRegister != null) && (!groupCashRegisterDepartmentStoreMap.containsKey(groupCashRegister)))
                        groupCashRegisterDepartmentStoreMap.put(groupCashRegister, departmentStore);
                }
            }

            Map<String, Integer> numberCashRegisterDepartmentStoreMap = new HashMap<String, Integer>();
            for (Map.Entry<Integer, Integer> groupCashRegisterDepartmentStore : groupCashRegisterDepartmentStoreMap.entrySet()) {

                LCP<PropertyInterface> isCashRegister = LM.is(getClass("cashRegister"));

                Map<PropertyInterface, KeyExpr> cashRegisterKeys = isCashRegister.getMapKeys();
                KeyExpr cashRegisterKey = BaseUtils.singleValue(cashRegisterKeys);
                Query<PropertyInterface, Object> cashRegisterQuery = new Query<PropertyInterface, Object>(cashRegisterKeys);
                cashRegisterQuery.properties.put("numberCashRegister", getLCP("numberCashRegister").getExpr(cashRegisterKey));
                cashRegisterQuery.and(isCashRegister.property.getExpr(cashRegisterKeys).getWhere());
                cashRegisterQuery.and(getLCP("groupCashRegisterCashRegister").getExpr(cashRegisterKey).compare((new DataObject(groupCashRegisterDepartmentStore.getKey(), (ConcreteClass) getClass("groupCashRegister"))).getExpr(), Compare.EQUALS));

                OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> cashRegisterResult = cashRegisterQuery.execute(session.sql);

                for (Map<Object, Object> values : cashRegisterResult.values()) {
                    String numberCashRegister = (String) values.get("numberCashRegister");
                    if ((numberCashRegister != null) && (!numberCashRegisterDepartmentStoreMap.containsKey(numberCashRegister)))
                        numberCashRegisterDepartmentStoreMap.put(numberCashRegister, groupCashRegisterDepartmentStore.getValue());
                }
            }

            if (!numberCashRegisterDepartmentStoreMap.isEmpty()) {
                Map<String, String> numberZReportCashRegisterMap = new HashMap<String, String>();

                for (int z = 1; z <= zReportCount; z++) {

                    Map.Entry<String, Integer> numberCashRegisterDepartmentStore = (Map.Entry<String, Integer>) (numberCashRegisterDepartmentStoreMap.entrySet().toArray()[r.nextInt(numberCashRegisterDepartmentStoreMap.size())/*1*/]);
                    String numberCashRegister = numberCashRegisterDepartmentStore.getKey();
                    Integer departmentStore = numberCashRegisterDepartmentStore.getValue();
                    Integer maxNumberZReport;
                    try {
                        String maxNumber = (String) getLCP("maxNumberZReport").read(session, new DataObject(getLCP("cashRegisterNumber").read(session, new DataObject(numberCashRegisterDepartmentStore.getKey(), StringClass.get(100))), (ConcreteClass) getClass("cashRegister")));
                        maxNumberZReport = maxNumber==null ? null : Integer.parseInt(maxNumber);
                    } catch (NumberFormatException e) {
                        maxNumberZReport = Math.abs(r.nextInt());
                    }
                    String numberZReport = null;
                    while (numberZReport == null || (numberZReportCashRegisterMap.containsKey(numberZReport) && numberZReportCashRegisterMap.containsValue(numberCashRegister)))
                        numberZReport = String.valueOf((maxNumberZReport == null ? 0 : maxNumberZReport) + (zReportCount < 1 ? 0 : r.nextInt(zReportCount)) + 1);
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
                            Double currentBalanceSkuStock = itemZReportInfo.count;
                            if ((currentBalanceSkuStock > 0) && (departmentStore.equals(itemZReportInfo.departmentStore))) {
                                Double quantityBillDetail;
                                if (itemZReportInfo.isWeightItem)
                                    quantityBillDetail = currentBalanceSkuStock <= 0.005 ? currentBalanceSkuStock : ((double) Math.round(r.nextDouble() * currentBalanceSkuStock / 5 * 1000) / 1000);
                                else
                                    quantityBillDetail = Math.ceil(currentBalanceSkuStock / 5) == 1 ? 1.0 : r.nextInt((int) Math.ceil(currentBalanceSkuStock / 5));
                                if ((quantityBillDetail > 0) && (currentBillDetailCount >= numberBillDetail)) {
                                    Double sumBillDetail = quantityBillDetail * (itemZReportInfo.price == null ? 0 : itemZReportInfo.price);
                                    numberBillDetail++;
                                    sumBill += sumBillDetail;
                                    Double discountSumBillDetail = r.nextDouble()>0.8 ? (sumBillDetail * r.nextInt(10) / 100) : 0;
                                    SalesInfo salesInfo = new SalesInfo(numberCashRegister, numberZReport,
                                            billNumber, date, time, 0.0, 0.0, 0.0, itemZReportInfo.barcode == null ? null : itemZReportInfo.barcode.trim(),
                                            quantityBillDetail, itemZReportInfo.price, sumBillDetail, discountSumBillDetail, null, numberBillDetail, null);
                                    billSalesInfoList.add(salesInfo);
                                    itemZReportInfo.count -= quantityBillDetail;
                                    billSalesInfoList.add(salesInfo);
                                    itemZReportInfo.count -= quantityBillDetail;
                                }
                            }
                        }
                        for (SalesInfo s : billSalesInfoList) {
                            s.sumBill = sumBill;
                            s.sumCash = sumBill;
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
        Boolean isWeightItem;
        Integer departmentStore;

        public ItemZReportInfo(String barcode, Double count, Double price, Boolean isWeightItem, Integer departmentStore) {
            this.barcode = barcode;
            this.count = count;
            this.price = price;
            this.isWeightItem = isWeightItem;
            this.departmentStore = departmentStore;
        }
    }
}