package lsfusion.server.data.expr.join.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.caches.OuterContext;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.join.classes.InnerExprFollows;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.RecursiveTable;
import lsfusion.server.data.query.build.Join;
import lsfusion.server.data.query.compile.CompiledQuery;
import lsfusion.server.data.query.translate.RemapJoin;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.stat.TableStatKeys;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.logics.property.Property;
import lsfusion.server.physics.admin.Settings;

import java.util.function.BiFunction;

public class RecursiveJoin extends QueryJoin<KeyExpr, RecursiveJoin.Query, RecursiveJoin, RecursiveJoin.QueryOuterContext> {

    public static class Query extends QueryJoin.Query<KeyExpr, Query> {
        private final Where initialWhere;
        private final Where stepWhere;
        private final boolean cyclePossible;
        private final boolean isLogical;
        private final ImRevMap<KeyExpr, KeyExpr> mapIterate;

        private final boolean noInnerFollows;

        public Query(InnerExprFollows<KeyExpr> follows, Where initialWhere, Where stepWhere, boolean cyclePossible, boolean isLogical, ImRevMap<KeyExpr, KeyExpr> mapIterate, boolean noInnerFollows) {
            super(follows);
            this.initialWhere = initialWhere;
            this.stepWhere = stepWhere;
            this.cyclePossible = cyclePossible;
            this.isLogical = isLogical;
            this.mapIterate = mapIterate;

            this.noInnerFollows = noInnerFollows;
        }

        public Query(Query query, Where initialWhere) {
            super(query.follows);
            this.initialWhere = initialWhere;
            stepWhere = query.stepWhere;
            mapIterate = query.mapIterate;
            cyclePossible = query.cyclePossible;
            isLogical = query.isLogical;
            
            assert !query.noInnerFollows;
            noInnerFollows = query.noInnerFollows;
        }

        public Query(Query query, MapTranslate translator) {
            super(query, translator);
            this.initialWhere = query.initialWhere.translateOuter(translator);
            this.stepWhere = query.stepWhere.translateOuter(translator);
            this.cyclePossible = query.cyclePossible;
            this.isLogical = query.isLogical;
            this.mapIterate = translator.translateRevMap(query.mapIterate);
            
            this.noInnerFollows = query.noInnerFollows;
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return super.calcTwins(o) && initialWhere.equals(((Query) o).initialWhere) && stepWhere.equals(((Query) o).stepWhere) && mapIterate.equals(((Query) o).mapIterate) && cyclePossible==((Query)o).cyclePossible && isLogical==((Query)o).isLogical && noInnerFollows==((Query)o).noInnerFollows;
        }

