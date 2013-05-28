package platform.server.session;

import platform.base.col.ListFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.formula.FormulaExpr;
import platform.server.data.expr.query.AggrExpr;
import platform.server.data.expr.query.PartitionExpr;
import platform.server.data.expr.query.PartitionType;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
import platform.server.data.type.ObjectType;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.PropertyInterface;

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
        QueryBuilder<T, Expr> query = new QueryBuilder<T, Expr>(mapKeys, getFullWhere());
        query.addProperties(orders.keys().toMap());
        return query.getQuery();
    }

    public Query<T, String> getAddQuery(BaseClass baseClass) {
        Expr exprNum = PartitionExpr.create(PartitionType.SUM,
                ListFact.singleton(new ValueExpr(1, ObjectType.idClass).and(getFullWhere())),
                AggrExpr.fixOrders(orders, mapKeys), ordersNotNull, SetFact.<Expr>EMPTY(), mapKeys.valuesSet().toMap());

        QueryBuilder<T, String> query = new QueryBuilder<T, String>(mapKeys, exprNum.getWhere());
        query.addProperty("value", FormulaExpr.createCustomFormula("prm1", baseClass.unknown, exprNum));
        return query.getQuery();
    }

    public ImOrderSet<ImMap<T, DataObject>> executeClasses(ExecutionEnvironment env) throws SQLException {
        return getExecuteQuery().executeClasses(env, orders).keyOrderSet();
    }
    
    public PropertySet<T> and(Where andWhere) {
        if(andWhere.isTrue())
            return this;

        return new PropertySet<T>(mapKeys, where.and(andWhere), orders, ordersNotNull);
    }

    public boolean isEmpty() {
        return where.isFalse();
    }
}
