package platform.server.session;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.caches.IdentityLazy;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.FormulaExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.AggrExpr;
import platform.server.data.expr.query.PartitionExpr;
import platform.server.data.expr.query.PartitionType;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.*;

public class PropertySet<T extends PropertyInterface> {
    public final Map<T,KeyExpr> mapKeys;
    public final Where where;
    public final OrderedMap<Expr, Boolean> orders;
    public final boolean ordersNotNull;

    public PropertySet(Map<T, KeyExpr> mapKeys, Where where, OrderedMap<Expr, Boolean> orders, boolean ordersNotNull) {
        this.mapKeys = mapKeys;
        this.where = where;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;
    }

    private Where getFullWhere() {
        return where.and(AggrExpr.getOrderWhere(orders, ordersNotNull));
    }

    @IdentityLazy
    private Query<T,Expr> getExecuteQuery() {
        Query<T, Expr> query = new Query<T, Expr>(mapKeys, getFullWhere());
        query.properties.putAll(BaseUtils.toMap(orders.keySet()));
        return query;
    }

    public Query<T, String> getAddQuery(BaseClass baseClass) {
        Expr exprNum = PartitionExpr.create(PartitionType.SUM,
                Collections.singletonList(ObjectType.idClass.getStaticExpr(1).and(getFullWhere())),
                AggrExpr.fixOrders(orders, mapKeys), ordersNotNull, new HashSet<Expr>(), BaseUtils.toMap(mapKeys.values()));

        Query<T, String> query = new Query<T, String>(mapKeys, exprNum.getWhere());
        query.properties.put("value", FormulaExpr.create1("prm1", baseClass.unknown, exprNum));
        return query;
    }

    public Collection<Map<T, DataObject>> executeClasses(ExecutionEnvironment env) throws SQLException {
        return getExecuteQuery().executeClasses(env, orders).keySet();
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
