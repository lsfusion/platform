package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.IntegralClass;
import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.InnerExpr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.where.pull.ExprPullWheres;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.PartialQueryTranslator;
import lsfusion.server.data.translator.QueryTranslator;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

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

    public static class Query extends AbstractOuterContext<Query> {
        public final ImRevMap<KeyExpr, KeyExpr> mapIterate; // новые на старые
        public final Expr initial; // может содержать и старый и новый контекст
        public final Expr step; // может содержать и старый и новый контекст
        public final boolean cyclePossible;

        protected boolean isComplex() {
            return true;
        }

        protected Query(ImRevMap<KeyExpr, KeyExpr> mapIterate, Expr initial, Expr step, boolean cyclePossible) {
            this.mapIterate = mapIterate;
            this.initial = initial;
            this.step = step;
            this.cyclePossible = cyclePossible;
        }

        protected Query(Query query, MapTranslate translator) {
            this.mapIterate = translator.translateRevMap(query.mapIterate);
            this.initial = query.initial.translateOuter(translator);
            this.step = query.step.translateOuter(translator);
            this.cyclePossible = query.cyclePossible;
        }

        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }

        protected ImSet<OuterContext> calculateOuterDepends() {
            return SetFact.<OuterContext>toSet(step, initial);
        }
        
        public DataClass getType() {
            DataClass type = (DataClass) initial.getType(initial.getWhere());
            assert type instanceof LogicalClass || type instanceof IntegralClass;
            return type;
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return mapIterate.equals(((Query)o).mapIterate) && initial.equals(((Query)o).initial) && step.equals(((Query)o).step) && cyclePossible == ((Query)o).cyclePossible;
        }

        public Query calculatePack() {
            return new Query(mapIterate, initial.pack(), step.pack(), cyclePossible);
        }

        protected int hash(HashContext hash) {
            return 31 * (31 * (31 * hashMapOuter(mapIterate, hash) + initial.hashOuter(hash)) + step.hashOuter(hash)) + (cyclePossible?1:0);
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

        protected Where getFullWhere() {
            throw new RuntimeException("should not be");
        }

        protected boolean isSelect() {
            return false;
        }
        protected boolean isSelectNotInWhere() {
            throw new RuntimeException("should not be");
        }
    }
    protected QueryInnerContext createInnerContext() {
        return new QueryInnerContext(this);
    }

    public RecursiveJoin getInnerJoin() {
        return new RecursiveJoin(getInner().getQueryKeys(), getInner().getInnerValues(), query.initial.getWhere(), query.step.getWhere(), query.mapIterate, query.cyclePossible, query.getType() instanceof LogicalClass, group);
    }

    public Expr translateQuery(QueryTranslator translator) {
        return create(query.mapIterate, query.initial, query.step, query.cyclePossible, translator.translate(group));
    }

    // новый на старый
    public static Expr create(final ImRevMap<KeyExpr, KeyExpr> mapIterate, final Expr initial, final Expr step, final boolean cyclePossible, ImMap<KeyExpr, ? extends Expr> group) {
        return new ExprPullWheres<KeyExpr>() {
            protected Expr proceedBase(ImMap<KeyExpr, BaseExpr> map) {
                return createBase(mapIterate, initial, step, cyclePossible, map);
            }
        }.proceed(group);
    }

    public static Expr createBase(final ImRevMap<KeyExpr, KeyExpr> mapIterate, Expr initial, Expr step, boolean cyclePossible, ImMap<KeyExpr, BaseExpr> group) {
        Result<ImMap<KeyExpr,BaseExpr>> restGroup = new Result<ImMap<KeyExpr, BaseExpr>>();
        ImMap<KeyExpr, BaseExpr> translate = group.splitKeys(new GetKeyValue<Boolean, KeyExpr, BaseExpr>() {
            public Boolean getMapValue(KeyExpr key, BaseExpr value) {
                return value.isValue() && !mapIterate.containsKey(key);
            }
        }, restGroup);
        
        if(translate.size()>0) {
            QueryTranslator translator = new PartialQueryTranslator(translate, true);
            initial = initial.translateQuery(translator);
            step = step.translateQuery(translator);
        }

        if(initial.isNull()) // потому как иначе в getInnerJoin используется getType который assert'ит что не null
            return NULL;

        RecursiveExpr expr = new RecursiveExpr(new Query(mapIterate, initial, step, cyclePossible), restGroup.result);
        if(expr.getInnerJoin().isOnlyInitial()) // чтобы кэшировалось
            return GroupExpr.create(restGroup.result.keys().toMap(), initial, GroupType.SUM, restGroup.result);

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
            return create(packedQuery.mapIterate, packedQuery.initial, packedQuery.step, packedQuery.cyclePossible, packedGroup);
        else
            return this;
    }

    public String getSource(CompileSource compile) {
        return compile.getSource(this);
    }

    public NotNull calculateNotNullWhere() {
        return new NotNull();
    }

    @Override
    public String toString() {
        return "RECURSIVE(" + query + "," + group + ")";
    }
}
