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
import lsfusion.server.caches.AbstractOuterContext;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.OuterContext;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.KeyField;
import lsfusion.server.data.PropertyField;
import lsfusion.server.data.Value;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.CompiledQuery;
import lsfusion.server.data.query.Join;
import lsfusion.server.data.query.RemapJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.classes.ClassExprWhere;

public class RecursiveJoin extends QueryJoin<KeyExpr, RecursiveJoin.Query, RecursiveJoin, RecursiveJoin.QueryOuterContext> {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where initialWhere;
        private final Where stepWhere;
        private final boolean cyclePossible;
        private final boolean isLogical;
        private final ImRevMap<KeyExpr, KeyExpr> mapIterate;

        public Query(Where initialWhere, Where stepWhere, ImRevMap<KeyExpr, KeyExpr> mapIterate, boolean cyclePossible, boolean isLogical) {
            this.initialWhere = initialWhere;
            this.stepWhere = stepWhere;
            this.mapIterate = mapIterate;
            this.cyclePossible = cyclePossible;
            this.isLogical = isLogical;
        }

        public Query(Query query, Where initialWhere) {
            this.initialWhere = initialWhere;
            stepWhere = query.stepWhere;
            mapIterate = query.mapIterate;
            cyclePossible = query.cyclePossible;
            isLogical = query.isLogical;
        }

        public boolean twins(TwinImmutableObject o) {
            return initialWhere.equals(((Query) o).initialWhere) && stepWhere.equals(((Query) o).stepWhere) && mapIterate.equals(((Query) o).mapIterate) && cyclePossible==((Query)o).cyclePossible && isLogical==((Query)o).isLogical;
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hash) {
            return 31 * (31 * (31 * (31 * hashMapOuter(mapIterate, hash) + initialWhere.hashOuter(hash)) + stepWhere.hashOuter(hash)) + (isLogical ? 1 : 0)) + (cyclePossible ? 1 : 0);
        }
        protected Query translate(MapTranslate translator) {
            return new Query(initialWhere.translateOuter(translator),stepWhere.translateOuter(translator), translator.translateRevMap(mapIterate), cyclePossible, isLogical);
        }
        public ImSet<OuterContext> calculateOuterDepends() {
            return SetFact.<OuterContext>toSet(initialWhere, stepWhere);
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
        super(keys, values, new Query(initialWhere, stepWhere, mapIterate, cyclePossible, isLogical), group);
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
        Pair<ClassExprWhere, StatKeys<KeyExpr>> recursive = new Pair<ClassExprWhere, StatKeys<KeyExpr>>(getClassWhere(getInitialWhere()), getStatKeys(getInitialWhere()));
        Pair<ClassExprWhere, StatKeys<KeyExpr>> result = recursive;

        Where stepWhere = getStepWhere();
        boolean onlyInitial = true;

        MAddSet<Pair<ClassExprWhere, StatKeys<KeyExpr>>> mChecked = SetFact.mAddSet();

        while(!recursive.first.isFalse() && !mChecked.add(recursive)) {
            Where recWhere = stepWhere.and(getRecJoin(MapFact.<String, Type>EMPTY(), "recursivetable", new Result<ImRevMap<String, KeyExpr>>(),
                    recursive.first, recursive.second).getWhere());
            if(!recWhere.isFalse()) // значит будет еще итерация
                onlyInitial = false;
            recursive = new Pair<ClassExprWhere, StatKeys<KeyExpr>>(getClassWhere(recWhere), getStatKeys(recWhere));
            result = new Pair<ClassExprWhere, StatKeys<KeyExpr>>(recursive.first.or(result.first), recursive.second.or(result.second));
        }
        return new Pair<Pair<ClassExprWhere, StatKeys<KeyExpr>>, Boolean>(result, onlyInitial);
    }

    public Join<String> getRecJoin(ImMap<String, Type> props, String name, Result<ImRevMap<String, KeyExpr>> keys) {
        return getRecJoin(props, name, keys, getClassWhere(), getStatKeys());
    }

    public Join<String> getRecJoin(ImMap<String, Type> props, String name, Result<ImRevMap<String, KeyExpr>> keys, final ClassExprWhere classWhere, StatKeys<KeyExpr> statKeys) {

        // генерируем имена
        ImRevMap<String, KeyExpr> keyNames = group.keys().mapRevKeys(new CompiledQuery.GenNameIndex("rk", ""));
        keys.set(keyNames);
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
                classWhere.map(recKeys), statKeys.mapBack(recKeys));

        return new RemapJoin<String, PropertyField>(recTable.join(recKeys.join(getFullMapIterate())), recProps); // mapp'им на предыдушие ключи
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
        return getClassWhere().map(group.keys().toRevMap()).getWhere(getFullMapIterate());
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
