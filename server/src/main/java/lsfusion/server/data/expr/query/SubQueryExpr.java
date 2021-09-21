package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.query.SubQueryJoin;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.PartialKeyExprTranslator;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;

public class SubQueryExpr extends QueryExpr<KeyExpr, SubQueryExpr.Query, SubQueryJoin, SubQueryExpr, SubQueryExpr.QueryInnerContext> {

    public SubQueryExpr(Query query, ImMap<KeyExpr, BaseExpr> group) {
        super(query, group);
    }

    public static class QueryInnerContext extends QueryExpr.QueryInnerContext<KeyExpr, SubQueryExpr.Query, SubQueryJoin, SubQueryExpr, QueryInnerContext> {
        public QueryInnerContext(SubQueryExpr thisObj) {
            super(thisObj);
        }

        public Type getType() {
            return thisObj.query.getType();
        }

        protected Expr getMainExpr() {
            return thisObj.query.getMainExpr();
        }

        protected Where getFullWhere() {
            return thisObj.query.getFullWhere();
        }

        protected boolean isSelect() {
            return true;
        }

        protected boolean isSelectNotInFullWhere() {
            return false;
        }
    }
    protected QueryInnerContext createInnerContext() {
        return new QueryInnerContext(this);
    }

    public static class Query extends QueryExpr.Query<SubQueryExpr.Query> {
        public final Expr expr; // может содержать и старый и новый контекст
        public final int top;

        public Query(Expr expr, boolean noInnerFollows, int top) {
            super(noInnerFollows);
            this.expr = expr;
            this.top = top;
        }

        protected Query(SubQueryExpr.Query query, MapTranslate translator) {
            super(query, translator);
            this.expr = query.expr.translateOuter(translator);
            this.top = query.top;
        }

        protected SubQueryExpr.Query translate(MapTranslate translator) {
            return new SubQueryExpr.Query(this, translator);
        }

        protected Query(SubQueryExpr.Query query, ExprTranslator translator) {
            super(query, translator);
            this.expr = query.expr.translateExpr(translator);
            this.top = query.top;
        }

        protected SubQueryExpr.Query translate(ExprTranslator translator) {
            return new SubQueryExpr.Query(this, translator);
        }

        protected ImSet<OuterContext> calculateOuterDepends() {
            return SetFact.toSet(expr);
        }

        public Type getType() {
            return expr.getType(expr.getWhere());
        }

        public Where getFullWhere() {
            return expr.getWhere();
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && expr.equals(((SubQueryExpr.Query)o).expr) && top == ((Query) o).top;
        }
        
        public Expr getMainExpr() {
            return expr;
        }

        public SubQueryExpr.Query calculatePack() {
            return new SubQueryExpr.Query(expr.pack(), noInnerFollows, top);
        }

        public int hash(HashContext hash) {
            return 31 * (31 * expr.hashOuter(hash) + top) + super.hash(hash);
        }

        @Override
        public String toString() {
            return "INNER(" + expr + "," + top + ")";
        }
    }

    @Override
    protected SubQueryExpr createThis(Query query, ImMap<KeyExpr, BaseExpr> group) {
        return new SubQueryExpr(query, group);
    }

    @IdentityInstanceLazy
    public SubQueryJoin getInnerJoin() {
        return new SubQueryJoin(getInner().getQueryKeys(), getInner().getInnerValues(), getInner().getInnerFollows(), getInner().getFullWhere(), query.top, group);
    }

    public SubQueryExpr(SubQueryExpr expr, MapTranslate translator) {
        super(expr, translator);
    }

    protected InnerExpr translate(MapTranslate translator) {
        return new SubQueryExpr(this, translator);
    }

    public class NotNull extends QueryExpr.NotNull {
    }

    public NotNull calculateNotNullWhere() {
        return new NotNull();
    }

    public Expr translate(ExprTranslator translator) {
        return create(query, translator.translate(group));
    }

    public String getSource(CompileSource compile, boolean needValue) {
        return compile.getSource(this, needValue);
    }

    @Override
    public Expr packFollowFalse(Where falseWhere) {
        ImMap<KeyExpr, Expr> packedGroup = packPushFollowFalse(group, falseWhere);
        Query packedQuery = query.pack();
        if(!(BaseUtils.hashEquals(packedQuery, query) && BaseUtils.hashEquals(packedGroup,group)))
            return create(packedQuery, packedGroup);
        else
            return this;
    }

    public static Expr create(Expr expr, boolean noInnerFollows) {
        return create(expr, noInnerFollows, 0);
    }

    public static Expr create(Expr expr, boolean noInnerFollows, int top) {
        return create(expr, BaseUtils.<ImMap<KeyExpr, BaseExpr>>immutableCast(expr.getOuterKeys().toMap()), null, noInnerFollows, top);
    }

    public static Where create(Where where, boolean noInnerFollows) {
        return create(ValueExpr.get(where), noInnerFollows).getWhere();
    }

    public static Expr create(final Expr expr, final ImMap<KeyExpr, ? extends Expr> group, final PullExpr noPull, boolean noInnerFollows) {
        return create(expr, group, noPull, noInnerFollows, 0);
    }

    public static Expr create(final Expr expr, final ImMap<KeyExpr, ? extends Expr> group, final PullExpr noPull, boolean noInnerFollows, int top) {
        ImMap<KeyExpr, KeyExpr> pullKeys = BaseUtils.<ImSet<KeyExpr>>immutableCast(getOuterKeys(expr)).filterFn(key -> key instanceof PullExpr && !group.containsKey(key) && !key.equals(noPull)).toMap();
        return create(new Query(expr, noInnerFollows, top), MapFact.addExcl(group, pullKeys));
    }

    public static Expr create(final Query expr, ImMap<KeyExpr, ? extends Expr> group) {
        return new ExprPullWheres<KeyExpr>() {
            protected Expr proceedBase(ImMap<KeyExpr, BaseExpr> map) {
                return createBase(expr, map);
            }
        }.proceed(group);
    }

    public static Expr createBase(Query query, ImMap<KeyExpr, BaseExpr> group) {
        Result<ImMap<KeyExpr, BaseExpr>> restGroup = new Result<>();
        ImMap<KeyExpr, BaseExpr> translate = group.splitKeys((key, value) -> value.isValue(), restGroup);

        if(translate.size()>0) {
            ExprTranslator translator = new PartialKeyExprTranslator(translate, true);
            query = query.translate(translator);
        }

        return BaseExpr.create(new SubQueryExpr(query, restGroup.result));
    }

    @Override
    public String toString() {
        return "SUBQUERY(" + query + "," + group + ")";
    }
}
