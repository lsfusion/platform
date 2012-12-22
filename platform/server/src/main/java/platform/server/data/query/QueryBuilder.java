package platform.server.data.query;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.*;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.server.classes.BaseClass;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.instance.FormInstance;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;
import platform.server.session.ExecutionEnvironment;
import platform.server.session.PropertyChange;

import java.sql.SQLException;

// mutable
public class QueryBuilder<K, V> {

    private final ImRevMap<K,KeyExpr> mapKeys;
    private final MExclMap<V, Expr> mProperties = MapFact.mExclMap();
    private Where where = Where.TRUE;

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
        this(mapKeys, Where.TRUE);
    }

    public QueryBuilder(ImSet<K> mapInterface) {
        this(mapInterface, MapFact.<K, DataObject>EMPTY());
    }

    public QueryBuilder(ImSet<K> keys, ImMap<K, DataObject> mapValues) {
        this(KeyExpr.getMapKeys(keys), Where.TRUE, mapValues);
    }

    public QueryBuilder(ImRevMap<K, KeyExpr> keys, ImMap<K, DataObject> mapValues) {
        this(keys, Where.TRUE, mapValues);
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
            mapExprs = (ImMap<K, Expr>) PropertyChange.simplifyExprs(mapKeys, where); //MapFact.override(mapKeys, DataObject.getMapExprs(mapValues));
        return mapExprs;
    }

    public Query<K, V> getQuery() {
        return new Query<K, V>(mapKeys, mProperties.immutable(), where);
    }

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session) throws SQLException {
        return getQuery().execute(session);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, ImOrderMap<V, Boolean> orders) throws SQLException {
        return getQuery().execute(session, orders);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session) throws SQLException {
        return getQuery().execute(session);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionContext context) throws SQLException {
        return getQuery().execute(context);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionEnvironment env) throws SQLException {
        return getQuery().execute(env);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(FormInstance form) throws SQLException {
        return getQuery().execute(form);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, QueryEnvironment env) throws SQLException {
        return getQuery().execute(session, env);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session, ImOrderMap<V, Boolean> orders, int selectTop) throws SQLException {
        return getQuery().execute(session, orders, selectTop);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(FormInstance form, ImOrderMap<V, Boolean> orders, int selectTop) throws SQLException {
        return getQuery().execute(form, orders, selectTop);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, ImOrderMap<V, Boolean> orders, int selectTop, QueryEnvironment env) throws SQLException {
        return getQuery().execute(session, orders, selectTop, env);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, BaseClass baseClass) throws SQLException {
        return getQuery().executeClasses(session, baseClass);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(DataSession session) throws SQLException {
        return getQuery().executeClasses(session);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass) throws SQLException {
        return getQuery().executeClasses(session, env, baseClass);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, QueryEnvironment env, BaseClass baseClass, ImOrderMap<? extends Expr, Boolean> orders) throws SQLException {
        return getQuery().executeClasses(session, env, baseClass, orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionContext context) throws SQLException {
        return getQuery().executeClasses(context);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionContext context, ImOrderMap<? extends V, Boolean> orders) throws SQLException {
        return getQuery().executeClasses(context, orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env) throws SQLException {
        return getQuery().executeClasses(env);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ExecutionEnvironment env, ImOrderMap<? extends V, Boolean> orders) throws SQLException {
        return getQuery().executeClasses(env, orders);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(ImOrderMap<? extends Expr, Boolean> orders, ExecutionEnvironment env) throws SQLException {
        return getQuery().executeClasses(orders, env);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(FormInstance formInstance, BaseClass baseClass) throws SQLException {
        return getQuery().executeClasses(formInstance, baseClass);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, ImOrderMap<? extends V, Boolean> orders, int selectTop, BaseClass baseClass, QueryEnvironment env) throws SQLException {
        return getQuery().executeClasses(session, orders, selectTop, baseClass, env);
    }
}
