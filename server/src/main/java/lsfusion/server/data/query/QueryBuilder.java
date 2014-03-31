package lsfusion.server.data.query;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.ExecutionEnvironment;
import lsfusion.server.session.PropertyChange;
import org.apache.xmlbeans.impl.common.XPath;

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

    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, OperationOwner owner) throws SQLException, SQLHandledException {
        return getQuery().execute(session, owner);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, OperationOwner owner, ImOrderMap<V, Boolean> orders) throws SQLException, SQLHandledException {
        return getQuery().execute(session, owner, orders);
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
        return getQuery().execute(session, orders, 0);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(ExecutionContext context, ImOrderMap<V, Boolean> orders) throws SQLException, SQLHandledException {
        return getQuery().execute(context, orders, 0);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(DataSession session, ImOrderMap<V, Boolean> orders, int selectTop) throws SQLException, SQLHandledException {
        return getQuery().execute(session, orders, selectTop);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(FormInstance form, ImOrderMap<V, Boolean> orders, int selectTop) throws SQLException, SQLHandledException {
        return getQuery().execute(form, orders, selectTop);
    }
    public ImOrderMap<ImMap<K, Object>, ImMap<V, Object>> execute(SQLSession session, ImOrderMap<V, Boolean> orders, int selectTop, QueryEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().execute(session, orders, selectTop, env);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, BaseClass baseClass, OperationOwner owner) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session, baseClass, owner);
    }
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(DataSession session) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session);
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
    public ImOrderMap<ImMap<K, DataObject>, ImMap<V, ObjectValue>> executeClasses(SQLSession session, ImOrderMap<? extends V, Boolean> orders, int selectTop, BaseClass baseClass, QueryEnvironment env) throws SQLException, SQLHandledException {
        return getQuery().executeClasses(session, orders, selectTop, baseClass, env);
    }
}
