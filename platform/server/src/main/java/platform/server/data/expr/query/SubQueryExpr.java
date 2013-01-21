package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.SFunctionSet;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.*;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

public class SubQueryExpr extends QueryExpr<KeyExpr, Expr, SubQueryJoin, SubQueryExpr, SubQueryExpr.QueryInnerContext> {

    public SubQueryExpr(Expr query, ImMap<KeyExpr, BaseExpr> group) {
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

    protected SubQueryExpr createThis(Expr query, ImMap<KeyExpr, BaseExpr> group) {
        return new SubQueryExpr(query, group);
    }

    @IdentityInstanceLazy
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
        ImMap<KeyExpr, Expr> packedGroup = packPushFollowFalse(group, falseWhere);
        Expr packedQuery = query.pack();
        if(!(BaseUtils.hashEquals(packedQuery, query) && BaseUtils.hashEquals(packedGroup,group)))
            return create(packedQuery, packedGroup);
        else
            return this;
    }

    public static Expr create(Expr expr) {
        return create(expr, BaseUtils.<ImMap<KeyExpr, BaseExpr>>immutableCast(expr.getOuterKeys().toMap()), null);
    }

    public static Where create(Where where) {
        return create(ValueExpr.get(where)).getWhere();
    }

    public static Expr create(final Expr expr, final ImMap<KeyExpr, ? extends Expr> group, final PullExpr noPull) {
        ImMap<KeyExpr, KeyExpr> pullKeys = getOuterKeys(expr).filterFn(new SFunctionSet<KeyExpr>() {
            public boolean contains(KeyExpr key) {
                return key instanceof PullExpr && !group.containsKey(key) && !key.equals(noPull);
            }}).toMap();
        return create(expr, MapFact.addExcl(group, pullKeys));
    }

    public static Expr create(final Expr expr, ImMap<KeyExpr, ? extends Expr> group) {
        return new ExprPullWheres<KeyExpr>() {
            protected Expr proceedBase(ImMap<KeyExpr, BaseExpr> map) {
                return createBase(expr, map);
            }
        }.proceed(group);
    }

    public static Expr createBase(Expr expr, ImMap<KeyExpr, BaseExpr> group) {
        Result<ImMap<KeyExpr, BaseExpr>> restGroup = new Result<ImMap<KeyExpr, BaseExpr>>();
        ImMap<KeyExpr, BaseExpr> translate = group.splitKeys(new GetKeyValue<Boolean, KeyExpr, BaseExpr>() {
            public Boolean getMapValue(KeyExpr key, BaseExpr value) {
                return value.isValue();
            }
        }, restGroup);

        if(translate.size()>0) {
            QueryTranslator translator = new PartialQueryTranslator(translate);
            expr = expr.translateQuery(translator);
        }

        return BaseExpr.create(new SubQueryExpr(expr, restGroup.result));
    }

    @Override
    public String toString() {
        return "SUBQUERY(" + query + "," + group + ")";
    }
}