        protected boolean isComplex() {
            return true;
        }
        public int hash(HashContext hash) {
            return 31 * (31 * (31 * (31 * (31 * (31 * super.hash(hash) + hashMapOuter(mapIterate, hash)) + initialWhere.hashOuter(hash)) + stepWhere.hashOuter(hash)) + (isLogical ? 1 : 0)) + (cyclePossible ? 1 : 0)) + (noInnerFollows ? 1 : 0);
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

    public RecursiveJoin(ImSet<KeyExpr> keys, ImSet<Value> values, Where initialWhere, Where stepWhere, ImRevMap<KeyExpr, KeyExpr> mapIterate, boolean cyclePossible, boolean isLogical, ImMap<KeyExpr, BaseExpr> group, boolean noInnerFollows) {
        super(keys, values, new Query(InnerExprFollows.EMPTYEXPR(), initialWhere, stepWhere, cyclePossible, isLogical, mapIterate, noInnerFollows), group);
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
        return getRecClassesStats(StatType.DEFAULT).first.first;
    }

    @IdentityLazy
    public InnerExprFollows<KeyExpr> getInnerFollows() {
        if(Settings.get().isDisableInnerFollows() || query.noInnerFollows)
            return InnerExprFollows.EMPTYEXPR();

        ImSet<KeyExpr> groupKeys = group.keys();
        return new InnerExprFollows<>(getClassWhere().get(groupKeys.toRevMap()), groupKeys);
    }

    public StatKeys<KeyExpr> getStatKeys(StatType type) {
        return getStatKeys(type, StatKeys.NOPUSH());
    }

    public StatKeys<KeyExpr> getStatKeys(StatType type, StatKeys<KeyExpr> pushStatKeys) {
        return getRecClassesStats(type, pushStatKeys).first.second;
    }

    public boolean isOnlyInitial() {
        return getRecClassesStats(StatType.DEFAULT).second;
    }

    private StatKeys<KeyExpr> getStatKeys(Where where, StatType type, StatKeys<KeyExpr> pushStatKeys) {
        return PartitionJoin.getStatKeys(where, group.keys(), type, pushStatKeys);
    }

    private Pair<Pair<ClassExprWhere, StatKeys<KeyExpr>>, Boolean> getRecClassesStats(StatType statType) {
        return getRecClassesStats(statType, StatKeys.NOPUSH());
    }

    // теоретически можно было бы разными прогонами, но тогда функциональщиной пришлось бы заниматься, плюс непонятно как подставлять друг другу статистику / классы
    @IdentityLazy
    private Pair<Pair<ClassExprWhere, StatKeys<KeyExpr>>, Boolean> getRecClassesStats(StatType statType, StatKeys<KeyExpr> pushStatKeys) {
        Where initialWhere = getInitialWhere();
        ClassExprWhere recClasses = getClassWhere(initialWhere);
        StatKeys<KeyExpr> recStats = getStatKeys(initialWhere, statType, pushStatKeys);

        ClassExprWhere resultClasses = recClasses;
        StatKeys<KeyExpr> resultStats = recStats;

        Where stepWhere = getStepWhere();
        boolean onlyInitial = true;

        MAddSet<ClassExprWhere> mCheckedClasses = SetFact.mAddSet();
        MAddSet<StatKeys<KeyExpr>> mCheckedStats = SetFact.mAddSet();

        int iterations = 0; int maxStatsIterations = Settings.get().getMaxRecursionStatsIterations();
        while(!recClasses.isFalse() && !(mCheckedClasses.add(recClasses) && (iterations >= maxStatsIterations || mCheckedStats.add(recStats)))) {
            Where recWhere = stepWhere.and(getRecJoin(MapFact.EMPTY(), "recursivetable", genKeyNames(),
                    recClasses, recStats, null, null).getWhere());
            if(!recWhere.isFalse()) // значит будет еще итерация
                onlyInitial = false;
            recClasses = getClassWhere(recWhere);
            recStats = getStatKeys(recWhere, statType, StatKeys.NOPUSH()); // тут можно было бы и pushStatKeys, но нет особого смысла

            resultClasses = recClasses.or(resultClasses);
            resultStats = recStats.or(resultStats);
            iterations++;
        }
        return new Pair<>(new Pair<>(resultClasses, resultStats), onlyInitial);
    }

    // для recursive join статистика рекурсивной таблицы делается заведомо маленькой, так как после первых операций, как правило рекурсия начинает сходится и количество записей начинает резко падать
    public Join<String> getRecJoin(ImMap<String, Type> props, String name, ImRevMap<String, KeyExpr> keyNames, Stat adjustStat, Result<RecursiveTable> recursiveTable) {
        StatKeys<KeyExpr> statKeys = getStatKeys(StatType.ADJUST_RECURSION);
        return getRecJoin(props, name, keyNames, getClassWhere(), statKeys, adjustStat, recursiveTable);
    }

    public Join<String> getRecJoin(ImMap<String, Type> props, String name, ImRevMap<String, KeyExpr> keyNames, final ClassExprWhere classWhere, StatKeys<KeyExpr> statKeys, Stat adjustStat, Result<RecursiveTable> rRecTable) {

        // генерируем поля таблицы
        ImRevMap<KeyField, KeyExpr> recKeys = keyNames.mapRevKeys((keyExpr, name1) -> new KeyField(name1, classWhere.getKeyType(keyExpr)));
        // assert что пустое если logical рекурсия
        ImRevMap<String, PropertyField> recProps = props.mapRevValues((BiFunction<String, Type, PropertyField>) PropertyField::new);

        TableStatKeys tableStatKeys = TableStatKeys.createForTable(statKeys.mapBack(recKeys));
        if(adjustStat != null)
            tableStatKeys = tableStatKeys.decrease(adjustStat);
        RecursiveTable recTable = new RecursiveTable(name, recKeys.keys(), // recProps.valuesSet(),
                classWhere.mapClasses(recKeys), tableStatKeys, query.noInnerFollows);
        if(rRecTable != null)
            rRecTable.set(recTable);

        return new RemapJoin<>(recTable.join(recKeys.join(getFullMapIterate())), recProps); // mapp'им на предыдушие ключи
    }

    public ImRevMap<String, KeyExpr> genKeyNames() {
        return group.keys().mapRevKeys(new CompiledQuery.GenFieldNameIndex("rk", ""));
    }

    private ImRevMap<KeyExpr, KeyExpr> getFullMapIterate() {
        final ImRevMap<KeyExpr, KeyExpr> mapIterate = getMapIterate();
        return group.keys().mapRevValues((KeyExpr recKey) -> BaseUtils.nvl(mapIterate.get(recKey), recKey));
    }
    public Where getIsClassWhere() {
        return getClassWhere().mapClasses(group.keys().toRevMap()).getWhere(getFullMapIterate(), false, query.noInnerFollows ? IsClassType.VIRTUAL : IsClassType.CONSISTENT);
    }

    @Override
    public StatKeys<KeyExpr> getPushedStatKeys(StatType type, StatKeys<KeyExpr> pushStatKeys) {
        return getStatKeys(type, pushStatKeys);
    }

    @Override
    public ImSet<KeyExpr> getPushKeys(ImSet<KeyExpr> pushKeys) {
        return pushKeys.remove(getMapIterate().keys());
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
