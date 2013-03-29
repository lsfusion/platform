package integration;

import fdk.integration.*;
import org.apache.commons.lang.time.DateUtils;
import org.xBaseJ.xBaseJException;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.interop.action.MessageClientAction;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteCustomClass;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.QueryBuilder;
import platform.server.integration.*;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.scripted.ScriptingActionProperty;
import platform.server.logics.scripted.ScriptingErrorLog;
import platform.server.logics.scripted.ScriptingLogicsModule;
import platform.server.session.DataSession;

import java.io.*;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            String mag2CustomerStock = customerStockInvoice==null ? null : (String) LM.findLCPByCompoundName("mag2Stock").read(context, new DataObject(customerStockInvoice, (ConcreteClass) LM.findClassByCompoundName("warehouse")));

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
                        String uomID = uomObject == null ? null : (String) LM.findLCPByCompoundName("sidExternalizable").read(context, new DataObject(uomObject, (ConcreteClass) LM.findClassByCompoundName("SotUOM")));
                        Object itemGroupValue = LM.findLCPByCompoundName("itemGroupItem").read(context, itemObject);
                        DataObject itemGroupObject = itemGroupValue == null ? null : new DataObject(itemGroupValue, (ConcreteClass) LM.findClassByCompoundName("ItemGroup"));
                        String itemGroupID = itemGroupObject == null ? null : (String) LM.findLCPByCompoundName("sidExternalizable").read(context, new DataObject(itemGroupValue, (ConcreteClass) LM.findClassByCompoundName("ItemGroup")));
                        Object parentGroupObject = itemGroupObject == null ? null : LM.findLCPByCompoundName("parentItemGroup").read(context, itemGroupObject);
                        String parentGroupID = parentGroupObject == null ? null : (String) LM.findLCPByCompoundName("sidExternalizable").read(context, new DataObject(parentGroupObject, (ConcreteClass) LM.findClassByCompoundName("ItemGroup")));

                        String nameSku = (String) uidValues.get("Sale.nameSkuInvoiceDetail");
                        Double price = (Double) uidValues.get("Sale.RRPPriceInvoiceDetail");
                        Double quantity = (Double) uidValues.get("Sale.quantityInvoiceDetail");
                        String sotSIDOverrideUserInvoiceDetail = (String) uidValues.get("sotSIDOverrideUserInvoiceDetail");
                        String userInvoiceNumber = numberInvoice == null ? "" : String.valueOf(Integer.parseInt(numberInvoice.trim()));
                        reepList.add(new Reep(mag2CustomerStock == null ? "" : mag2CustomerStock.trim(),
                                userInvoiceNumber, sotSIDOverrideUserInvoiceDetail== null ? "": sotSIDOverrideUserInvoiceDetail.trim(),
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
}