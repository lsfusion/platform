package lsfusion.server.logics.action.session.classes.change;

import lsfusion.base.col.MapFact;
import lsfusion.base.mutability.ImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.where.KeyEqual;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.modify.Modify;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.change.ModifyResult;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.action.session.table.SingleKeyPropertyUsage;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.ConcreteObjectClass;

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

    public static ClassChange EMPTY = new ClassChange(new KeyExpr("no"), Where.FALSE(), Expr.NULL());    
    public static ClassChange EMPTY_DELETE = new ClassChange(new KeyExpr("no"), Where.FALSE());    
    
    public ClassChange(DataObject keyValue, ConcreteObjectClass cls) {
        this.keyValue = keyValue;
        this.propValue = cls.getClassObject();

        this.key = null;
        this.where = null;
        this.expr = null;
    }

    public ClassChange(KeyExpr key, Where where, ConcreteObjectClass cls) {
        this(key, where, cls.getClassObject().getStaticExpr());
    }
    
    // delete constructor
    public ClassChange(KeyExpr key, Where where) {
        this(key, where, Expr.NULL());
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
            return table.modifyRows(session, type == Modify.DELETE ? getDeleteQuery() : getQuery(), baseClass, type, queryEnv, updateClasses);
    }
    
    public boolean containsObject(SQLSession sql, DataObject object, BaseClass baseClass, QueryEnvironment queryEnv) throws SQLException, SQLHandledException {
        if(keyValue != null)
            return keyValue.equals(object);
        
        return !Expr.readObjectValues(sql, baseClass, MapFact.singleton("value", ValueExpr.get(where.translateExpr(
                new KeyEqual(key, object.getExpr()).getTranslator()))), queryEnv).singleValue().isNull();
    }

    @IdentityLazy
    public Query<String, String> getQuery() {
        if(keyValue != null)
            return new Query<>(MapFact.singletonRev("key", new KeyExpr("key")), Where.TRUE(), MapFact.singleton("key", keyValue), MapFact.singleton("value", propValue.getStaticExpr()));
        else
            return new Query<>(MapFact.singletonRev("key", key), expr, "value", where);
    }
    @IdentityLazy
    public Query<String, String> getDeleteQuery() {
        assert keyValue == null && expr == Expr.NULL();
        return new Query<>(MapFact.singletonRev("key", key), where);
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

    public SingleKeyPropertyUsage materialize(String debugInfo, SQLSession sql, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        SingleKeyPropertyUsage changedClasses = new SingleKeyPropertyUsage(debugInfo, ObjectType.instance, ObjectType.instance, true);
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
