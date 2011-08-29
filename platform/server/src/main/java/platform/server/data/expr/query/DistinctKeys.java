package platform.server.data.expr.query;

import platform.base.QuickMap;

import java.util.Map;

public class DistinctKeys<K> extends QuickMap<K, Stat> {

    public DistinctKeys() {
    }

    public DistinctKeys(DistinctKeys<K> copy) {
        super(copy);
    }

    protected Stat addValue(K key, Stat prevValue, Stat newValue) {
        if(newValue.less(prevValue))
            return newValue;
        return null;
    }

    protected boolean containsAll(Stat who, Stat what) {
        throw new RuntimeException("not supported");
    }

    public Stat getMax() {
        Stat result = Stat.ONE;
        for(int i=0;i<size;i++)
            result = result.mult(getValue(i));
        return result;
    }

    public <T> DistinctKeys<T> map(Map<K,T> map) {
        DistinctKeys<T> result = new DistinctKeys<T>();
        for(int i=0;i<size;i++)
            result.add(map.get(getKey(i)), getValue(i));
        return result;
    }

    public DistinctKeys<K> or(DistinctKeys<K> stat) {
        DistinctKeys<K> result = new DistinctKeys<K>();
        for(int i=0;i<size;i++) {
            K key = getKey(i);
            result.add(key, getValue(i).or(stat.get(key)));
        }
        return result;
    }

    public DistinctKeys<K> and(DistinctKeys<K> stat) {
        DistinctKeys<K> result = new DistinctKeys<K>(this);
        addAll(stat);
        return result;
    }
}
