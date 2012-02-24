package platform.server.data.expr.query;

import platform.base.*;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Value;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Join;
import platform.server.data.query.RemapJoin;
import platform.server.data.query.stat.*;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;

import static platform.base.BaseUtils.reverse;

public class RecursiveJoin extends QueryJoin<KeyExpr, RecursiveJoin.Query, RecursiveJoin, RecursiveJoin.QueryOuterContext> {

    public static class Query extends AbstractOuterContext<Query> {
        private final Where initialWhere;
        private final Where stepWhere;
        private final boolean cyclePossible;
        private final boolean isLogical;
        private final Map<KeyExpr, KeyExpr> mapIterate;

        public Query(Where initialWhere, Where stepWhere, Map<KeyExpr, KeyExpr> mapIterate, boolean cyclePossible, boolean isLogical) {
            this.initialWhere = initialWhere;
            this.stepWhere = stepWhere;
            this.mapIterate = mapIterate;
            this.cyclePossible = cyclePossible;
            this.isLogical = isLogical;
        }

        public boolean twins(TwinImmutableInterface o) {
            return initialWhere.equals(((Query) o).initialWhere) && stepWhere.equals(((Query) o).stepWhere) && mapIterate.equals(((Query) o).mapIterate) && cyclePossible==((Query)o).cyclePossible && isLogical==((Query)o).isLogical;
        }

        protected boolean isComplex() {
            return true;
        }
        protected int hash(HashContext hash) {
            return 31 * (31 * (31 * (31 * hashMapOuter(mapIterate, hash) + initialWhere.hashOuter(hash)) + stepWhere.hashOuter(hash)) + (isLogical ? 1 : 0)) + (cyclePossible ? 1 : 0);
        }
        protected Query translate(MapTranslate translator) {
            return new Query(initialWhere.translateOuter(translator),stepWhere.translateOuter(translator), translator.translateMap(mapIterate), cyclePossible, isLogical);
        }
        public QuickSet<OuterContext> calculateOuterDepends() {
            return new QuickSet<OuterContext>(initialWhere, stepWhere);
        }
    }

    public RecursiveJoin(RecursiveJoin join, MapTranslate translator) {
        super(join, translator);
    }

    public RecursiveJoin(QuickSet<KeyExpr> keys, QuickSet<Value> values, Where initialWhere, Where stepWhere, Map<KeyExpr, KeyExpr> mapIterate, boolean cyclePossible, boolean isLogical, Map<KeyExpr, BaseExpr> group) {
        super(keys, values, new Query(initialWhere, stepWhere, mapIterate, cyclePossible, isLogical), group);
    }

    public RecursiveJoin(QuickSet<KeyExpr> keys, QuickSet<Value> values, Query inner, Map<KeyExpr, BaseExpr> group) {
        super(keys, values, inner, group);
    }
    protected RecursiveJoin createThis(QuickSet<KeyExpr> keys, QuickSet<Value> values, Query query, Map<KeyExpr, BaseExpr> group) {
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
        return where.getClassWhere().keep(group.keySet());
    }

    @IdentityLazy
    public ClassExprWhere getClassWhere() {
        return getInitialClassWhere().or(getClassWhere(getFullStepWhere()));
    }

    @IdentityLazy
    public ClassExprWhere getInitialClassWhere() {
        return getClassWhere(getInitialWhere());
    }

    private StatKeys<KeyExpr> getStatKeys(Where where) {
        return where.getStatKeys(new QuickSet<KeyExpr>(group.keySet()));
    }

    @IdentityLazy
    public StatKeys<KeyExpr> getInitialStatKeys() {
        return getStatKeys(getInitialWhere());
    }

    @IdentityLazy
    public StatKeys<KeyExpr> getStatKeys() {
        return getInitialStatKeys().or(getStatKeys(getFullStepWhere()));
    }

    @IdentityLazy
    public Where getFullStepWhere() {
        return getStepWhere().and(getRecJoin(new HashMap<String, Type>(), "recursivetable", new HashMap<String, KeyExpr>(),
                getInitialClassWhere(), getInitialStatKeys()).getWhere());
    }
    
    public Join<String> getRecJoin(Map<String, Type> props, String name, Map<String, KeyExpr> keys) {
        return getRecJoin(props, name, keys, getClassWhere(), getStatKeys());
    }

    public Join<String> getRecJoin(Map<String, Type> props, String name, Map<String, KeyExpr> keys, ClassExprWhere classWhere, StatKeys<KeyExpr> statKeys) {

        // создаем рекурсивную таблиц
        int i=0;
        Map<KeyExpr, KeyField> recKeys = new HashMap<KeyExpr, KeyField>();
        for(KeyExpr key : group.keySet()) { // подготавливаем ключи
            String keyName = "rk" + (i++);
            recKeys.put(key, new KeyField(keyName, classWhere.getKeyType(key)));
            keys.put(keyName, key);
        }

        Map<PropertyField, String> recProps = new HashMap<PropertyField, String>();
        for(Map.Entry<String, Type> query : props.entrySet()) // assert что пустое если logical рекурсия
            recProps.put(new PropertyField(query.getKey(), query.getValue()), query.getKey());

        RecursiveTable recTable = new RecursiveTable(name, recKeys.values(), recProps.keySet(),
                classWhere.map(recKeys), statKeys.map(recKeys));

        Map<KeyExpr, KeyExpr> mapIterate = getMapIterate();
        Map<KeyField, KeyExpr> joinKeys = new HashMap<KeyField, KeyExpr>();
        for(Map.Entry<KeyExpr, KeyField> recKey : recKeys.entrySet()) {
            KeyExpr prevKey = mapIterate.get(recKey.getKey());
            joinKeys.put(recKey.getValue(), prevKey!=null?prevKey:recKey.getKey());
        }

        return new RemapJoin<String, PropertyField>(recTable.join(joinKeys), reverse(recProps)); // mapp'им на предыдушие ключи
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
    public Map<KeyExpr, KeyExpr> getMapIterate() {
        return query.mapIterate;
    }
    public boolean isCyclePossible() {
        return query.cyclePossible;
    }
}
