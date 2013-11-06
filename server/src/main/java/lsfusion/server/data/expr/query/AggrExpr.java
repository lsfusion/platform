package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.where.Where;

public abstract class AggrExpr<K extends Expr,G extends AggrType, I extends AggrExpr.Query<G, I>, J extends QueryJoin<?, ?, ?, ?>,
        T extends AggrExpr<K, G, I, J, T, IC>, IC extends AggrExpr.QueryInnerContext<K, G, I, J, T, IC>> extends QueryExpr<K, I, J, T, IC> {

    protected AggrExpr(I query, ImMap<K, BaseExpr> group) {
        super(query, group);
    }

    protected AggrExpr(T queryExpr, MapTranslate translator) {
        super(queryExpr, translator);
    }

    public static abstract class Query<G extends AggrType, I extends Query<G, I>> extends AbstractOuterContext<I> {
        public final ImList<Expr> exprs;
        public final ImOrderMap<Expr, Boolean> orders;
        public final boolean ordersNotNull;
        public final G type;

        protected boolean isComplex() {
            return true;
        }

        protected Query(ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, G type) {
            this.exprs = exprs;
            this.orders = orders;
            this.ordersNotNull = ordersNotNull;
            this.type = type;
        }

        public boolean twins(TwinImmutableObject o) {
            return exprs.equals(((Query)o).exprs) && orders.equals(((Query)o).orders)  && ordersNotNull == ((Query)o).ordersNotNull && type.equals(((Query) o).type);
        }

        protected int hash(HashContext hashContext) {
            return (31 * (AbstractOuterContext.hashOuter(exprs, hashContext) * 31 + AbstractOuterContext.hashOuter(orders, hashContext)) * 31 + type.hashCode()) + (ordersNotNull ? 1 : 0);
        }

        protected Query(I query, MapTranslate translate) {
            this.exprs = translate.translate(query.exprs);
            this.orders = translate.translate(query.orders);
            this.ordersNotNull = query.ordersNotNull;
            this.type = query.type;
        }

        protected Query(I query, QueryTranslator translator) {
            this.exprs = translator.translate(query.exprs);
            this.orders = translator.translate(query.orders);
            this.ordersNotNull = query.ordersNotNull;
            this.type = query.type;
        }

        private Where where;
        @ManualLazy
        public Where getWhere() {
            if(where==null)
                where = calculateWhere();
            return where;
        }
        
        protected Where calculateWhere() { // чтобы с аспектами проще было
            return type.getWhere(exprs).and(getOrderWhere());
        }
        
        public Where getOrderWhere() { // редкое использование поэтому не кэшируем
            return AggrExpr.getOrderWhere(orders, ordersNotNull); 
        }

        public Expr getMainExpr() {
            return type.getMainExpr(exprs);
        }

        public ImSet<Expr> getExprs() { // получает все выражения
            return SetFact.add(exprs.toOrderSet().getSet(), orders.keys());
        }

        public ImSet<OuterContext> calculateOuterDepends() {
            return BaseUtils.immutableCast(getExprs());
        }
    }
    
    public static Where getOrderWhere(ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull) {
        if(ordersNotNull)
            return Expr.getWhere(orders.keys());
        else
            return Where.TRUE;
    }

    public static ImOrderMap<Expr, Boolean> fixOrders(ImOrderMap<Expr, Boolean> orders, ImRevMap<?, KeyExpr> mapKeys) {
        return orders.mergeOrder(mapKeys.valuesSet().toOrderSet().toOrderMap(false));
    }

    public abstract static class QueryInnerContext<K extends Expr,G extends AggrType, I extends AggrExpr.Query<G, I>, J extends QueryJoin<?, ?, ?, ?>,
        T extends AggrExpr<K, G, I, J, T, IC>, IC extends QueryInnerContext<K, G, I, J, T, IC>> extends QueryExpr.QueryInnerContext<K, I, J, T, IC> {

        protected QueryInnerContext(T thisObj) {
            super(thisObj);
        }

        protected Expr getMainExpr() {
            return thisObj.query.getMainExpr();
        }

        protected boolean isSelect() {
            return thisObj.query.type.isSelect();
        }

        protected boolean isSelectNotInWhere() {
            return thisObj.query.type.isSelectNotInWhere();
        }
    }

}
