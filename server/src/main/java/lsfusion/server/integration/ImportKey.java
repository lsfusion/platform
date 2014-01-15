package lsfusion.server.integration;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.Message;
import lsfusion.server.ThisMessage;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcPropertyImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.*;

import java.sql.SQLException;

/**
 * User: DAle
 * Date: 24.12.10
 * Time: 16:33
 */

public class ImportKey<P extends PropertyInterface> implements ImportKeyInterface, ImportDeleteInterface {
    CustomClass keyClass;
    final CalcPropertyImplement<P, ImportFieldInterface> implement;

    public ImportKey(ImportKey key) {
        this.keyClass = key.keyClass;
        this.implement = key.implement;
    }

    public ImportKey(CustomClass keyClass, CalcPropertyImplement<P, ImportFieldInterface> implement) {
        this.keyClass = keyClass;
        this.implement = implement;
    }

    public ImMap<P, ImportFieldInterface> getMapping() {
        return implement.mapping;
    }

    public CalcProperty<P> getProperty() {
        return implement.property;
    }

    public ImMap<P, DataObject> mapObjects(final ImportTable.Row row) {
        return getMapping().mapValues(new GetValue<DataObject, ImportFieldInterface>() {
            public DataObject getMapValue(ImportFieldInterface value) {
                return value.getDataObject(row);
            }
        });
    }

    public Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys, Modifier modifier) throws SQLException, SQLHandledException {
        return implement.property.getExpr(getImplementExprs(importKeys), modifier);
    }

    public Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys, ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Modifier modifier) throws SQLException, SQLHandledException {
        ImMap<P, Expr> implementExprs = getImplementExprs(importKeys);

        Expr expr = implement.property.getExpr(implementExprs, modifier);

        SinglePropertyTableUsage<P> addedKey = (SinglePropertyTableUsage<P>) addedKeys.get(this);
        if (addedKey != null) {
            Join<String> addedJoin = addedKey.join(implementExprs);
            expr = addedJoin.getExpr("value").ifElse(addedJoin.getWhere(), expr);
        }
    
        return expr;
    }

    ImMap<P, Expr> getImplementExprs(final ImMap<ImportField, ? extends Expr> importKeys) {
        return implement.mapping.mapValues(new GetValue<Expr, ImportFieldInterface>() {
            public Expr getMapValue(ImportFieldInterface value) {
                return value.getExpr(importKeys);
            }});
    }


    public String toString() {
        return keyClass.toString();
    }

    public boolean skipKey;

    // не будет виден CGProp, который тут неявно assert'ися но это и не важно
    @Message("message.synchronize.key")
    @ThisMessage
    public SinglePropertyTableUsage<P> synchronize(DataSession session, SingleKeyTableUsage<ImportField> importTable) throws SQLException, SQLHandledException {

        ImRevMap<P, KeyExpr> mapKeys = implement.property.getMapKeys();
        Where where = GroupExpr.create(getImplementExprs(importTable.join(importTable.getMapKeys()).getExprs()), Where.TRUE, mapKeys).getWhere().and( // в импортируемой таблице
                implement.property.getExpr(mapKeys, session.getModifier()).getWhere().not()); // для которых не определился объект

        return session.addObjects((ConcreteCustomClass)keyClass, new PropertySet<P>(mapKeys, where, MapFact.<Expr, Boolean>EMPTYORDER(), false));
    }

    @Override
    public Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) throws SQLException, SQLHandledException {
        ImMap<ImportField, Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();
        Expr interfaceKeyExpr = getExpr(importExprs, modifier);
        return GroupExpr.create(MapFact.singleton("key", interfaceKeyExpr),
                interfaceKeyExpr,
                GroupType.ANY,
                MapFact.singleton("key", intraKeyExpr));
    }
}
