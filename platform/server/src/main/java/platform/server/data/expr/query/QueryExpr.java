package platform.server.data.expr.query;

import platform.base.*;
import platform.server.caches.*;
import platform.server.caches.hash.HashContext;
import platform.server.classes.DataClass;
import platform.server.data.Value;
import platform.server.data.expr.*;
import platform.server.data.expr.where.pull.StatPullWheres;
import platform.server.data.query.CompileSource;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

// query именно Outer а не Inner, потому как его контекст "связан" с group, и его нельзя прозрачно подменять
public abstract class QueryExpr<K extends Expr,I extends OuterContext<I>, J extends QueryJoin<?, ?, ?, ?>,
        T extends QueryExpr<K, I, J, T, IC>, IC extends QueryExpr.QueryInnerContext<K, I, J, T, IC>> extends InnerExpr {

    public I query;
    Map<K, BaseExpr> group; // вообще гря не reverseable

    public Type getType() {
        return getInner().getType();
    }
    public Type getType(KeyType keyType) {
        return getType();
    }
    public Stat getTypeStat(KeyStat keyStat) {
        return getInner().getTypeStat();
    }
    public Stat getStatValue(platform.server.data.query.stat.KeyStat keyStat) {
        return getInner().getStatValue();
    }
    @Override
    public QuickSet<Value> getValues() {
        return super.getValues().merge(getInner().getInnerValues());
    }

    protected long calculateComplexity(boolean outer) {
        long result = super.calculateComplexity(outer);
        if(!outer)
            result += query.getComplexity(outer) + getComplexity(group.keySet(), outer);
        return result;
    }

    protected QueryExpr(I query, Map<K, BaseExpr> group) {
        this.query = query;
        this.group = group;

        assert checkExpr();
    }

    protected boolean checkExpr() {
        return true;
    }

    // трансляция
    protected QueryExpr(T queryExpr, MapTranslate translator) {
        // надо еще транслировать "внутренние" values
        MapValuesTranslate mapValues = translator.mapValues().filter(queryExpr.getInner().getInnerValues());
        MapTranslate valueTranslator = mapValues.mapKeys();
        query = queryExpr.query.translateOuter(valueTranslator);
        group = new HashMap<K, BaseExpr>();
        for(Map.Entry<K, BaseExpr> keyJoin : queryExpr.group.entrySet())
            group.put((K)keyJoin.getKey().translateOuter(valueTranslator),keyJoin.getValue().translateOuter(translator));
        assert checkExpr();
    }

    protected abstract T createThis(I query, Map<K,BaseExpr> group);

    protected abstract static class QueryInnerHashContext<K extends Expr,I extends OuterContext<I>, J extends QueryJoin<?, ?, ?, ?>,
        T extends QueryExpr<K, I, J, T, IC>, IC extends QueryExpr.QueryInnerContext<K, I, J, T, IC>> extends AbstractInnerHashContext {

        protected final T thisObj;
        protected QueryInnerHashContext(T thisObj) {
            this.thisObj = thisObj;
        }

        protected abstract int hashOuterExpr(BaseExpr outerExpr);

        public int hashInner(HashContext hashContext) {
            int hash = 0;
            for(Map.Entry<K,BaseExpr> groupExpr : thisObj.group.entrySet())
                hash += groupExpr.getKey().hashOuter(hashContext) ^ hashOuterExpr(groupExpr.getValue());
            return thisObj.query.hashOuter(hashContext) * 31 + hash;
        }

        public QuickSet<KeyExpr> getInnerKeys() {
            return thisObj.getInner().getInnerKeys();
        }
    }

    // вообще должно быть множественное наследование самого QueryExpr от InnerContext
    protected abstract static class QueryInnerContext<K extends Expr,I extends OuterContext<I>, J extends QueryJoin<?, ?, ?, ?>,
        T extends QueryExpr<K, I, J, T, IC>, IC extends QueryExpr.QueryInnerContext<K, I, J, T, IC>> extends AbstractInnerContext<IC> {

        // вообще должно быть множественное наследование от QueryInnerHashContext, правда нюанс что вместе с верхним своего же Inner класса
        private final QueryInnerHashContext<K, I, J, T, IC> inherit;
        protected final T thisObj;
        protected QueryInnerContext(T thisObj) {
            this.thisObj = thisObj;

            inherit = new QueryInnerHashContext<K, I, J, T, IC>(thisObj) {
                protected int hashOuterExpr(BaseExpr outerExpr) {
                    return outerExpr.hashCode();
                }
            };
        }

        public int hash(HashContext hashContext) {
            return inherit.hashInner(hashContext);
        }

        protected boolean isComplex() {
            return true;
        }

        public QuickSet<KeyExpr> getKeys() {
            return getOuterKeys(thisObj.group.keySet()).merge(thisObj.query.getOuterKeys());
        }

        public QuickSet<Value> getValues() {
            return getOuterValues(thisObj.group.keySet()).merge(thisObj.query.getOuterValues());
        }

        protected IC translate(MapTranslate translate) {
            return thisObj.createThis(thisObj.query.translateOuter(translate), (Map<K, BaseExpr>) translate.translateExprKeys(thisObj.group)).getInner();
        }

        private T getThis() {
            return thisObj;
        }

        public boolean equalsInner(IC object) {
            return thisObj.getClass()==object.getThis().getClass() &&  BaseUtils.hashEquals(thisObj.query,object.getThis().query) && BaseUtils.hashEquals(thisObj.group,object.getThis().group);
        }

        public abstract Type getType();
        protected Stat getTypeStat() {
            return getMainExpr().getTypeStat(getFullWhere());
        }
        @IdentityLazy
        protected Stat getStatValue() {
            if(isSelect()) { // assert что expr учавствует в where
                return new StatPullWheres().proceed(getFullWhere(), getMainExpr());
            } else
                return Stat.ALOT;
        }
        protected abstract Expr getMainExpr();
        protected abstract Where getFullWhere();
        protected abstract boolean isSelect();
        protected abstract boolean isSelectNotInWhere();
    }
    protected abstract IC createInnerContext();
    private IC inner;
    public IC getInner() {
        if(inner==null)
            inner = createInnerContext();
        return inner;
    }

    protected boolean isComplex() {
        return true;
    }
    protected int hash(final HashContext hashContext) {
        return new QueryInnerHashContext<K, I, J, T, IC>((T) this) {
            protected int hashOuterExpr(BaseExpr outerExpr) {
                return outerExpr.hashOuter(hashContext);
            }
        }.hashValues(hashContext.values);
    }

    public boolean twins(TwinImmutableInterface obj) {
        return getInner().equals(((QueryExpr)obj).getInner());
    }

    public abstract J getInnerJoin();

    public QuickSet<OuterContext> calculateOuterDepends() { // для оптимизации
        return new QuickSet<OuterContext>(group.values());
    }

    protected boolean hasDuplicateOuter() {
        return true;
    }
    public abstract class NotNull extends InnerExpr.NotNull {

        @Override
        public String getSource(CompileSource compile) {
            return compile.getNullSource(QueryExpr.this, true);
        }

        @Override
        protected String getNotSource(CompileSource compile) {
            return compile.getNullSource(QueryExpr.this, false);
        }

        public ClassExprWhere calculateClassWhere() {
            Where fullWhere = getInner().getFullWhere();
            if(fullWhere.isFalse()) return ClassExprWhere.FALSE; // нужен потому как вызывается до create

            Map<BaseExpr, K> outerInner;
/*            if(hasDuplicateOuter()) {
                ReversedMap<BaseExpr, K> reversed = new ReversedHashMap<BaseExpr, K>();
                Where equalsWhere = GroupExpr.getEqualsWhere(GroupExpr.groupMap(group, fullWhere.getExprValues(), reversed));
                fullWhere = fullWhere.and(equalsWhere);
                outerInner = reversed;
            } else
                outerInner = BaseUtils.reverse(group);*/
            outerInner = BaseUtils.reverse(group, true);

            ClassExprWhere result;
            if(getInner().isSelect()) {
                Expr mainExpr = getInner().getMainExpr();
                Map<BaseExpr, Expr> valueMap = Collections.<BaseExpr, Expr>singletonMap(QueryExpr.this, mainExpr);
                if(getInner().isSelectNotInWhere())
                    result = ClassExprWhere.mapBack(outerInner, fullWhere).and(ClassExprWhere.mapBack(valueMap, fullWhere.and(mainExpr.getWhere())));
                else
                    result = ClassExprWhere.mapBack(BaseUtils.merge(valueMap, outerInner), fullWhere);
            } else
                result = ClassExprWhere.mapBack(outerInner, fullWhere).and(new ClassExprWhere(QueryExpr.this,(DataClass) getInner().getType()));
            return result.and(getWhere(group).getClassWhere());
        }
    }
}
