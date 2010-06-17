package tmc.integration;

import platform.server.logics.scheduler.SchedulerTask;
import platform.server.logics.scheduler.FlagSemaphoreTask;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;
import platform.server.classes.DataClass;
import platform.server.classes.StringClass;
import platform.server.classes.DoubleClass;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Field;
import platform.server.data.CustomSessionTable;
import platform.server.data.type.ObjectType;
import platform.server.data.query.Query;
import platform.server.data.query.Join;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.DataSession;
import platform.server.session.MapDataChanges;
import platform.server.session.PropertyChange;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.interop.Compare;
import org.xBaseJ.DBF;
import org.xBaseJ.xBaseJException;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.io.IOException;
import java.sql.SQLException;

import tmc.VEDBusinessLogics;

public class SinglePriceImportTask extends FlagSemaphoreTask {

    VEDBusinessLogics BL;
    String impDbf;
    Integer impDocID;

    public SinglePriceImportTask(VEDBusinessLogics BL, String impDbf, Integer impDocID) {
        this.BL = BL;
        this.impDbf = impDbf;
        this.impDocID = impDocID;
    }

    public void run() throws Exception {

        DBF impFile = new DBF(impDbf);
        int recordCount = impFile.getRecordCount();

        DataClass barcodeClass = StringClass.get(13);
        DataClass nameClass = StringClass.get(50);
        DataClass priceClass = DoubleClass.instance;

        KeyField barcodeField = new KeyField("barcode", barcodeClass);
        PropertyField nameField = new PropertyField("name", nameClass);
        PropertyField priceField = new PropertyField("price", priceClass);

        Map<PropertyField, ClassWhere<Field>> classProperties = new HashMap<PropertyField, ClassWhere<Field>>();
        ClassWhere<Field> nameClassWhere = new ClassWhere<Field>(barcodeField, barcodeClass).and(new ClassWhere<Field>(nameField, nameClass));

        classProperties.put(nameField, nameClassWhere);
        classProperties.put(priceField, new ClassWhere<Field>(barcodeField, barcodeClass).and(new ClassWhere<Field>(priceField, priceClass)));

        CustomSessionTable table = new CustomSessionTable("priceimp",
                new ClassWhere(barcodeField, barcodeClass), classProperties,
                Collections.singleton(barcodeField), BaseUtils.toSetElements(nameField, priceField));

        DataSession session = BL.createSession();
        session.createTemporaryTable(table);

        for (int i = 0; i < recordCount; i++) {

            impFile.read();

            String barcode = new String(impFile.getField("bar").getBytes(), "Cp866");
            String name = new String(impFile.getField("name").getBytes(), "Cp866");
            Double price = Double.parseDouble(impFile.getField("cen").get());

            Map<KeyField, DataObject> keys = Collections.singletonMap(barcodeField, new DataObject(barcode));

            Map<PropertyField, ObjectValue> properties = new HashMap<PropertyField, ObjectValue>();
            properties.put(nameField, new DataObject(name));
            properties.put(priceField, new DataObject(price));

            session.insertRecord(table, keys, properties);
        }

        impFile.close();

        Map<PropertyInterface, KeyExpr> mapKeys = (Map<PropertyInterface,KeyExpr>) BL.barcodeToObject.property.getMapKeys();
        Map<KeyField, KeyExpr> mapFields = Collections.singletonMap(barcodeField, BaseUtils.singleValue(mapKeys));

        Query<KeyField, Object> query = new Query<KeyField, Object>(mapFields);
        query.and(table.joinAnd(mapFields).getWhere());

        query.properties.put("value", BL.barcodeToObject.property.getExpr(mapKeys));

        OrderedMap<Map<KeyField, Object>, Map<Object, Object>> result = query.execute(session);

        for (Map.Entry<Map<KeyField, Object>, Map<Object, Object>> row : result.entrySet()) {

            if (BaseUtils.singleValue(row.getValue()) == null) { // не нашли объект

                DataObject article = session.addObject(BL.article, session.modifier);

                String barcode = (String)row.getKey().get(barcodeField);

                Map<PropertyInterface, KeyExpr> mapBarKeys = BL.barcode.property.getMapKeys();
                MapDataChanges<PropertyInterface> barcodeChanges = BL.barcode.property.getDataChanges(new PropertyChange(mapBarKeys, new DataObject(barcode).getExpr(),
                        BaseUtils.singleValue(mapBarKeys).compare(article, Compare.EQUALS)),
                        null, session.modifier);

                session.execute(barcodeChanges, null, null);
            }
        }

        Map<PropertyInterface, KeyExpr> mapNameKeys = (Map<PropertyInterface, KeyExpr>) BL.name.property.getMapKeys();
        Map<PropertyInterface, KeyExpr> mapBarKeys = Collections.singletonMap(BaseUtils.single(BL.barcode.property.interfaces),
                                                                           BaseUtils.singleValue(mapNameKeys));
        Join<PropertyField> priceImpJoin = table.join(Collections.singletonMap(barcodeField, BL.barcode.property.getExpr(mapBarKeys, session.modifier, null)));

        MapDataChanges<PropertyInterface> nameChanges = (MapDataChanges<PropertyInterface>) BL.name.property.getDataChanges(
                                            new PropertyChange(mapNameKeys, priceImpJoin.getExpr(nameField), priceImpJoin.getWhere()),
                                            null, session.modifier);

        ObjectValue docValue = session.getObjectValue(impDocID, ObjectType.instance);

        OrderedMap<PropertyInterface, KeyExpr> mapQuantityKeys = BL.outerCommitedQuantity.getMapKeys();
        mapBarKeys = Collections.singletonMap(BaseUtils.single(BL.barcode.property.interfaces),
                                              mapQuantityKeys.getValue(1));

        priceImpJoin = table.join(Collections.singletonMap(barcodeField, BL.barcode.property.getExpr(mapBarKeys, session.modifier, null)));

        MapDataChanges<PropertyInterface> quantityChanges = (MapDataChanges<PropertyInterface>) BL.outerCommitedQuantity.property.getDataChanges(
                                            new PropertyChange(mapQuantityKeys, new DataObject(999999.0).getExpr(),
                                            priceImpJoin.getWhere().and(mapQuantityKeys.getValue(0).compare(docValue.getExpr(), Compare.EQUALS))),
                                            null, session.modifier);

        OrderedMap<PropertyInterface, KeyExpr> mapPriceKeys = BL.shopPrice.getMapKeys();
        mapBarKeys = Collections.singletonMap(BaseUtils.single(BL.barcode.property.interfaces),
                                              mapPriceKeys.getValue(1));

        priceImpJoin = table.join(Collections.singletonMap(barcodeField, BL.barcode.property.getExpr(mapBarKeys, session.modifier, null)));

        MapDataChanges<PropertyInterface> priceChanges = (MapDataChanges<PropertyInterface>) BL.shopPrice.property.getDataChanges(
                                            new PropertyChange(mapPriceKeys, priceImpJoin.getExpr(priceField),
                                            priceImpJoin.getWhere().and(mapPriceKeys.getValue(0).compare(docValue.getExpr(), Compare.EQUALS))),
                                            null, session.modifier);

        session.execute(priceChanges.add(quantityChanges.add(nameChanges)), null, null);

        session.apply(BL);
    }
}
