package platform.server.data.expr.query;

import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractInnerContext;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.SourceJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.Where;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AggrExpr<K extends Expr,G extends AggrType, I extends AggrExpr.Query<G, I>, J extends QueryJoin<?, ?, ?, ?>,
        T extends AggrExpr<K, G, I, J, T, IC>, IC extends AggrExpr.QueryInnerContext<K, G, I, J, T, IC>> extends QueryExpr<K, I, J, T, IC> {

    protected AggrExpr(I query, Map<K, BaseExpr> group) {
        super(query, group);
    }

    protected AggrExpr(T queryExpr, MapTranslate translator) {
        super(queryExpr, translator);
    }

    public static abstract class Query<G extends AggrType, I extends Query<G, I>> extends AbstractOuterContext<I> {
        public final List<Expr> exprs;
        public final OrderedMap<Expr, Boolean> orders;
        public final G type;

        protected boolean isComplex() {
            return true;
        }

        protected Query(List<Expr> exprs, OrderedMap<Expr, Boolean> orders, G type) {
            this.exprs = exprs;
            this.orders = orders;
            this.type = type;
        }

        public Expr getMainExpr() {
            return exprs.get(0);
        }

        public boolean twins(TwinImmutableInterface o) {
            return exprs.equals(((Query)o).exprs) && orders.equals(((Query)o).orders) && type.equals(((Query) o).type);
        }

        protected int hash(HashContext hashContext) {
            return (AbstractOuterContext.hashOuter(exprs, hashContext) * 31 + AbstractOuterContext.hashOuter(orders, hashContext)) * 31 + type.hashCode();
        }

        protected Query(I query, MapTranslate translate) {
            this.exprs = translate.translate(query.exprs);
            this.orders = translate.translate(query.orders);
            this.type = query.type;
        }

        protected Query(I query, QueryTranslator translator) {
            this.exprs = translator.translate(query.exprs);
            this.orders = translator.translate(query.orders);
            this.type = query.type;
        }

        @IdentityLazy
        public Where getWhere() {
            return Expr.getWhere(getExprs());
        }

        public abstract Set<Expr> getExprs();

        public QuickSet<OuterContext> calculateOuterDepends() {
            return new QuickSet<OuterContext>(getExprs());
        }
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
    }

}
