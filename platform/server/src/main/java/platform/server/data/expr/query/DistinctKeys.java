package platform.server.data.expr.query;

import platform.base.col.WrapMap;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.AddValue;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapTranslate;

public class DistinctKeys<K> extends WrapMap<K, Stat> {

    public DistinctKeys(ImMap<K, Stat> map) {
        super(map);
    }

    public Stat getMax() {
        Stat result = Stat.ONE;
        for(int i=0,size=size();i<size;i++)
            result = result.mult(getValue(i));
        return result;
    }

    public <T> DistinctKeys<T> mapBack(ImMap<T, K> map) {
        return new DistinctKeys<T>(map.mapValues(new GetValue<Stat, K>() {
            public Stat getMapValue(K value) {
                return get(value);
            }
        }));
    }

    public DistinctKeys<K> or(final DistinctKeys<K> stat) {
        return new DistinctKeys<K>(mapValues(new GetKeyValue<Stat, K, Stat>() {
            public Stat getMapValue(K key, Stat value) {
                return value.or(stat.get(key));
            }}));
    }

    public static <K extends Expr> int hashOuter(DistinctKeys<K> distinct, HashContext hashContext) {
        int hash = 0;
        for(int i=0,size=distinct.size();i<size;i++)
            hash += distinct.getKey(i).hashOuter(hashContext) ^ distinct.getValue(i).hashCode();
        return hash;
    }

    public static <K extends Expr> DistinctKeys<K> translateOuter(DistinctKeys<K> distinct, MapTranslate translator) {
        return new DistinctKeys<K>(translator.translateExprKeys(distinct.map));
    }

}
