package integration;

import org.xBaseJ.xBaseJException;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.*;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExportSOTActionProperty extends ScriptingActionProperty {

    public ExportSOTActionProperty(ScriptingLogicsModule LM) {
        super(LM);
    }

    @Override
    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        try {
            String path = (String) getLCP("exportSOTDirectory").read(context);
            if (path != null && !path.isEmpty()) {
                path = path.trim();

                if (getLCP("exportSOTReep").read(context) != null)
                    exportReep(context, path);

                if (getLCP("exportSOTFtxp").read(context) != null)
                    exportFtx(context, path, false);

                if (getLCP("exportSOTFtxd").read(context) != null)
                    exportFtx(context, path, true);

            }
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        } catch (xBaseJException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void exportReep(ExecutionContext context, String path) throws IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException {

        List<Reep> reepList = exportReepToList(context);

        if (reepList.size() == 0) {
            context.requestUserInteraction(new MessageClientAction("По заданным параметрам не найдено ни одной накладной", "Ошибка"));
            return;
        } else {

            if (!new File(path).exists())
                new File(path).mkdir();

            FileOutputStream stream = new FileOutputStream(path + "//REEP");
            OutputStreamWriter writer = new OutputStreamWriter(stream, "cp866");

            writer.write(new SimpleDateFormat("hh:mm a  dd-MMM-yy", Locale.ENGLISH).format(new Date(System.currentTimeMillis())));
            writer.write("\r\n\r\n");

            for (Reep reep : reepList) {
                writer.write("^REEP(" + reep.warehouseID + "," + reep.userInvoiceNumber + "," + reep.itemID + ")\r\n");
                writer.write(reep.uomID + ":" + reep.quantity + ":" + reep.price + ":" + reep.parentGroupID +
                        ":" + reep.itemGroupID + ":" + new SimpleDateFormat("ddMM").format(reep.date) + ":" + reep.itemName + "\r\n");
            }
            writer.close();
            context.requestUserInteraction(new MessageClientAction("Успешно экспортировано строк накладных: " + reepList.size(), "Экспорт завершён"));

        }
    }

    private List<Reep> exportReepToList(ExecutionContext context) throws ScriptingErrorLog.SemanticErrorException, SQLException {

        List<Reep> reepList = new ArrayList<Reep>();

        String[] userInvoiceProperties = new String[]{"Sale.supplierInvoice", "Sale.supplierStockInvoice",
                "Sale.customerInvoice", "Sale.customerStockInvoice", "Sale.numberInvoice",
                "Sale.dateInvoice"};
        LCP<?> isUserInvoice = LM.is(LM.findClassByCompoundName("Sale.UserInvoice"));
        ImRevMap<Object, KeyExpr> keys = (ImRevMap<Object, KeyExpr>) isUserInvoice.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> userInvoiceQuery = new QueryBuilder<Object, Object>(keys);
        for (String prop : userInvoiceProperties)
            userInvoiceQuery.addProperty(prop, getLCP(prop).getExpr(context.getModifier(), key));
        userInvoiceQuery.and(isUserInvoice.getExpr(key).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> userInvoiceResult = userInvoiceQuery.execute(context.getSession().sql);

        int i = 0;
        for (ImMap<Object, Object> userInvoiceValues : userInvoiceResult.valueIt()) {
            DataObject userInvoiceObject = new DataObject(userInvoiceResult.getKey(i).valueIt().iterator().next(), (ConcreteClass) LM.findClassByCompoundName("Sale.UserInvoice"));
            i++;

            Object supplierInvoice = userInvoiceValues.get("Sale.supplierInvoice");
            Object supplierStockInvoice = userInvoiceValues.get("Sale.supplierStockInvoice");
            Object customerInvoice = userInvoiceValues.get("Sale.customerInvoice");
            Object customerStockInvoice = userInvoiceValues.get("Sale.customerStockInvoice");
            String mag2CustomerStock = customerStockInvoice == null ? null : (String) LM.findLCPByCompoundName("mag2Stock").read(context, new DataObject(customerStockInvoice, (ConcreteClass) LM.findClassByCompoundName("Warehouse")));

            Object exportSOTSupplier = LM.findLCPByCompoundName("exportSOTSupplier").read(context);
            Object exportSOTSupplierStock = LM.findLCPByCompoundName("exportSOTSupplierStock").read(context);
            Object exportSOTCustomer = LM.findLCPByCompoundName("exportSOTCustomer").read(context);
            Object exportSOTCustomerStock = LM.findLCPByCompoundName("exportSOTCustomerStock").read(context);
            String numberInvoice = (String) userInvoiceValues.get("Sale.numberInvoice");
            Date dateInvoice = (Date) userInvoiceValues.get("Sale.dateInvoice");
            Date dateFrom = (Date) LM.findLCPByCompoundName("exportSOTDateFrom").read(context);
            Date dateTo = (Date) LM.findLCPByCompoundName("exportSOTDateTo").read(context);
            if ((dateFrom == null || dateFrom.getTime() <= dateInvoice.getTime()) &&
                    (dateTo == null || dateTo.getTime() >= dateInvoice.getTime()) &&
                    (exportSOTSupplier == null || exportSOTSupplier.equals(supplierInvoice)) &&
                    (exportSOTSupplierStock == null || exportSOTSupplierStock.equals(supplierStockInvoice)) &&
                    (exportSOTCustomer == null || exportSOTCustomer.equals(customerInvoice)) &&
                    (exportSOTCustomerStock == null || exportSOTCustomerStock.equals(customerStockInvoice))) {

                KeyExpr userInvoiceDetailExpr = new KeyExpr("userInvoiceDetail");
                ImRevMap<Object, KeyExpr> uidKeys = MapFact.singletonRev((Object) "userInvoiceDetail", userInvoiceDetailExpr);

                String[] userInvoiceDetailProperties = new String[]{"Sale.nameSkuInvoiceDetail", "Sale.skuInvoiceDetail",
                        "Sale.RRPPriceInvoiceDetail", "Sale.quantityInvoiceDetail", "sotSIDOverrideUserInvoiceDetail"};
                QueryBuilder<Object, Object> uidQuery = new QueryBuilder<Object, Object>(uidKeys);
                for (String uidProperty : userInvoiceDetailProperties) {
                    uidQuery.addProperty(uidProperty, getLCP(uidProperty).getExpr(context.getModifier(), userInvoiceDetailExpr));
                }
                uidQuery.and(getLCP("Sale.userInvoiceUserInvoiceDetail").getExpr(context.getModifier(), uidQuery.getMapExprs().get("userInvoiceDetail")).compare(userInvoiceObject.getExpr(), Compare.EQUALS));

                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> uidResult = uidQuery.execute(context.getSession().sql);

                for (ImMap<Object, Object> uidValues : uidResult.valueIt()) {
                    Object sku = uidValues.get("Sale.skuInvoiceDetail");
                    if (sku != null) {
                        DataObject itemObject = new DataObject(sku, (ConcreteClass) LM.findClassByCompoundName("Item"));

                        Object uomObject = LM.findLCPByCompoundName("sotUOMItem").read(context, itemObject);
                        String uomID = uomObject == null ? null : (String) LM.findLCPByCompoundName("idUOM").read(context, new DataObject(uomObject, (ConcreteClass) LM.findClassByCompoundName("SotUOM")));
                        Object itemGroupValue = LM.findLCPByCompoundName("itemGroupItem").read(context, itemObject);
                        DataObject itemGroupObject = itemGroupValue == null ? null : new DataObject(itemGroupValue, (ConcreteClass) LM.findClassByCompoundName("ItemGroup"));
                        String itemGroupID = itemGroupObject == null ? null : (String) LM.findLCPByCompoundName("idItemGroup").read(context, new DataObject(itemGroupValue, (ConcreteClass) LM.findClassByCompoundName("ItemGroup")));
                        Object parentGroupObject = itemGroupObject == null ? null : LM.findLCPByCompoundName("parentItemGroup").read(context, itemGroupObject);
                        String parentGroupID = parentGroupObject == null ? null : (String) LM.findLCPByCompoundName("idItemGroup").read(context, new DataObject(parentGroupObject, (ConcreteClass) LM.findClassByCompoundName("ItemGroup")));

                        String nameSku = (String) uidValues.get("Sale.nameSkuInvoiceDetail");
                        Double price = (Double) uidValues.get("Sale.RRPPriceInvoiceDetail");
                        Double quantity = (Double) uidValues.get("Sale.quantityInvoiceDetail");
                        String sotSIDOverrideUserInvoiceDetail = (String) uidValues.get("sotSIDOverrideUserInvoiceDetail");
                        String userInvoiceNumber = numberInvoice == null ? "" : String.valueOf(Integer.parseInt(numberInvoice.trim()));
                        reepList.add(new Reep(mag2CustomerStock == null ? "" : mag2CustomerStock.trim(),
                                userInvoiceNumber, sotSIDOverrideUserInvoiceDetail == null ? "" : sotSIDOverrideUserInvoiceDetail.trim(),
                                nameSku == null ? "" : nameSku.trim(), uomID == null ? "" : uomID.trim().replaceFirst("UOMS", ""),
                                parentGroupID == null ? "" : parentGroupID.trim().split("IG|:")[1],
                                itemGroupID == null ? "" : itemGroupID.trim().split("IG|:")[2],
                                price == null ? "" : String.valueOf(price), quantity == null ? "" : String.valueOf(quantity),
                                dateInvoice));
                    }
                }
            }
        }
        return reepList;
    }

    private void exportFtx(ExecutionContext context, String path, Boolean isReturn) throws IOException, xBaseJException, ScriptingErrorLog.SemanticErrorException, SQLException {

        List<Ftx> ftxList = exportFtxToList(context, isReturn);
        String prefix = isReturn ? "FTXD" : "FTXP";
        if (ftxList.size() == 0) {
            context.requestUserInteraction(new MessageClientAction("По заданным параметрам не найдено ни одной накладной", "Ошибка"));
            return;
        } else {

            if (!new File(path).exists())
                new File(path).mkdir();

            FileOutputStream stream = new FileOutputStream(path + "//" + prefix);
            OutputStreamWriter writer = new OutputStreamWriter(stream, "cp866");
            String previousUNPLegalEntity1 = "";
            String previousUNPLegalEntity2 = "";
            Integer previousEntryNumber = 0;
            for (Ftx ftx : ftxList) {
                if (!previousUNPLegalEntity1.equals(ftx.unpLegalEntity1)) {
                    writer.write(String.format("^" + prefix + "\r\n%s\r\n", ftx.unpLegalEntity1));
                    previousUNPLegalEntity1 = ftx.unpLegalEntity1 == null ? "" : ftx.unpLegalEntity1;
                }

                if (!previousUNPLegalEntity2.equals(ftx.unpLegalEntity2)) {
                    writer.write(String.format("^" + prefix + "(%d,412,%s,%s)\r\n%s:%s:%s:%s\r\n",
                            ftx.monthNumber, ftx.storeID, ftx.numberDocument,
                            new SimpleDateFormat("ddMM").format(ftx.dateDocument), ftx.sumDocument.intValue(), ftx.sumWareDocument.intValue(), ftx.unpLegalEntity2));
                    previousUNPLegalEntity2 = ftx.unpLegalEntity2 == null ? "" : ftx.unpLegalEntity2;
                }

                if (!previousEntryNumber.equals(ftx.entryNumber)) {
                    writer.write(String.format("^" + prefix + "(%d,412,%s,%s,%s)\r\n%s:%s:%s:%s:%s\r\n",
                            ftx.monthNumber, ftx.storeID, ftx.numberDocument, ftx.entryNumber,
                            ftx.operationCode, ftx.correspondingAccountCode, ftx.analyticalAccountCode, ftx.sumEntry.intValue(), ftx.sumWareEntry.intValue()));
                    previousEntryNumber = ftx.entryNumber == null ? 0 : ftx.entryNumber;
                }

                if (ftx.financialSum != null)
                    writer.write(String.format("^" + prefix + "(%d,412,%s,%s,%s,%s)\r\n183:%s:601:%s:%s\r\n",
                            ftx.monthNumber, ftx.storeID, ftx.numberDocument, ftx.entryNumber, ftx.mainEntryAccountingInformationNumber,
                            ftx.debitAnalytics, ftx.creditAnalytics, ftx.financialSum.intValue()));
            }
            writer.close();
            context.requestUserInteraction(new MessageClientAction("Успешно экспортировано проводок: " + ftxList.size(), "Экспорт завершён"));

        }
    }

    private List<Ftx> exportFtxToList(ExecutionContext context, Boolean isReturn) throws ScriptingErrorLog.SemanticErrorException, SQLException {

        List<Ftx> ftxList = new ArrayList<Ftx>();

        String namespace = isReturn ? "SaleReturn." : "Sale.";
        String[] saleInvoiceProperties = new String[]{namespace + "supplierInvoice", namespace + "customerInvoice",
                namespace + "dateUserInvoice", "numberObject", namespace + "customerStockInvoice"};
        LCP<?> isSaleInvoice = LM.is(LM.findClassByCompoundName(namespace + "Invoice"));
        ImRevMap<Object, KeyExpr> keys = (ImRevMap<Object, KeyExpr>) isSaleInvoice.getMapKeys();
        KeyExpr key = keys.singleValue();
        QueryBuilder<Object, Object> saleInvoiceQuery = new QueryBuilder<Object, Object>(keys);
        for (String prop : saleInvoiceProperties)
            saleInvoiceQuery.addProperty(prop, getLCP(prop).getExpr(context.getModifier(), key));
        saleInvoiceQuery.and(isSaleInvoice.getExpr(key).getWhere());

        ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> saleInvoiceResult = saleInvoiceQuery.execute(context.getSession().sql);

        int i = 0;
        for (ImMap<Object, Object> saleInvoiceValues : saleInvoiceResult.valueIt()) {
            DataObject userInvoiceObject = new DataObject(saleInvoiceResult.getKey(i).valueIt().iterator().next(), (ConcreteClass) LM.findClassByCompoundName("Sale.UserInvoice"));
            i++;
            Object operationObject = getLCP(namespace + "operationUserInvoice").read(context, userInvoiceObject);
            String codeOperation = operationObject == null ? "" : (String) getLCP("codeOperation").read(context, new DataObject(operationObject, (ConcreteClass) LM.findClassByCompoundName("Sale.Operation")));
            codeOperation = codeOperation==null ? "" : codeOperation.trim();
            DataObject supplierInvoiceObject = new DataObject(saleInvoiceValues.get(namespace + "supplierInvoice"), (ConcreteClass) LM.findClassByCompoundName("LegalEntity"));
            String unpLegalEntity1 = (String) getLCP("UNPLegalEntity").read(context, supplierInvoiceObject);
            Object customerStockInvoice = saleInvoiceValues.get(namespace + "customerStockInvoice");
            DataObject customerInvoiceObject = new DataObject(saleInvoiceValues.get(namespace + "customerInvoice"), (ConcreteClass) LM.findClassByCompoundName("LegalEntity"));
            String unpLegalEntity2 = (String) getLCP("UNPLegalEntity").read(context, customerInvoiceObject);
            String mag2Stock = customerStockInvoice == null ? null : (String) LM.findLCPByCompoundName("mag2Stock").read(context, new DataObject(customerStockInvoice, (ConcreteClass) LM.findClassByCompoundName("Warehouse")));
            mag2Stock = mag2Stock == null ? "" : mag2Stock.trim();

            Date dateInvoice = (Date) saleInvoiceValues.get(namespace + "dateUserInvoice");
            Date dateFrom = (Date) LM.findLCPByCompoundName(isReturn ? "exportSOTFtxdDateFrom" : "exportSOTFtxpDateFrom").read(context);
            Date dateTo = (Date) LM.findLCPByCompoundName(isReturn ? "exportSOTFtxdDateTo": "exportSOTFtxpDateTo").read(context);

            if ((dateFrom == null || dateFrom.getTime() <= dateInvoice.getTime()) &&
                    (dateTo == null || dateTo.getTime() >= dateInvoice.getTime())) {

                Integer monthNumber = dateInvoice.getMonth() + 1;
                String numberDocument = (String) saleInvoiceValues.get("numberObject");

                KeyExpr userInvoiceDetailExpr = new KeyExpr("userInvoiceDetail");
                ImRevMap<Object, KeyExpr> uidKeys = MapFact.singletonRev((Object) "userInvoiceDetail", userInvoiceDetailExpr);

                String[] userInvoiceDetailProperties = new String[]{namespace + "valueVATUserInvoiceDetail", namespace + "sumUserInvoiceDetail",
                        "purchaseRetailVATSumUserInvoiceDetail", namespace + "VATSumInvoiceDetail", "purchaseRetailMarkupSumUserInvoiceDetail"};
                QueryBuilder<Object, Object> uidQuery = new QueryBuilder<Object, Object>(uidKeys);
                for (String uidProperty : userInvoiceDetailProperties) {
                    uidQuery.addProperty(uidProperty, getLCP(uidProperty).getExpr(context.getModifier(), userInvoiceDetailExpr));
                }
                uidQuery.and(getLCP(namespace + "userInvoiceUserInvoiceDetail").getExpr(context.getModifier(), uidQuery.getMapExprs().get("userInvoiceDetail")).compare(userInvoiceObject.getExpr(), Compare.EQUALS));
                ImOrderMap<ImMap<Object, Object>, ImMap<Object, Object>> uidResult = uidQuery.execute(context.getSession().sql);

                Double sum = 0.0;
                Double vatSum = 0.0;
                Double markupSum = 0.0;
                Map<Double, Double> vatMap = new HashMap<Double, Double>();
                for (ImMap<Object, Object> uidValues : uidResult.valueIt()) {
                    Double vat = (Double) uidValues.get("purchaseRetailVATSumUserInvoiceDetail");
                    Double valueVAT = (Double) uidValues.get(namespace + "valueVATUserInvoiceDetail");
                    Double currentSum = vatMap.containsKey(valueVAT) ? vatMap.get(valueVAT) : 0;
                    if (vat != null) {
                        vatMap.put(valueVAT, currentSum + vat);
                        vatSum += vat;
                    }
                    sum += (Double) uidValues.get(namespace + "sumUserInvoiceDetail");
                    markupSum += (Double) uidValues.get("purchaseRetailMarkupSumUserInvoiceDetail");
                }

                unpLegalEntity1 = unpLegalEntity1 == null ? "" : unpLegalEntity1.trim();
                unpLegalEntity2 = unpLegalEntity2 == null ? "" : unpLegalEntity2.trim();
                numberDocument = numberDocument == null ? "" : numberDocument.trim();

                Double sumDocument = sum + markupSum + vatSum;

                int j = 1;
                for (Map.Entry<Double, Double> entry : vatMap.entrySet()) {
                    String vatCode = entry.getKey().equals(20.0) ? "55" : entry.getKey().equals(10.0) ? "11" : "00";
                    ftxList.add(new Ftx(unpLegalEntity1, unpLegalEntity2, monthNumber, mag2Stock, numberDocument,
                            dateInvoice, sumDocument, 0.0, 1, codeOperation, "601", "412", sum, 0.0, j, vatCode, "290387275", entry.getValue()));
                    j++;
                }

                ftxList.add(new Ftx(unpLegalEntity1, unpLegalEntity2, monthNumber, mag2Stock, numberDocument,
                        dateInvoice, sumDocument, 0.0, 2, codeOperation, "428", "412", vatSum, 0.0, null, null, null, null));

                ftxList.add(new Ftx(unpLegalEntity1, unpLegalEntity2, monthNumber, mag2Stock, numberDocument,
                        dateInvoice, sumDocument, 0.0, 3, codeOperation, "423", "412", markupSum, 0.0, null, null, null, null));

            }
        }
        return ftxList;
    }
}