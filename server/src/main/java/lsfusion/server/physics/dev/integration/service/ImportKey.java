package lsfusion.server.physics.dev.integration.service;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.base.controller.stack.StackMessage;
import lsfusion.server.base.controller.stack.ThisMessage;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.data.PropertyOrderSet;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.action.session.table.SingleKeyTableUsage;
import lsfusion.server.logics.action.session.table.SinglePropertyTableUsage;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class ImportKey<P extends PropertyInterface> implements ImportKeyInterface, ImportDeleteInterface {
    CustomClass keyClass;
    final PropertyImplement<P, ImportFieldInterface> implement;

    public ImportKey(ImportKey<P> key) {
        this.keyClass = key.keyClass;
        this.implement = key.implement;
    }

    public ImportKey(CustomClass keyClass, PropertyImplement<P, ImportFieldInterface> implement) {
        this.keyClass = keyClass;
        this.implement = implement;
    }

    public ImMap<P, ImportFieldInterface> getMapping() {
        return implement.mapping;
    }

    public Property<P> getProperty() {
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
    @StackMessage("{message.synchronize.key}")
    @ThisMessage
    public SinglePropertyTableUsage<P> synchronize(String debugInfo, DataSession session, SingleKeyTableUsage<ImportField> importTable) throws SQLException, SQLHandledException {

        ImRevMap<P, KeyExpr> mapKeys = implement.property.getMapKeys();
        Where where = GroupExpr.create(getImplementExprs(importTable.join(importTable.getMapKeys()).getExprs()), Where.TRUE, mapKeys).getWhere().and( // в импортируемой таблице
                implement.property.getExpr(mapKeys, session.getModifier()).getWhere().not()); // для которых не определился объект

        return session.addObjects(debugInfo, (ConcreteCustomClass)keyClass, new PropertyOrderSet<>(mapKeys, where, MapFact.<Expr, Boolean>EMPTYORDER(), false));
    }

    @Override
    public Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) throws SQLException, SQLHandledException {
        ImMap<ImportField, Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();
        Expr interfaceKeyExpr = getExpr(importExprs, modifier);
        return GroupExpr.create(MapFact.singleton("key", interfaceKeyExpr),
                interfaceKeyExpr,
                GroupType.CHANGE(implement.property.getType()),
                MapFact.singleton("key", intraKeyExpr));
    }
}
