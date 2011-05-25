package tmc.integration.imp;

import org.xBaseJ.DBF;
import org.xBaseJ.fields.LogicalField;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import platform.server.classes.DataClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.LogicalClass;
import platform.server.classes.StringClass;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.scheduler.FlagSemaphoreTask;
import platform.server.session.DataSession;
import platform.server.session.MapDataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.SingleKeyTableUsage;
import tmc.VEDBusinessLogics;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SinglePriceImportTask extends FlagSemaphoreTask {

    VEDBusinessLogics BL;
    String impPath;
    String impFileName;
    Integer impDocID;
    Integer impSaleActionID;
    Integer impReturnDocID;

    public SinglePriceImportTask(VEDBusinessLogics BL, String impPath, String impFileName, Integer impDocID, Integer impSaleActionID, Integer impReturnDocID) {
        this.BL = BL;
        this.impPath = impPath;
        this.impFileName = impFileName;
        this.impDocID = impDocID;
        this.impSaleActionID = impSaleActionID;
        this.impReturnDocID = impReturnDocID;
    }

    public void run() throws Exception {

        DBF impFile = null;

        try {
            impFile = new DBF(impPath + "\\" + impFileName + ".dbf");
            int recordCount = impFile.getRecordCount();

            DataClass barcodeClass = StringClass.get(13);
            DataClass nameClass = StringClass.get(50);
            DataClass priceClass = DoubleClass.instance;

            KeyField barcodeField = new KeyField("barcode", barcodeClass);
            PropertyField nameField = new PropertyField("name", nameClass);
            PropertyField priceField = new PropertyField("price", priceClass);
            PropertyField noDiscField = new PropertyField("nodisc", LogicalClass.instance);

            DataSession session = BL.createSession();

            SingleKeyTableUsage<PropertyField> table = new SingleKeyTableUsage<PropertyField>(barcodeClass, BaseUtils.toList(nameField, priceField, noDiscField), Field.<PropertyField>typeGetter());

            for (int i = 0; i < recordCount; i++) {

                impFile.read();

                String barcode = new String(impFile.getField("bar").getBytes(), "Cp866");
                String name = new String(impFile.getField("name").getBytes(), "Cp866");
                Double price = Double.parseDouble(impFile.getField("cen").get());
                Boolean noDisc = (((LogicalField)impFile.getField("isdisc")).getBoolean()?null:true);

                Map<KeyField, DataObject> keys = Collections.singletonMap(barcodeField, new DataObject(barcode));

                Map<PropertyField, ObjectValue> properties = new HashMap<PropertyField, ObjectValue>();
                properties.put(nameField, new DataObject(name));
                properties.put(priceField, new DataObject(price));
                properties.put(noDiscField, noDisc==null ? NullValue.instance : new DataObject(true, LogicalClass.instance));

                table.insertRecord(session.sql, new DataObject(barcode), properties, true, i == (recordCount - 1));
            }

            Map<PropertyInterface, KeyExpr> mapKeys = (Map<PropertyInterface,KeyExpr>) BL.VEDLM.LM.barcodeToObject.property.getMapKeys();

            Query<PropertyInterface, Object> query = new Query<PropertyInterface, Object>(mapKeys);
            query.and(table.join(BaseUtils.singleValue(mapKeys)).getWhere());
            query.properties.put("value", BL.VEDLM.LM.barcodeToObject.property.getExpr(mapKeys));

            OrderedMap<Map<PropertyInterface, Object>, Map<Object, Object>> result = query.execute(session.sql, session.env);

            for (Map.Entry<Map<PropertyInterface, Object>, Map<Object, Object>> row : result.entrySet()) {

                if (BaseUtils.singleValue(row.getValue()) == null) { // не нашли объект

                    DataObject article = session.addObject(BL.VEDLM.article, session.modifier);

                    String barcode = (String)BaseUtils.singleValue(row.getKey());

                    Map<PropertyInterface, KeyExpr> mapBarKeys = BL.VEDLM.LM.barcode.property.getMapKeys();
                    MapDataChanges<PropertyInterface> barcodeChanges = BL.VEDLM.LM.barcode.property.getDataChanges(new PropertyChange(mapBarKeys, new DataObject(barcode).getExpr(),
                            BaseUtils.singleValue(mapBarKeys).compare(article, Compare.EQUALS)),
                            null, session.modifier);

                    session.execute(barcodeChanges, null, null);
                }
            }

            Map<PropertyInterface, KeyExpr> mapNameKeys = (Map<PropertyInterface, KeyExpr>) BL.VEDLM.LM.name.property.getMapKeys();
            Map<PropertyInterface, KeyExpr> mapBarKeys = Collections.singletonMap(BaseUtils.single(BL.VEDLM.LM.barcode.property.interfaces),
                                                                               BaseUtils.singleValue(mapNameKeys));
            Join<PropertyField> priceImpJoin = table.join(BL.VEDLM.LM.barcode.property.getExpr(mapBarKeys, session.modifier));

            MapDataChanges<PropertyInterface> nameChanges = (MapDataChanges<PropertyInterface>) BL.VEDLM.LM.name.property.getDataChanges(
                                                new PropertyChange(mapNameKeys, priceImpJoin.getExpr(nameField), priceImpJoin.getWhere()),
                                                null, session.modifier);

            // импорт количества и цены
            ObjectValue docValue = session.getObjectValue(impDocID, ObjectType.instance);

            // импорт количества
            OrderedMap<PropertyInterface, KeyExpr> mapQuantityKeys = BL.VEDLM.outerCommitedQuantity.getMapKeys();
            mapBarKeys = Collections.singletonMap(BaseUtils.single(BL.VEDLM.LM.barcode.property.interfaces),
                                                  mapQuantityKeys.getValue(1));

            priceImpJoin = table.join(BL.VEDLM.LM.barcode.property.getExpr(mapBarKeys, session.modifier));

            MapDataChanges<PropertyInterface> quantityChanges = (MapDataChanges<PropertyInterface>) BL.VEDLM.outerCommitedQuantity.property.getDataChanges(
                                                new PropertyChange(mapQuantityKeys, new DataObject(999999.0).getExpr(),
                                                priceImpJoin.getWhere().and(mapQuantityKeys.getValue(0).compare(docValue.getExpr(), Compare.EQUALS))),
                                                null, session.modifier);

            // импорт цены
            OrderedMap<PropertyInterface, KeyExpr> mapPriceKeys = BL.VEDLM.shopPrice.getMapKeys();

            mapBarKeys = Collections.singletonMap(BaseUtils.single(BL.VEDLM.LM.barcode.property.interfaces),
                                                  mapPriceKeys.getValue(1));
            priceImpJoin = table.join(BL.VEDLM.LM.barcode.property.getExpr(mapBarKeys, session.modifier));

            MapDataChanges<PropertyInterface> priceChanges = (MapDataChanges<PropertyInterface>) BL.VEDLM.shopPrice.property.getDataChanges(
                                                new PropertyChange(mapPriceKeys, priceImpJoin.getExpr(priceField),
                                                priceImpJoin.getWhere().and(mapPriceKeys.getValue(0).compare(docValue.getExpr(), Compare.EQUALS))),
                                                null, session.modifier);

            // импорт распродаж
            ObjectValue saleActionValue = session.getObjectValue(impSaleActionID, ObjectType.instance);

            OrderedMap<PropertyInterface, KeyExpr> mapActionKeys = BL.VEDLM.xorActionArticle.getMapKeys();
            mapBarKeys = Collections.singletonMap(BaseUtils.single(BL.VEDLM.LM.barcode.property.interfaces),
                                                  mapActionKeys.getValue(1));

            priceImpJoin = table.join(BL.VEDLM.LM.barcode.property.getExpr(mapBarKeys, session.modifier));

            MapDataChanges<PropertyInterface> actionChanges = (MapDataChanges<PropertyInterface>) BL.VEDLM.xorActionArticle.property.getDataChanges(
                                                new PropertyChange(mapActionKeys, priceImpJoin.getExpr(noDiscField),
                                                priceImpJoin.getWhere().and(mapActionKeys.getValue(0).compare(saleActionValue.getExpr(), Compare.EQUALS))),
                                                null, session.modifier);

            // сначала execute'им чтобы возврату были только созданные партии
            session.execute(actionChanges.add(priceChanges.add(quantityChanges.add(nameChanges))), null, null);
/*
            // импорт количества возврата
            ObjectValue returnDocValue = session.getObjectValue(impReturnDocID, ObjectType.instance);

            OrderedMap<PropertyInterface, KeyExpr> mapReturnQuantityKeys = LM.articleInnerQuantity.getMapKeys();
            mapBarKeys = Collections.singletonMap(BaseUtils.single(LM.barcode.property.interfaces),
                                                  mapReturnQuantityKeys.getValue(1));

            priceImpJoin = table.join(Collections.singletonMap(barcodeField, LM.barcode.property.getExpr(mapBarKeys, session.modifier)));

            MapDataChanges<PropertyInterface> returnChanges = (MapDataChanges<PropertyInterface>) LM.articleInnerQuantity.property.getDataChanges(
                                                new PropertyChange(mapReturnQuantityKeys, new DataObject(99999.0).getExpr(),
                                                LM.articleInnerQuantity.property.getExpr(mapReturnQuantityKeys).getWhere().not().and(
                                                priceImpJoin.getWhere().and(mapReturnQuantityKeys.getValue(0).compare(returnDocValue.getExpr(), Compare.EQUALS)))),
                                                null, session.modifier);

            // импорт цены возврата
            OrderedMap<PropertyInterface, KeyExpr> mapReturnPriceKeys = LM.orderSaleDocPrice.getMapKeys();

            mapBarKeys = Collections.singletonMap(BaseUtils.single(LM.barcode.property.interfaces),
                                                  mapReturnPriceKeys.getValue(1));
            priceImpJoin = table.join(Collections.singletonMap(barcodeField, LM.barcode.property.getExpr(mapBarKeys, session.modifier)));

            MapDataChanges<PropertyInterface> returnPriceChanges = (MapDataChanges<PropertyInterface>) LM.orderSaleDocPrice.property.getDataChanges(
                                                new PropertyChange(mapReturnPriceKeys, priceImpJoin.getExpr(priceField),
                                                LM.orderSaleDocPrice.property.getExpr(mapReturnPriceKeys).getWhere().not().and(
                                                priceImpJoin.getWhere().and(mapReturnPriceKeys.getValue(0).compare(returnDocValue.getExpr(), Compare.EQUALS)))),
                                                null, session.modifier);

            session.execute(returnPriceChanges.add(returnChanges), null, null);*/

            try {
                Object importStore = BL.VEDLM.subjectIncOrder.read(session, (DataObject) docValue);
                BL.VEDLM.dateLastImportShop.execute(BL.VEDLM.LM.currentDate.read(session), session, session.getDataObject(importStore, ObjectType.instance));
            } catch (SQLException e) {
            }

            String apply = session.apply(BL);
            if(apply==null) {
                System.out.println("OK");
                try {
                    String copyDir = impPath + "\\" + "log";
                    new File(copyDir).mkdirs();
                    impFile.copyTo(copyDir + "\\" + impFileName + new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss").format(new Date()) + ".dbf");
                } catch (IOException e) {
                    System.out.println("Logging : " + e);
                }
            } else
                System.out.println(apply);


        } finally {
            if (impFile != null)
                impFile.close();
        }
    }
}
