package lsfusion.server.data.query.build;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.query.MapKeysInterface;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.stat.LimitOffset;

import java.sql.SQLException;

// mutable
public class QueryBuilder<K, V> {

    private final ImRevMap<K,KeyExpr> mapKeys;
    private final MExclMap<V, Expr> mProperties = MapFact.mExclMap();
    private Where where = Where.TRUE();

    public void addProperty(V prop, Expr expr) {
        mProperties.exclAdd(prop, expr);
    }

    public void addProperties(ImMap<? extends V, ? extends Expr> props) {
        mProperties.exclAddAll(props);
    }

    public void and(Where where) {
        this.where = this.where.and(where);
        mapExprs = null;
    }
    
    public QueryBuilder(ImRevMap<K,KeyExpr> mapKeys) {
        this(mapKeys, Where.TRUE());
    }

    public QueryBuilder(ImSet<K> mapInterface) {
        this(mapInterface, MapFact.EMPTY());
    }

    public QueryBuilder(ImSet<K> keys, ImMap<K, DataObject> mapValues) {
        this(KeyExpr.getMapKeys(keys), Where.TRUE(), mapValues);
    }

    public QueryBuilder(ImRevMap<K, KeyExpr> keys, ImMap<K, DataObject> mapValues) {
        this(keys, Where.TRUE(), mapValues);
    }

    public QueryBuilder(ImRevMap<K,KeyExpr> mapKeys, Where where, ImMap<K, DataObject> mapValues) {
        this.mapKeys = mapKeys;
        this.where = where.and(CompareWhere.compareValues(mapKeys.filterInclRev(mapValues.keys()), mapValues));
        assert mapKeys.keys().containsAll(mapValues.keys());
    }

    public QueryBuilder(ImRevMap<K,KeyExpr> mapKeys, Where where) {
        this.mapKeys = mapKeys;
        this.where = where;
    }

    public QueryBuilder(MapKeysInterface<K> mapInterface) {
        this(mapInterface.getMapKeys());
    }

    public QueryBuilder(MapKeysInterface<K> mapInterface, ImMap<K, DataObject> mapValues) {
        this(mapInterface.getMapKeys(), mapValues);
    }

    private ImMap<K, Expr> mapExprs;
    public ImMap<K, Expr> getMapExprs() {
        if(mapExprs == null)
            mapExprs = PropertyChange.simplifyExprs(mapKeys, where); //MapFact.override(mapKeys, DataObject.getMapExprs(mapValues));
        return mapExprs;
    }

    public Query<K, V> getQuery() {
        return new Query<>(mapKeys, mProperties.immutable(), where);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, OperationOwner owner) throws SQLException, SQLHandledException {
        return getQuery().execute(session, owner);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session) throws SQLException, SQLHandledException {
        return getQuery().execute(session);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionContext context) throws SQLException, SQLHandledException {
        return getQuery().execute(context);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().execute(env);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(FormInstance form) throws SQLException, SQLHandledException {
        return getQuery().execute(form);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, QueryEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().execute(session, env);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session, ImOrderMap<V, Boolean> orders) throws SQLException, SQLHandledException {
        return getQuery().execute(session, orders, LimitOffset.NOLIMIT);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionContext context, ImOrderMap<V, Boolean> orders) throws SQLException, SQLHandledException {
        return getQuery().execute(context, orders, LimitOffset.NOLIMIT);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return getQuery().execute(session, orders, limitOffset);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(FormInstance form, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return getQuery().execute(form, orders, limitOffset);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, ImOrderMap<V, Boolean> orders, LimitOffset limitOffset, QueryEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().execute(session, orders, limitOffset, env);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session, baseClass, owner);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(DataSession session) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(DataSession session, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session, limitOffset);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session, env, baseClass);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass, ImOrderMap<? extends Expr, Boolean> orders) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session, env, baseClass, orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionContext context) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(context);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionContext context, ImOrderMap<? extends V, Boolean> orders) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(context, orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(env);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env, ImOrderMap<? extends V, Boolean> orders) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(env, orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ImOrderMap<? extends Expr, Boolean> orders, ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(orders, env);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(FormInstance formInstance, BaseClass baseClass) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(formInstance, baseClass);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, ImOrderMap<? extends V, Boolean> orders, LimitOffset limitOffset, DataSession dataSession) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session, orders, limitOffset, dataSession.baseClass, dataSession.env);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, ImOrderMap<? extends V, Boolean> orders, LimitOffset limitOffset, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session, orders, limitOffset, baseClass, env);
    }
}
