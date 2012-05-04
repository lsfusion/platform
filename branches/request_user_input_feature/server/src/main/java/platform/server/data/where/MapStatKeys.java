package platform.server.data.where;

import platform.base.QuickMap;
import platform.server.data.query.stat.StatKeys;

public class MapStatKeys<T, K> extends QuickMap<T, StatKeys<K>> {

    @Override
    protected StatKeys<K> addValue(T key, StatKeys<K> prevValue, StatKeys<K> newValue) {
        return prevValue.or(newValue);
    }

    @Override
    protected boolean containsAll(StatKeys<K> who, StatKeys<K> what) {
        throw new RuntimeException("not supported");
    }
}
