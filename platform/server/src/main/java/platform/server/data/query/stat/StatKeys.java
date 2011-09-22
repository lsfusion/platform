package platform.server.data.query.stat;

import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.query.DistinctKeys;
import platform.server.data.expr.query.Stat;
import platform.server.data.translator.MapTranslate;

import java.util.Map;
import java.util.Set;

public class StatKeys<K> extends TwinImmutableObject {

    public final Stat rows;

    public final DistinctKeys<K> distinct;

    public StatKeys(Set<K> allKeys) {
        this(allKeys, Stat.MIN);
    }

    public StatKeys(Set<K> allKeys, Stat stat) {
        rows = stat;
        distinct = new DistinctKeys<K>();
        for(K key : allKeys)
            distinct.add(key, stat);
    }

    public StatKeys(Stat rows, DistinctKeys<K> distinct) {
        this.rows = rows;
        this.distinct = distinct;
    }

    public <T> StatKeys<T> map(Map<K,T> map) {
        return new StatKeys<T>(rows, distinct.map(map));
    }

    public StatKeys<K> or(StatKeys<K> stat) {
        return new StatKeys<K>(rows.or(stat.rows), distinct.or(stat.distinct));
    }

    public StatKeys<K> and(StatKeys<K> stat) { // assert что ключи не пересекаются
        return new StatKeys<K>(rows.mult(stat.rows), distinct.and(stat.distinct));
    }

    public static <K extends BaseExpr> int hashOuter(StatKeys<K> statKeys, HashContext hashContext) {
        int hash = 0;
        for(int i=0;i<statKeys.distinct.size;i++)
            hash += statKeys.distinct.getKey(i).hashOuter(hashContext) ^ statKeys.distinct.getValue(i).hashCode();
        return hash;
    }

    public static <K extends BaseExpr> StatKeys<K> translateOuter(StatKeys<K> statKeys, MapTranslate translator) {
        DistinctKeys<K> transKeys = new DistinctKeys<K>();
        for(int i=0;i<statKeys.distinct.size;i++)
            transKeys.add((K) statKeys.distinct.getKey(i).translateOuter(translator), statKeys.distinct.getValue(i));
        return new StatKeys<K>(statKeys.rows, transKeys);
    }

    public boolean twins(TwinImmutableInterface o) {
        return rows.equals(((StatKeys)o).rows) && distinct.equals(((StatKeys)o).distinct);
    }

    public int immutableHashCode() {
        return 31 * rows.hashCode() + distinct.hashCode();
    }
}

