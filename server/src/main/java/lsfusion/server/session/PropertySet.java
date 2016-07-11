package lsfusion.server.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SessionTable;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.query.AggrExpr;
import lsfusion.server.data.expr.query.PartitionExpr;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.NullReader;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

public class PropertySet<T extends PropertyInterface> {
    private final ImRevMap<T,KeyExpr> mapKeys;
    private final Where where;
    private final ImOrderMap<Expr, Boolean> orders;
    private final boolean ordersNotNull;

    public PropertySet(ImRevMap<T, KeyExpr> mapKeys, Where where, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull) {
        this.mapKeys = mapKeys;
        this.where = where;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;
    }

    private Where getFullWhere() {
        return where.and(AggrExpr.getOrderWhere(orders, ordersNotNull));
    }

    @IdentityInstanceLazy
    private Query<T,Expr> getExecuteQuery() {
        QueryBuilder<T, Expr> query = new QueryBuilder<>(mapKeys, getFullWhere());
        query.addProperties(orders.keys().toMap());
        return query.getQuery();
    }

    public Query<T, String> getAddQuery(BaseClass baseClass) {
        Expr exprNum = PartitionExpr.create(PartitionType.SUM,
                ListFact.singleton(new ValueExpr(1, ObjectType.idClass).and(getFullWhere())),
                AggrExpr.fixOrders(orders, mapKeys), ordersNotNull, SetFact.<Expr>EMPTY(), mapKeys.valuesSet().toMap());

        QueryBuilder<T, String> query = new QueryBuilder<>(mapKeys, exprNum.getWhere());
        query.addProperty("value", FormulaExpr.createCustomFormula("prm1", baseClass.unknown, exprNum));
        return query.getQuery();
    }

    // временный пока фикс из-за ошибки postgre при Partition запросах, когда при наличии SUM ORDER BY expr начинает GroupAggregate крутить в цикле
    // materialize subqueries могут решить эту проблему
    public boolean needMaterialize() {
        if(where.needMaterialize())
            return true;

        for(Expr order : orders.keyIt())
            if(order.needMaterialize())
                return true;

        return false;
    }

    public Pair<PropertySet<T>, SessionTableUsage> materialize(DataSession session) throws SQLException, SQLHandledException {
        final Where fullWhere = getFullWhere();

        final ImRevMap<Object, Expr> objects = BaseUtils.generateObjects(orders.keys()).reverse();

        SessionTableUsage<T, Object> tableUsage = new SessionTableUsage<>(mapKeys.keys().toOrderSet(), objects.keys().toOrderSet(), new Type.Getter<T>() {
            public Type getType(T key) {
                return mapKeys.get(key).getType(fullWhere);
            }
        }, new Type.Getter<Object>() {
            public Type getType(Object key) {
                Expr expr = objects.get(key);
                Type type = expr.getType(fullWhere);
                if(type == null) {
                    assert expr.isNull();
                    return NullReader.typeInstance;
                }
                return type;
            }
        });
        tableUsage.writeRows(session.sql, new Query<>(mapKeys, objects, where), session.baseClass, session.env, SessionTable.matLocalQuery);

        final Join<Object> join = tableUsage.join(mapKeys);
        return new Pair<>(new PropertySet<>(mapKeys, join.getWhere(), orders.map(objects.reverse()).mapOrderKeys(new GetValue<Expr, Object>() {
            public Expr getMapValue(Object value) {
                return join.getExpr(value);
            }}), ordersNotNull), (SessionTableUsage)tableUsage);
    }

    public ImOrderSet<ImMap<T, DataObject>> executeClasses(ExecutionEnvironment env) throws SQLException, SQLHandledException {
        return getExecuteQuery().executeClasses(env, orders).keyOrderSet();
    }
    
    public PropertySet<T> and(Where andWhere) {
        if(andWhere.isTrue())
            return this;

        return new PropertySet<>(mapKeys, where.and(andWhere), orders, ordersNotNull);
    }

    public boolean isEmpty() {
        return where.isFalse();
    }
}
