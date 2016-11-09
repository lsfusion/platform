package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.Settings;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.CompiledQuery;
import lsfusion.server.data.query.InnerExprFollows;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.RemapJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public class RecursiveJoin extends QueryJoin<KeyExpr, RecursiveJoin.Query, RecursiveJoin, RecursiveJoin.QueryOuterContext> {

    public static class Query extends QueryJoin.Query<KeyExpr, Query> {
        private final Where initialWhere;
        private final Where stepWhere;
        private final boolean cyclePossible;
        private final boolean isLogical;
        private final ImRevMap<KeyExpr, KeyExpr> mapIterate;

        public Query(InnerExprFollows<KeyExpr> follows, Where initialWhere, Where stepWhere, boolean cyclePossible, boolean isLogical, ImRevMap<KeyExpr, KeyExpr> mapIterate) {
            super(follows);
            this.initialWhere = initialWhere;
            this.stepWhere = stepWhere;
            this.cyclePossible = cyclePossible;
            this.isLogical = isLogical;
            this.mapIterate = mapIterate;
        }

        public Query(Query query, Where initialWhere) {
            super(query.follows);
            this.initialWhere = initialWhere;
            stepWhere = query.stepWhere;
            mapIterate = query.mapIterate;
            cyclePossible = query.cyclePossible;
            isLogical = query.isLogical;
        }

        public Query(Query query, MapTranslate translator) {
            super(query, translator);
            this.initialWhere = query.initialWhere.translateOuter(translator);
            this.stepWhere = query.stepWhere.translateOuter(translator);
            this.cyclePossible = query.cyclePossible;
            this.isLogical = query.isLogical;
            this.mapIterate = translator.translateRevMap(query.mapIterate);
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && initialWhere.equals(((Query) o).initialWhere) && stepWhere.equals(((Query) o).stepWhere) && mapIterate.equals(((Query) o).mapIterate) && cyclePossible==((Query)o).cyclePossible && isLogical==((Query)o).isLogical;
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hash) {
            return 31 * (31 * (31 * (31 * (31 * super.hash(hash) + hashMapOuter(mapIterate, hash)) + initialWhere.hashOuter(hash)) + stepWhere.hashOuter(hash)) + (isLogical ? 1 : 0)) + (cyclePossible ? 1 : 0);
        }
        protected Query translate(MapTranslate translator) {
            return new Query(this, translator);
        }
        public ImSet<OuterContext> calculateOuterDepends() {
            return super.calculateOuterDepends().merge(SetFact.<OuterContext>toSet(initialWhere, stepWhere));
        }
    }

    public RecursiveJoin(RecursiveJoin join, MapTranslate translator) {
        super(join, translator);
    }

    // для проталкивания
    public RecursiveJoin(RecursiveJoin join, Where pushedInitialWhere) {
        super(join, new Query(join.query, pushedInitialWhere));
    }

    public RecursiveJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Where initialWhere, Where stepWhere, ImRevMap<KeyExpr, KeyExpr> mapIterate, boolean cyclePossible, boolean isLogical, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, new Query(InnerExprFollows.<KeyExpr>EMPTYEXPR(), initialWhere, stepWhere, cyclePossible, isLogical, mapIterate), group);
    }

    public RecursiveJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Query inner, ImMap<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }
    protected RecursiveJoin createThis(ImSet<KeyExpr> keys, ImSet<Value> values, Query query, ImMap<KeyExpr, BaseExpr> group) {
        return new RecursiveJoin(keys, values, query, group);
    }

    public static class QueryOuterContext extends QueryJoin.QueryOuterContext<KeyExpr, Query, RecursiveJoin, QueryOuterContext> {
        public QueryOuterContext(RecursiveJoin thisObj) {
            super(thisObj);
        }

        public RecursiveJoin translateThis(MapTranslate translator) {
            return new RecursiveJoin(thisObj, translator);
        }
    }
    protected QueryOuterContext createOuterContext() {
        return new QueryOuterContext(this);
    }

