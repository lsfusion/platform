package lsfusion.server.logics.action.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.AggrExpr;
import lsfusion.server.data.expr.query.PartitionExpr;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.build.QueryBuilder;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.reader.NullReader;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.table.SessionTableUsage;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.form.stat.LimitOffset;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

public class PropertyOrderSet<T extends PropertyInterface> {
    private final ImRevMap<T,KeyExpr> mapKeys;
    private final Where where;
    private final ImOrderMap<Expr, Boolean> orders;
    private final boolean ordersNotNull;

    public PropertyOrderSet(ImRevMap<T, KeyExpr> mapKeys, Where where, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull) {
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
        Expr exprNum = PartitionExpr.create(PartitionType.sum(),
                ListFact.singleton(new ValueExpr(1L, ObjectType.idClass).and(getFullWhere())),
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

    public Pair<PropertyOrderSet<T>, SessionTableUsage> materialize(String debugInfo, DataSession session) throws SQLException, SQLHandledException {
        final Where fullWhere = getFullWhere();

        final ImRevMap<Object, Expr> objects = BaseUtils.generateObjects(orders.keys()).reverse();

        SessionTableUsage<T, Object> tableUsage = new SessionTableUsage<>(debugInfo+"-mt", mapKeys.keys().toOrderSet(), objects.keys().toOrderSet(), key -> mapKeys.get(key).getType(fullWhere), key -> {
            Expr expr = objects.get(key);
            Type type = expr.getType(fullWhere);
            if(type == null) {
                assert expr.isNull();
                return NullReader.typeInstance;
            }
            return type;
        });
        tableUsage.writeRows(session.sql, new Query<>(mapKeys, objects, where), session.baseClass, session.env, SessionTable.matLocalQuery);

        final Join<Object> join = tableUsage.join(mapKeys);
        return new Pair<>(new PropertyOrderSet<>(mapKeys, join.getWhere(), orders.map(objects.reverse()).mapOrderKeys(join::getExpr), ordersNotNull), (SessionTableUsage)tableUsage);
    }

    public ImOrderSet<ImMap<T, DataObject>> executeClasses(ExecutionEnvironment env, LimitOffset limitOffset) throws SQLException, SQLHandledException {
        return getExecuteQuery().executeClasses(env, orders, limitOffset).keyOrderSet();
    }
    
    public PropertyOrderSet<T> and(Where andWhere) {
        if(andWhere.isTrue())
            return this;

        return new PropertyOrderSet<>(mapKeys, where.and(andWhere), orders, ordersNotNull);
    }

    public boolean isEmpty() {
        return where.isFalse();
    }
}
