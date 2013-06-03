package lsfusion.server.session;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.caches.IdentityInstanceLazy;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.ValueExpr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.query.AggrExpr;
import lsfusion.server.data.expr.query.PartitionExpr;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.ObjectType;
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
