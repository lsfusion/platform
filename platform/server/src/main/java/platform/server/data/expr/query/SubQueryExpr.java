package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.*;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SubQueryExpr extends QueryExpr<KeyExpr, Expr, SubQueryJoin, SubQueryExpr, SubQueryExpr.QueryInnerContext> {

    public SubQueryExpr(Expr query, Map<KeyExpr, BaseExpr> group) {
        super(query, group);
    }

    public static class QueryInnerContext extends QueryExpr.QueryInnerContext<KeyExpr, Expr, SubQueryJoin, SubQueryExpr, QueryInnerContext> {
        public QueryInnerContext(SubQueryExpr thisObj) {
            super(thisObj);
        }

        public Type getType() {
            return thisObj.query.getType(thisObj.query.getWhere());
        }

        protected Expr getMainExpr() {
            return thisObj.query;
        }

        protected Where getFullWhere() {
            return thisObj.query.getWhere();
        }

        protected boolean isSelect() {
            return true;
        }

        protected boolean isSelectNotInWhere() {
//            assert isSelect();
            return false;
        }
    }
    protected QueryInnerContext createInnerContext() {
        return new QueryInnerContext(this);
    }

    protected SubQueryExpr createThis(Expr query, Map<KeyExpr, BaseExpr> group) {
        return new SubQueryExpr(query, group);
    }

    @IdentityLazy
    public SubQueryJoin getInnerJoin() {
        return new SubQueryJoin(getInner().getInnerKeys(), getInner().getInnerValues(), query.getWhere(), group);
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

    public Expr translateQuery(QueryTranslator translator) {
        return create(query, translator.translate(group));
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    @Override
    public Expr packFollowFalse(Where falseWhere) {
        Map<KeyExpr, Expr> packedGroup = packPushFollowFalse(group, falseWhere);
        Expr packedQuery = query.pack();
        if(!(BaseUtils.hashEquals(packedQuery, query) && BaseUtils.hashEquals(packedGroup,group)))
            return create(packedQuery, packedGroup);
        else
            return this;
    }

    public static Expr create(Expr expr) {
        return create(expr, BaseUtils.<Map<KeyExpr, BaseExpr>>immutableCast(expr.getOuterKeys().toMap()), null);
    }

    public static Where create(Where where) {
        return create(ValueExpr.get(where)).getWhere();
    }

    public static Expr create(final Expr expr, Map<KeyExpr, ? extends Expr> group, PullExpr noPull) {
        Map<KeyExpr, Expr> pullGroup = new HashMap<KeyExpr, Expr>(group);
        for(KeyExpr key : getOuterKeys(expr))
            if(key instanceof PullExpr && !group.containsKey(key) && !key.equals(noPull))
                pullGroup.put(key, key);

        return create(expr, pullGroup);
    }

    public static Expr create(final Expr expr, Map<KeyExpr, ? extends Expr> group) {
        return new ExprPullWheres<KeyExpr>() {
            protected Expr proceedBase(Map<KeyExpr, BaseExpr> map) {
                return createBase(expr, map);
            }
        }.proceed(group);
    }

    public static Expr createBase(Expr expr, Map<KeyExpr, BaseExpr> group) {
        Map<KeyExpr,BaseExpr> restGroup = new HashMap<KeyExpr, BaseExpr>();
        Map<KeyExpr,BaseExpr> translate = new HashMap<KeyExpr, BaseExpr>();
        for(Map.Entry<KeyExpr,BaseExpr> groupKey : group.entrySet())
            if(groupKey.getValue().isValue())
                translate.put(groupKey.getKey(), groupKey.getValue());
            else
                restGroup.put(groupKey.getKey(), groupKey.getValue());
        if(translate.size()>0) {
            QueryTranslator translator = new PartialQueryTranslator(translate);
            expr = expr.translateQuery(translator);
        }

        return BaseExpr.create(new SubQueryExpr(expr, restGroup));
    }

    @Override
    public String toString() {
        return "SUBQUERY(" + query + "," + group + ")";
    }
}
