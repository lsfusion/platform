package lsfusion.server.session;

import lsfusion.base.ImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ConcreteObjectClass;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.innerjoins.KeyEqual;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

import java.sql.SQLException;

public class ClassChange extends ImmutableObject {
    
    public final DataObject keyValue;
    public final ObjectValue propValue;

    public final KeyExpr key;
    public final Where where;
    public final Expr expr;

    @Override
    public String toString() {
        if(keyValue != null)
            return "KV : " + keyValue + ", PV : " + propValue;
        return "W : " + where + ", E : " + expr + ", K : " + key;
    }

    public ClassChange(DataObject keyValue, ConcreteObjectClass cls) {
        this.keyValue = keyValue;
        this.propValue = cls.getClassObject();

        this.key = null;
        this.where = null;
        this.expr = null;
    }

    public ClassChange(KeyExpr key, Where where, ConcreteObjectClass cls) {
        this(key, where, cls.getClassObject().getExpr());
    }
    
    public ClassChange(KeyExpr key, Where where, Expr expr) {
        this.key = key;
        this.where = where;
        this.expr = expr;

        this.keyValue = null;
        this.propValue = null;
    }

    public ModifyResult modifyRows(SingleKeyPropertyUsage table, SQLSession session, BaseClass baseClass, Modify type, QueryEnvironment queryEnv, OperationOwner owner, boolean updateClasses) throws SQLException, SQLHandledException {
        if(keyValue !=null)
            return table.modifyRecord(session, keyValue, propValue, type, owner);
        else
            return table.modifyRows(session, getQuery(), baseClass, type, queryEnv, updateClasses);
    }
    
    public boolean containsObject(SQLSession sql, DataObject object, BaseClass baseClass, QueryEnvironment queryEnv) throws SQLException, SQLHandledException {
        if(keyValue != null)
            return keyValue.equals(object);
        
        return Expr.readValues(sql, baseClass, MapFact.singleton("value", ValueExpr.get(where.translateQuery(
                new KeyEqual(key, object.getExpr()).getTranslator()))), queryEnv).singleValue()!=null;
    }

    @IdentityLazy
    public Query<String, String> getQuery() {
        if(keyValue != null)
            return new Query<String, String>(MapFact.singletonRev("key", new KeyExpr("key")), Where.TRUE, MapFact.singleton("key", keyValue), MapFact.singleton("value", propValue.getExpr()));
        else
            return new Query<String, String>(MapFact.singletonRev("key", key), expr, "value", where);
    }

    public Join<String> join(Expr expr) {
        return getQuery().join(MapFact.singleton("key", expr));
    }

    public boolean needMaterialize() { // из-за сложности
        if(keyValue != null)
            return false;

        return where.needMaterialize() || expr.needMaterialize();
    }
    public boolean needMaterialize(SessionTableUsage usage) { // из-за множественного использования
        return usage.used(getQuery());
    }

    public SingleKeyPropertyUsage materialize(SQLSession sql, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        SingleKeyPropertyUsage changedClasses = new SingleKeyPropertyUsage(ObjectType.instance, ObjectType.instance);
        changedClasses.writeRows(sql, getQuery(), baseClass, env, SessionTable.matLocalQuery);
        return changedClasses;
    }
    
    public boolean isEmpty() {
        return where != null && where.isFalse();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException();
    }
}
