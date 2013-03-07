package tmc.integration.imp;

import org.xBaseJ.DBF;
import org.xBaseJ.fields.LogicalField;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.interop.Compare;
import platform.server.classes.DataClass;
import platform.server.classes.DoubleClass;
import platform.server.classes.LogicalClass;
import platform.server.classes.StringClass;
import platform.server.data.Field;
import platform.server.data.Modify;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Join;
import platform.server.data.query.QueryBuilder;
import platform.server.data.type.ObjectType;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import tmc.integration.scheduler.FlagSemaphoreTask;
import platform.server.session.*;
import tmc.VEDBusinessLogics;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class SinglePriceImportTask extends FlagSemaphoreTask {

    VEDBusinessLogics BL;
    String impPath;
    String impFileName;
    Integer impDocID;
    Integer impSaleActionID;
    Integer impReturnDocID;
    private ExecutionContext<ClassPropertyInterface> context;

    public SinglePriceImportTask(ExecutionContext<ClassPropertyInterface> context, String impPath, String impFileName, Integer impDocID, Integer impSaleActionID, Integer impReturnDocID) {
        this.context = context;
        this.BL = (VEDBusinessLogics) context.getBL();
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

            DataSession session = context.createSession();

            SingleKeyTableUsage<PropertyField> table = new SingleKeyTableUsage<PropertyField>(barcodeClass, SetFact.toOrderExclSet(nameField, priceField, noDiscField), Field.<PropertyField>typeGetter());

            for (int i = 0; i < recordCount; i++) {

                impFile.read();

                String barcode = new String(impFile.getField("bar").getBytes(), "Cp866");
                String name = new String(impFile.getField("name").getBytes(), "Cp866");
                Double price = Double.parseDouble(impFile.getField("cen").get());
                Boolean noDisc = (((LogicalField)impFile.getField("isdisc")).getBoolean()?null:true);

                Map<KeyField, DataObject> keys = Collections.singletonMap(barcodeField, new DataObject(barcode));

                MExclMap<PropertyField, ObjectValue> mProperties = MapFact.mExclMap(3);
                mProperties.exclAdd(nameField, new DataObject(name));
                mProperties.exclAdd(priceField, new DataObject(price));
                mProperties.exclAdd(noDiscField, noDisc == null ? NullValue.instance : new DataObject(true, LogicalClass.instance));

                table.modifyRecord(session.sql, new DataObject(barcode), mProperties.immutable(), Modify.MODIFY);
            }

            ImRevMap<PropertyInterface, KeyExpr> mapKeys = BL.VEDLM.barcodeToObject.property.getMapKeys();

            QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(mapKeys);
            query.and(table.join(mapKeys.singleValue()).getWhere());
            query.addProperty("value", BL.VEDLM.barcodeToObject.property.getExpr(mapKeys));

            ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(session);

            for (int i=0,size=result.size();i<size;i++) {

                if (result.getValue(i).singleValue() == null) { // не нашли объект

                    DataObject article = session.addObject(BL.VEDLM.article);

                    String barcode = (String)result.getKey(i).singleValue();

                    ImRevMap<PropertyInterface, KeyExpr> mapBarKeys = BL.VEDLM.barcode.property.getMapKeys();
                    DataChanges barcodeChanges = BL.VEDLM.barcode.property.getDataChanges(
                            new PropertyChange(mapBarKeys, new DataObject(barcode).getExpr(), mapBarKeys.singleValue().compare(article, Compare.EQUALS)),
                            session.getModifier());

                    session.change(barcodeChanges);
                }
            }

            ImRevMap<PropertyInterface, KeyExpr> mapNameKeys = (ImRevMap<PropertyInterface, KeyExpr>) BL.VEDLM.baseLM.name.property.getMapKeys();
            ImRevMap<PropertyInterface, KeyExpr> mapBarKeys = MapFact.singletonRev(BL.VEDLM.barcode.property.interfaces.single(),
                                                                               mapNameKeys.singleValue());
            Join<PropertyField> priceImpJoin = table.join(BL.VEDLM.barcode.property.getExpr(mapBarKeys, session.getModifier()));

            DataChanges nameChanges = BL.VEDLM.baseLM.name.property.getDataChanges(
                    new PropertyChange(mapNameKeys, priceImpJoin.getExpr(nameField), priceImpJoin.getWhere()),
                    session.getModifier());

            // импорт количества и цены
            ObjectValue docValue = session.getObjectValue(impDocID, ObjectType.instance);

            // импорт количества
            ImRevMap<PropertyInterface, KeyExpr> mapQuantityKeys = BL.VEDLM.outerCommitedQuantity.getMapKeys();
            mapBarKeys = MapFact.singletonRev(BL.VEDLM.barcode.property.interfaces.single(),
                                                  mapQuantityKeys.getValue(1));

            priceImpJoin = table.join(BL.VEDLM.barcode.property.getExpr(mapBarKeys, session.getModifier()));

            DataChanges quantityChanges = ((CalcProperty<PropertyInterface>)BL.VEDLM.outerCommitedQuantity.property).getDataChanges(
                    new PropertyChange(mapQuantityKeys, new DataObject(999999.0).getExpr(),
                            priceImpJoin.getWhere().and(mapQuantityKeys.getValue(0).compare(docValue.getExpr(), Compare.EQUALS))),
                    session.getModifier());

            // импорт цены
            ImRevMap<PropertyInterface, KeyExpr> mapPriceKeys = BL.VEDLM.shopPrice.getMapKeys();

            mapBarKeys = MapFact.singletonRev(BL.VEDLM.barcode.property.interfaces.single(),
                                                  mapPriceKeys.getValue(1));
            priceImpJoin = table.join(BL.VEDLM.barcode.property.getExpr(mapBarKeys, session.getModifier()));

            DataChanges priceChanges = ((CalcProperty)BL.VEDLM.shopPrice.property).getDataChanges(
                    new PropertyChange(mapPriceKeys, priceImpJoin.getExpr(priceField),
                            priceImpJoin.getWhere().and(mapPriceKeys.getValue(0).compare(docValue.getExpr(), Compare.EQUALS))),
                    session.getModifier());

            // импорт распродаж
            ObjectValue saleActionValue = session.getObjectValue(impSaleActionID, ObjectType.instance);

            ImRevMap<PropertyInterface, KeyExpr> mapActionKeys = BL.VEDLM.xorActionArticle.getMapKeys();
            mapBarKeys = MapFact.singletonRev(BL.VEDLM.barcode.property.interfaces.single(),
                                                  mapActionKeys.getValue(1));

            priceImpJoin = table.join(BL.VEDLM.barcode.property.getExpr(mapBarKeys, session.getModifier()));

            DataChanges actionChanges = ((CalcProperty)BL.VEDLM.xorActionArticle.property).getDataChanges(
                    new PropertyChange(mapActionKeys, priceImpJoin.getExpr(noDiscField),
                            priceImpJoin.getWhere().and(mapActionKeys.getValue(0).compare(saleActionValue.getExpr(), Compare.EQUALS))),
                    session.getModifier());

            // сначала execute'им чтобы возврату были только созданные партии
            session.change(actionChanges.add(priceChanges.add(quantityChanges.add(nameChanges))));
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
                BL.VEDLM.dateLastImportShop.change(BL.VEDLM.baseLM.currentDate.read(session), session, session.getDataObject(importStore, ObjectType.instance));
            } catch (SQLException e) {
            }

            String apply = session.applyMessage(BL);
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
