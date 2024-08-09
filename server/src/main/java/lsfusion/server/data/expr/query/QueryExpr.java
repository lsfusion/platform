package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.caches.AbstractInnerContext;
import lsfusion.server.data.caches.AbstractInnerHashContext;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.inner.InnerExpr;
import lsfusion.server.data.expr.join.classes.InnerExprFollows;
import lsfusion.server.data.expr.join.query.QueryJoin;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.value.StaticValueExpr;
import lsfusion.server.data.expr.where.pull.StatPullWheres;
import lsfusion.server.data.stat.KeyStat;
import lsfusion.server.data.stat.PropStat;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.classes.ConcreteClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.physics.admin.Settings;

// query именно Outer а не Inner, потому как его контекст "связан" с group, и его нельзя прозрачно подменять
public abstract class QueryExpr<K extends Expr,I extends QueryExpr.Query<I>, J extends QueryJoin<?, ?, ?, ?>,
        T extends QueryExpr<K, I, J, T, IC>, IC extends QueryExpr.QueryInnerContext<K, I, J, T, IC>> extends InnerExpr {

    public I query;
    public ImMap<K, BaseExpr> group; // вообще гря не reverseable, например в Partition, Recursion

    public Type getType() {
        return getInner().getType();
    }
    public Type getType(KeyType keyType) {
        return getType();
    }
    public Stat getTypeStat(KeyStat keyStat, boolean forJoin) {
        return getInner().getTypeStat(forJoin);
    }
    public PropStat getInnerStatValue(KeyStat keyStat, StatType type) {
        return new PropStat(getInner().getStatValue(type));
    }
    @Override
    public ImSet<Value> getValues() {
        return super.getValues().merge(getInner().getInnerValues());
    }
    @Override
    public ImSet<StaticValueExpr> getOuterStaticValues() {
        return super.getOuterStaticValues().merge(getInner().getInnerStaticValues());
    }
    
    public static abstract class Query<I extends QueryExpr.Query<I>> extends AbstractOuterContext<I> {
        public final boolean noInnerFollows;

        public Query(boolean noInnerFollows) {
            this.noInnerFollows = noInnerFollows;
        }

        protected Query(I query, MapTranslate translate) {
            this.noInnerFollows = query.noInnerFollows;
        }

        protected Query(I query, ExprTranslator translator) {
            this.noInnerFollows = query.noInnerFollows;
        }        

        @Override
        public int hash(HashContext hash) {
            return noInnerFollows ? 1 : 0;
        }

        @Override
        protected boolean calcTwins(TwinImmutableObject o) {
            return noInnerFollows == ((Query)o).noInnerFollows;
        }
    }

    protected boolean isNoInnerFollows() {
        return query.noInnerFollows;
    }

    protected long calculateComplexity(boolean outer) {
        long result = super.calculateComplexity(outer);
        if(!outer)
            result += query.getComplexity(outer) + getComplexity(group.keys(), outer);
        return result;
    }

    protected QueryExpr(I query, ImMap<K, BaseExpr> group) {
        this.query = query;
        this.group = group;

        assert checkExpr();
    }

    protected boolean checkExpr() {
        return true;
    }

    // трансляция
    protected QueryExpr(T queryExpr, final MapTranslate translator) {
        // надо еще транслировать "внутренние" values
        MapValuesTranslate mapValues = translator.mapValues().filter(queryExpr.getInner().getInnerValues());
        final MapTranslate valueTranslator = mapValues.mapKeys();
        query = queryExpr.query.translateOuter(valueTranslator);
        group = valueTranslator.translateExprKeys(translator.translateDirect(queryExpr.group));
        assert checkExpr();
    }

    protected abstract T createThis(I query, ImMap<K, BaseExpr> group);

    protected abstract static class QueryInnerHashContext<K extends Expr,I extends Query<I>, J extends QueryJoin<?, ?, ?, ?>,
        T extends QueryExpr<K, I, J, T, IC>, IC extends QueryExpr.QueryInnerContext<K, I, J, T, IC>> extends AbstractInnerHashContext {

        protected final T thisObj;
        protected QueryInnerHashContext(T thisObj) {
            this.thisObj = thisObj;
        }

        protected abstract int hashOuterExpr(BaseExpr outerExpr);

        public int hashInner(HashContext hashContext) {
            int hash = 0;
            for(int i=0,size=thisObj.group.size();i<size;i++)
                hash += thisObj.group.getKey(i).hashOuter(hashContext) ^ hashOuterExpr(thisObj.group.getValue(i));
            return thisObj.query.hashOuter(hashContext) * 31 + hash;
        }

        public ImSet<ParamExpr> getInnerKeys() {
            return thisObj.getInner().getInnerKeys();
        }
        public ImSet<Value> getInnerValues() {
            return thisObj.getInner().getInnerValues();
        }

        protected boolean isComplex() {
            return thisObj.isComplex();
        }
    }

    // вообще должно быть множественное наследование самого QueryExpr от InnerContext
    // change protected to public for work with java 8
    public abstract static class QueryInnerContext<K extends Expr,I extends Query<I>, J extends QueryJoin<?, ?, ?, ?>,
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

        public ImSet<ParamExpr> getKeys() {
            return getOuterSetKeys(thisObj.group.keys()).merge(thisObj.query.getOuterKeys());
        }

        public ImSet<KeyExpr> getQueryKeys() {
            return BaseUtils.immutableCast(getInnerKeys());
        }

        public ImSet<Value> getValues() {
            return getOuterColValues(thisObj.group.keys()).merge(thisObj.query.getOuterValues());
        }

        public ImSet<StaticValueExpr> getInnerStaticValues() { // можно было бы вынести в общий интерфейс InnerContext, но нужен только для компиляции запросов
            return getOuterStaticValues(thisObj.group.keys()).merge(thisObj.query.getOuterStaticValues());
        }

        public InnerExprFollows<K> getInnerFollows() { // не делаем IdentityLazy так как только при конструировании InnerJoin используется
            if(Settings.get().isDisableInnerFollows() || thisObj.isNoInnerFollows())
                return InnerExprFollows.EMPTYEXPR();

            ImSet<K> groupKeys = thisObj.group.keys();
            return new InnerExprFollows<>(lsfusion.server.data.query.Query.getClassWhere(getFullWhere(), MapFact.EMPTY(), groupKeys.toRevMap()), groupKeys);
        }

        protected IC translate(MapTranslate translate) {
            return thisObj.createThis(thisObj.query.translateOuter(translate), translate.translateExprKeys(thisObj.group)).getInner();
        }

        protected T getThis() {
            return thisObj;
        }

        public boolean equalsInner(IC object) {
            return thisObj.getClass()==object.getThis().getClass() &&  BaseUtils.hashEquals(thisObj.query,object.getThis().query) && BaseUtils.hashEquals(thisObj.group,object.getThis().group);
        }

        public abstract Type getType();
        protected abstract Stat getTypeStat(boolean forJoin);
        @IdentityLazy
        protected Stat getStatValue(StatType type) {
            Stat result;
            if(isSelect()) {
                Where fullWhere = getFullWhere();
                Expr mainExpr = getMainExpr();
                if(isSelectNotInFullWhere()) // there is an assertion that mainExpr is in FullWhere
                    fullWhere = fullWhere.and(mainExpr.getWhere());
                result = new StatPullWheres(type).proceed(fullWhere, mainExpr); 
            } else
                result = Stat.AGGR;
            // в случае с isSelect рушится assertion на min в случае когда GROUP MIN большое число разновидностей BY маленькое число разновидностей
            return thisObj.getAdjustStatValue(type, result); // возможно избыточно но рушит assertion'ы на min
        }
        protected abstract Expr getMainExpr();
        protected abstract Where getFullWhere();
        protected abstract boolean isSelect();
        protected abstract boolean isSelectNotInFullWhere(); // optimization not to do and every time
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
    public int hash(final HashContext hashContext) {
        return new QueryInnerHashContext<K, I, J, T, IC>((T) this) {
            protected int hashOuterExpr(BaseExpr outerExpr) {
                return outerExpr.hashOuter(hashContext);
            }
        }.hashValues(hashContext.values);
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return getInner().equals(((QueryExpr)obj).getInner());
    }

    public abstract J getInnerJoin();

    public ImSet<OuterContext> calculateOuterDepends() { // для оптимизации
        return BaseUtils.immutableCast(group.values().toSet());
    }

    protected boolean hasDuplicateOuter() {
        return true;
    }
    public abstract class NotNull extends InnerExpr.NotNull {

        public ClassExprWhere calculateClassWhere() {
            Where fullWhere = getInner().getFullWhere(); // в принципе сейчас можно и без groupWhere, но пока оставим так
            if(fullWhere.isFalse()) return ClassExprWhere.FALSE; // нужен потому как вызывается до create

            ImRevMap<BaseExpr, K> outerInner = group.toRevMap().reverse();

            ConcreteClass staticClass = null;
            if(!getInner().isSelect())
                staticClass = (DataClass) getInner().getType();

            if(staticClass==null) // оптимизация в основном для классов чтобы не тянуть case'ы лишние
                staticClass = getInner().getMainExpr().getStaticClass();

            ClassExprWhere result;
            if(staticClass==null) {
                Expr mainExpr = getInner().getMainExpr();
                ImRevMap<BaseExpr, Expr> valueMap = MapFact.singletonRev(QueryExpr.this, mainExpr);
                if(getInner().isSelect()) // isSelectNotInWhere неправильно использовать, так как он коррелирует вход с выходом, а это для select агрегаций не так
                    result = ClassExprWhere.mapBack(outerInner, fullWhere).and(ClassExprWhere.mapBack(valueMap, fullWhere));
                else {
                    assert false;
                    result = ClassExprWhere.mapBack(valueMap.addExcl(outerInner), fullWhere);
                }
            } else
                result = ClassExprWhere.mapBack(outerInner, fullWhere).and(new ClassExprWhere(QueryExpr.this, staticClass));
            return result.and(BaseExpr.getNotNullClassWhere(group));
        }

        @Override
        public Where translateOuter(MapTranslate translator) {
            return super.translateOuter(translator);
        }
    }
}
