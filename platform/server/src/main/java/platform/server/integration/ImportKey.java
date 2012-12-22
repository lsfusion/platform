package platform.server.integration;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.Message;
import platform.server.ThisMessage;
import platform.server.classes.ConcreteCustomClass;
import platform.server.classes.CustomClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Join;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcPropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    public Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys, Modifier modifier) {
        return implement.property.getExpr(getImplementExprs(importKeys), modifier);
    }

    public Expr getExpr(ImMap<ImportField, ? extends Expr> importKeys, ImMap<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Modifier modifier) {
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
    public SinglePropertyTableUsage<P> synchronize(DataSession session, SingleKeyTableUsage<ImportField> importTable) throws SQLException {

        ImRevMap<P, KeyExpr> mapKeys = implement.property.getMapKeys();
        Where where = GroupExpr.create(getImplementExprs(importTable.join(importTable.getMapKeys()).getExprs()), Where.TRUE, mapKeys).getWhere().and( // в импортируемой таблице
                implement.property.getExpr(mapKeys, session.getModifier()).getWhere().not()); // для которых не определился объект

        return session.addObjects((ConcreteCustomClass)keyClass, new PropertySet<P>(mapKeys, where, MapFact.<Expr, Boolean>EMPTYORDER(), false));
    }

    @Override
    public Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) {
        ImMap<ImportField, Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();
        Expr interfaceKeyExpr = getExpr(importExprs, modifier);
        return GroupExpr.create(MapFact.singleton("key", interfaceKeyExpr),
                interfaceKeyExpr,
                GroupType.ANY,
                MapFact.singleton("key", intraKeyExpr));
    }
}