/*    public static ClassExprWhere getClassWhere(Where initialWhere, Where stepWhere, Map<KeyExpr, KeyExpr> iterate) { // новые на старые
        ClassExprWhere initialClassWhere = initialWhere.getClassWhere().remove(iterate.values());
        return initialClassWhere.or(initialClassWhere.remove(iterate.keySet()).and(stepWhere.getClassWhere().remove(iterate.values())));
    }*/

    private ClassExprWhere getClassWhere(Where where) {
        return where.getClassWhere().filterInclKeys(group.keys());
    }

    public ClassExprWhere getClassWhere() {
        return getRecClassesStats().first.first;
    }

    @IdentityLazy
    public InnerExprFollows<KeyExpr> getInnerFollows() {
        if(Settings.get().isDisableInnerFollows())
            return InnerExprFollows.EMPTYEXPR();

        ImSet<KeyExpr> groupKeys = group.keys();
        return new InnerExprFollows<>(getClassWhere().get(groupKeys.toRevMap()), groupKeys);
    }

    public StatKeys<KeyExpr> getStatKeys() {
        return getRecClassesStats().first.second;
    }

    public boolean isOnlyInitial() {
        return getRecClassesStats().second;
    }

    private StatKeys<KeyExpr> getStatKeys(Where where) {
        return where.getStatKeys(group.keys());
    }

    // теоретически можно было бы разными прогонами, но тогда функциональщиной пришлось бы заниматься, плюс непонятно как подставлять друг другу статистику / классы
    @IdentityLazy
    private Pair<Pair<ClassExprWhere, StatKeys<KeyExpr>>, Boolean> getRecClassesStats() {
        ClassExprWhere recClasses = getClassWhere(getInitialWhere());
        StatKeys<KeyExpr> recStats = getStatKeys(getInitialWhere());

        ClassExprWhere resultClasses = recClasses;
        StatKeys<KeyExpr> resultStats = recStats;

        Where stepWhere = getStepWhere();
        boolean onlyInitial = true;

        MAddSet<ClassExprWhere> mCheckedClasses = SetFact.mAddSet();
        MAddSet<StatKeys<KeyExpr>> mCheckedStats = SetFact.mAddSet();

        int iterations = 0; int maxStatsIterations = Settings.get().getMaxRecursionStatsIterations();
        while(!recClasses.isFalse() && !(mCheckedClasses.add(recClasses) && (iterations >= maxStatsIterations || mCheckedStats.add(recStats)))) {
            Where recWhere = stepWhere.and(getRecJoin(MapFact.<String, Type>EMPTY(), "recursivetable", genKeyNames(),
                    recClasses, recStats).getWhere());
            if(!recWhere.isFalse()) // значит будет еще итерация
                onlyInitial = false;
            recClasses = getClassWhere(recWhere);
            recStats = getStatKeys(recWhere);

            resultClasses = recClasses.or(resultClasses);
            resultStats = recStats.or(resultStats);
            iterations++;
        }
        return new Pair<>(new Pair<>(resultClasses, resultStats), onlyInitial);
    }

    // для recursive join статистика рекурсивной таблицы делается заведомо маленькой, так как после первых операций, как правило рекурсия начинает сходится и количество записей начинает резко падать
    public Join<String> getRecJoin(ImMap<String, Type> props, String name, ImRevMap<String, KeyExpr> keyNames, Stat adjustStat) {
        StatKeys<KeyExpr> statKeys = getStatKeys();
        if(adjustStat != null)
            statKeys = statKeys.decrease(adjustStat);
        return getRecJoin(props, name, keyNames, getClassWhere(), statKeys);
    }

    public Join<String> getRecJoin(ImMap<String, Type> props, String name, ImRevMap<String, KeyExpr> keyNames, final ClassExprWhere classWhere, StatKeys<KeyExpr> statKeys) {

        // генерируем поля таблицы
        ImRevMap<KeyField, KeyExpr> recKeys = keyNames.mapRevKeys(new GetKeyValue<KeyField, KeyExpr, String>() {
            public KeyField getMapValue(KeyExpr keyExpr, String name) {
                return new KeyField(name, classWhere.getKeyType(keyExpr));
            }});
        ImRevMap<String, PropertyField> recProps = props.mapRevValues(new GetKeyValue<PropertyField, String, Type>() { // assert что пустое если logical рекурсия
            public PropertyField getMapValue(String key, Type value) {
                return new PropertyField(key, value);
            }});

        RecursiveTable recTable = new RecursiveTable(name, recKeys.keys(), recProps.valuesSet(),
                classWhere.mapClasses(recKeys), statKeys.mapBack(recKeys));

        return new RemapJoin<>(recTable.join(recKeys.join(getFullMapIterate())), recProps); // mapp'им на предыдушие ключи
    }

    public ImRevMap<String, KeyExpr> genKeyNames() {
        return group.keys().mapRevKeys(new CompiledQuery.GenNameIndex("rk", ""));
    }

    private ImRevMap<KeyExpr, KeyExpr> getFullMapIterate() {
        final ImRevMap<KeyExpr, KeyExpr> mapIterate = getMapIterate();
        return group.keys().mapRevValues(new GetValue<KeyExpr, KeyExpr>() {
            public KeyExpr getMapValue(KeyExpr recKey) {
                return BaseUtils.nvl(mapIterate.get(recKey), recKey);
            }
        });
    }
    public Where getIsClassWhere() {
        return getClassWhere().mapClasses(group.keys().toRevMap()).getWhere(getFullMapIterate());
    }

    public StatKeys<KeyExpr> getStatKeys(KeyStat keyStat) {
        return getStatKeys();
    }

    public Where getInitialWhere() {
        return query.initialWhere;
    }
    public Where getStepWhere() {
        return query.stepWhere;
    }
    public boolean isLogical() {
        return query.isLogical;
    }
    public ImRevMap<KeyExpr, KeyExpr> getMapIterate() {
        return query.mapIterate;
    }
    public boolean isCyclePossible() {
        return query.cyclePossible;
    }
}
