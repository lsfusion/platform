package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.query.RecursiveJoin;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.PartialKeyExprTranslator;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;

public class RecursiveExpr extends QueryExpr<KeyExpr, RecursiveExpr.Query, RecursiveJoin, RecursiveExpr, RecursiveExpr.QueryInnerContext> {

    public RecursiveExpr(Query query, ImMap<KeyExpr, BaseExpr> group) {
        super(query, group);
    }

    protected RecursiveExpr createThis(Query query, ImMap<KeyExpr, BaseExpr> group) {
        return new RecursiveExpr(query, group);
    }

    public RecursiveExpr(RecursiveExpr queryExpr, MapTranslate translator) {
        super(queryExpr, translator);
    }

    protected InnerExpr translate(MapTranslate translator) {
        return new RecursiveExpr(this, translator);
    }

    public static class Query extends QueryExpr.Query<Query> {
        public final ImRevMap<KeyExpr, KeyExpr> mapIterate; // new to old
        public final Expr initial; // there should be old values, however they are always equal to new (in RecursiveProperty constructor there is an explicit createAnd), so later should be refactored to have only new keys (and in incrementing algorithms just use keyexpr translation, from old to new one)
        public final Expr step;
        public final boolean cyclePossible;

        protected boolean isComplex() {
            return true;
        }

        protected Query(ImRevMap<KeyExpr, KeyExpr> mapIterate, Expr initial, Expr step, boolean cyclePossible, boolean noInnerFollows) {
            super(noInnerFollows);
            this.mapIterate = mapIterate;
            this.initial = initial;
            this.step = step;
            this.cyclePossible = cyclePossible;
        }

        protected Query(Query query, MapTranslate translator) {
            super(query, translator);
            this.mapIterate = translator.translateRevMap(query.mapIterate);
            this.initial = query.initial.translateOuter(translator);
            this.step = query.step.translateOuter(translator);
            this.cyclePossible = query.cyclePossible;
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        protected ImSet<OuterContext> calculateOuterDepends() {
            return SetFact.toSet(step, initial);
        }
        
        public DataClass getType() {
            DataClass type = (DataClass) initial.getType(initial.getWhere());
            assert type instanceof LogicalClass || type instanceof IntegralClass;
            return type;
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && mapIterate.equals(((Query)o).mapIterate) && initial.equals(((Query)o).initial) && step.equals(((Query)o).step) && cyclePossible == ((Query)o).cyclePossible;
        }

        public Query calculatePack() {
            return new Query(mapIterate, initial.pack(), step.pack(), cyclePossible, noInnerFollows);
        }

        public int hash(HashContext hash) {
            return 31 * (31 * (31 * (31 * hashMapOuter(mapIterate, hash) + initial.hashOuter(hash)) + step.hashOuter(hash)) + (cyclePossible?1:0)) + (noInnerFollows ? 1 : 0);
        }

        @Override
        public String toString() {
            return "INNER(" + mapIterate + "," + initial + "," + step + "," + cyclePossible + ")";
        }
    }

    public static class QueryInnerContext extends QueryExpr.QueryInnerContext<KeyExpr, Query, RecursiveJoin, RecursiveExpr, QueryInnerContext> {
        public QueryInnerContext(RecursiveExpr thisObj) {
            super(thisObj);
        }

        public DataClass getType() {
            return thisObj.query.getType();
        }

        @Override
        protected Stat getTypeStat(boolean forJoin) {
            return getType().getTypeStat(forJoin);
        }

        protected Expr getMainExpr() {
            throw new RuntimeException("should not be");
        }

        protected boolean isSelectNotInFullWhere() {
            throw new RuntimeException("should not be");
        }

        protected Where getFullWhere() {
            throw new RuntimeException("should not be");
        }

        protected boolean isSelect() {
            return false;
        }
    }
    protected QueryInnerContext createInnerContext() {
        return new QueryInnerContext(this);
    }

    public RecursiveJoin getInnerJoin() {
        return new RecursiveJoin(getInner().getQueryKeys(), getInner().getInnerValues(), query.initial.getWhere(), query.step.getWhere(), query.mapIterate, query.cyclePossible, query.getType() instanceof LogicalClass, group, query.noInnerFollows);
    }

