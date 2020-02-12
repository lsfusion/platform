package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.query.QueryJoin;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.where.Where;

public abstract class AggrExpr<K extends Expr,G extends AggrType, I extends AggrExpr.Query<G, I>, J extends QueryJoin<?, ?, ?, ?>,
        T extends AggrExpr<K, G, I, J, T, IC>, IC extends AggrExpr.QueryInnerContext<K, G, I, J, T, IC>> extends QueryExpr<K, I, J, T, IC> {

    protected AggrExpr(I query, ImMap<K, BaseExpr> group) {
        super(query, group);
    }

    protected AggrExpr(T queryExpr, MapTranslate translator) {
        super(queryExpr, translator);
    }

    public static abstract class Query<G extends AggrType, I extends Query<G, I>> extends QueryExpr.Query<I> {
        public final ImList<Expr> exprs;
        public final ImOrderMap<Expr, Boolean> orders;
        public final boolean ordersNotNull;
        public final G type;        

        protected boolean isComplex() {
            return true;
        }

        protected Query(ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, boolean ordersNotNull, G type, boolean noInnerFollows) {
            super(noInnerFollows);
            this.exprs = exprs;
            this.orders = orders;
            this.ordersNotNull = ordersNotNull;
            this.type = type;
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && exprs.equals(((Query)o).exprs) && orders.equals(((Query)o).orders)  && ordersNotNull == ((Query)o).ordersNotNull && type.equals(((Query) o).type);
        }

        public int hash(HashContext hashContext) {
            return 31 * ((31 * (AbstractOuterContext.hashOuter(exprs, hashContext) * 31 + AbstractOuterContext.hashOuter(orders, hashContext)) * 31 + type.hashCode()) + (ordersNotNull ? 1 : 0)) + super.hash(hashContext);
        }

        protected Query(I query, MapTranslate translate) {
            super(query, translate);
            this.exprs = translate.translate(query.exprs);
            this.orders = translate.translate(query.orders);
            this.ordersNotNull = query.ordersNotNull;
            this.type = query.type;
        }

        protected Query(I query, ExprTranslator translator) {
            super(query, translator);
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

        public boolean isSelectNotInWhere() {
            return type.isSelectNotInWhere();
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
            return Where.TRUE();
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

        protected boolean isSelectNotInFullWhere() {
            return thisObj.query.isSelectNotInWhere();
        }

        protected boolean isSelect() {
            return thisObj.query.type.isSelect();
        }
    }

}
