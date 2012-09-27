package platform.server.integration;

import platform.base.OrderedMap;
import platform.server.Message;
import platform.server.ThisMessage;
import platform.server.classes.ConcreteCustomClass;
import platform.server.data.Modify;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.*;

/**
 * User: DAle
 * Date: 24.12.10
 * Time: 16:33
 */

public class ImportKey<P extends PropertyInterface> implements ImportKeyInterface, ImportDeleteInterface {
    ConcreteCustomClass keyClass;
    final CalcPropertyImplement<P, ImportFieldInterface> implement;

    public ImportKey(ImportKey key) {
        this.keyClass = key.keyClass;
        this.implement = key.implement;
    }

    public ImportKey(ConcreteCustomClass keyClass, CalcPropertyImplement<P, ImportFieldInterface> implement) {
        this.keyClass = keyClass;
        this.implement = implement;
    }

    public ConcreteCustomClass getCustomClass() {
        return keyClass;
    }

    public Map<P, ImportFieldInterface> getMapping() {
        return implement.mapping;
    }

    public CalcProperty<P> getProperty() {
        return implement.property;
    }

    public Map<P, DataObject> mapObjects(ImportTable.Row row) {
        Map<P, DataObject> map = new HashMap<P, DataObject>();
        for (Map.Entry<P, ImportFieldInterface> entry : getMapping().entrySet()) {
            DataObject obj = entry.getValue().getDataObject(row);
            map.put(entry.getKey(), obj);
        }
        return map;
    }

    public Expr getExpr(Map<ImportField, ? extends Expr> importKeys, Modifier modifier) {
        return implement.property.getExpr(getImplementExprs(importKeys), modifier);
    }

    public Expr getExpr(Map<ImportField, ? extends Expr> importKeys, Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Modifier modifier) {
        Map<P, Expr> implementExprs = getImplementExprs(importKeys);

        Expr expr = implement.property.getExpr(implementExprs, modifier);

        SinglePropertyTableUsage<P> addedKey = (SinglePropertyTableUsage<P>) addedKeys.get(this);
        if (addedKey != null) {
            Join<String> addedJoin = addedKey.join(implementExprs);
            expr = addedJoin.getExpr("value").ifElse(addedJoin.getWhere(), expr);
        }
    
        return expr;
    }

    Map<P, Expr> getImplementExprs(Map<ImportField, ? extends Expr> importKeys) {
        Map<P, Expr> mapExprs = new HashMap<P, Expr>();
        for (Map.Entry<P, ImportFieldInterface> entry : implement.mapping.entrySet())
            mapExprs.put(entry.getKey(), entry.getValue().getExpr(importKeys));
        return mapExprs;
    }


    public String toString() {
        return keyClass.toString();
    }

    public boolean skipKey;

    // не будет виден CGProp, который тут неявно assert'ися но это и не важно
    @Message("message.synchronize.key")
    @ThisMessage
    public SinglePropertyTableUsage<P> synchronize(DataSession session, SingleKeyTableUsage<ImportField> importTable) throws SQLException {

        Map<P, KeyExpr> mapKeys = implement.property.getMapKeys();
        Where where = GroupExpr.create(getImplementExprs(importTable.join(importTable.getMapKeys()).getExprs()), Where.TRUE, mapKeys).getWhere().and( // в импортируемой таблице
                implement.property.getExpr(mapKeys, session.getModifier()).getWhere().not()); // для которых не определился объект

        return session.addObjects(keyClass, new PropertySet<P>(mapKeys, where, new OrderedMap<Expr, Boolean>(), false));
    }

    @Override
    public Expr getDeleteExpr(SessionTableUsage<String, ImportField> importTable, KeyExpr intraKeyExpr, Modifier modifier) {
        Map<ImportField, Expr> importExprs = importTable.join(importTable.getMapKeys()).getExprs();
        Expr interfaceKeyExpr = getExpr(importExprs, modifier);
        return GroupExpr.create(Collections.singletonMap("key", interfaceKeyExpr),
                interfaceKeyExpr,
                GroupType.ANY,
                Collections.singletonMap("key", intraKeyExpr));
    }
}