    public Expr translate(ExprTranslator translator) {
        return create(query.mapIterate, query.initial, query.step, query.cyclePossible, translator.translate(group), query.noInnerFollows);
    }

    public static Expr create(final ImRevMap<KeyExpr, KeyExpr> mapIterate, final Expr initial, final Expr step, ImMap<KeyExpr, ? extends Expr> group) {
        return create(mapIterate, initial.and(CompareWhere.compare(mapIterate)), step, true, group, false);
    }

    public static Expr create(final ImRevMap<KeyExpr, KeyExpr> mapIterate, final Expr initial, final Expr step, final boolean cyclePossible, ImMap<KeyExpr, ? extends Expr> group) {
        return create(mapIterate, initial, step, cyclePossible, group, false);
    }
    
    public static Expr create(final ImRevMap<KeyExpr, KeyExpr> mapIterate, final Expr initial, final Expr step, final boolean cyclePossible, ImMap<KeyExpr, ? extends Expr> group, final boolean noInnerFollows) {
        return new ExprPullWheres<KeyExpr>() {
            protected Expr proceedBase(ImMap<KeyExpr, BaseExpr> map) {
                return createBase(mapIterate, initial, step, cyclePossible, map, noInnerFollows);
            }
        }.proceed(group);
    }

    public static Expr createBase(final ImRevMap<KeyExpr, KeyExpr> mapIterate, Expr initial, Expr step, boolean cyclePossible, ImMap<KeyExpr, BaseExpr> group, boolean noInnerFollows) {
        Result<ImMap<KeyExpr,BaseExpr>> restGroup = new Result<>();
        ImMap<KeyExpr, BaseExpr> translate = group.splitKeys((key, value) -> value.isValue() && !mapIterate.containsKey(key), restGroup);
        
        if(translate.size()>0) {
            ExprTranslator translator = new PartialKeyExprTranslator(translate, true);
            initial = initial.translateExpr(translator);
            step = step.translateExpr(translator);
        }

        if(initial.isNull()) // потому как иначе в getInnerJoin используется getType который assert'ит что не null
            return Expr.NULL();

        RecursiveExpr expr = new RecursiveExpr(new Query(mapIterate, initial, step, cyclePossible, noInnerFollows), restGroup.result);
        RecursiveJoin innerJoin = expr.getInnerJoin();
        if(innerJoin.isOnlyInitial()) // чтобы кэшировалось
            return GroupExpr.create(restGroup.result.keys().toMap(), initial, innerJoin.isLogical() ? GroupType.LOGICAL() : GroupType.SUM, restGroup.result, noInnerFollows); // boolean

        return BaseExpr.create(expr);
    }

    public class NotNull extends QueryExpr.NotNull {

        @Override
        public ClassExprWhere calculateClassWhere() {
            Where initialWhere = query.initial.getWhere();
            if(initialWhere.isFalse()) return ClassExprWhere.FALSE;

//            RecursiveJoin.getClassWhere(initialWhere, query.step.getWhere(), query.mapIterate)
            // отдельно отрабатываем не рекурсивные (которые сохранятся от initial) и рекурсивные которые постепенно появляются
            return getInnerJoin().getClassWhere().mapBack(group.toRevMap().reverse()).
                    and(new ClassExprWhere(RecursiveExpr.this, getInner().getType())).and(getWhere(group).getClassWhere());
        }
    }

    // пока как и в PartitionExpr классы не пакуются, так как их predicate push down с большой вероятностью спакует, но видимо потом придется доделать
    @Override
    public Expr packFollowFalse(Where falseWhere) {
        ImMap<KeyExpr, Expr> packedGroup = packPushFollowFalse(group, falseWhere);
        Query packedQuery = query.pack();
        if(!(BaseUtils.hashEquals(packedQuery, query) && BaseUtils.hashEquals(packedGroup,group)))
            return create(packedQuery.mapIterate, packedQuery.initial, packedQuery.step, packedQuery.cyclePossible, packedGroup, packedQuery.noInnerFollows);
        else
            return this;
    }

    public String getSource(CompileSource compile, boolean needValue) {
        return compile.getSource(this, needValue);
    }

    public NotNull calculateNotNullWhere() {
        return new NotNull();
    }

    @Override
    public String toString() {
        return "RECURSIVE(" + query + "," + group + ")";
    }
}
