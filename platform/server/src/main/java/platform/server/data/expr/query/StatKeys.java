package platform.server.data.expr.query;

import platform.base.QuickMap;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.query.SourceJoin;
import platform.server.data.translator.MapTranslate;

import java.awt.image.Kernel;
import java.util.Collection;
import java.util.Set;

public class StatKeys<K> extends QuickMap<K, KeyStat> {

    public StatKeys() {
    }

    public StatKeys(Collection<K> keys, KeyStat value) {
        super(keys, value);
    }

    @Override
    protected KeyStat addValue(KeyStat prevValue, KeyStat newValue) {
        if(newValue.less(prevValue))
            return newValue;
        return null;
    }

    @Override
    protected boolean containsAll(KeyStat who, KeyStat what) {
        throw new RuntimeException("not supported");
    }

    public KeyStat getMaxStat(Collection<K> keys) {
        KeyStat result = KeyStat.FEW;
        for(K key : keys)
            result = result.max(get(key));
        return result;
    }

    public boolean isMin() { // чисто оптимизационный момент
        for(int i=0;i<size;i++)
            if(!getValue(i).isMin())
                return false;
        return true;
    }

    public StatKeys<K> filterNotMin() {
        StatKeys<K> result = new StatKeys<K>();
        for(int i=0;i<size;i++)
            if(!getValue(i).isMin())
                result.add(getKey(i), getValue(i));
        return result;
    }

    public boolean lessEquals(StatKeys<K> statKeys) {
        // все <= или есть <
        for(int i=0;i<size;i++) {
            KeyStat value = getValue(i);
            KeyStat statValue = statKeys.get(getKey(i));
            if(statValue.less(value))
                return false;
        }
        return true;
    }

    public boolean less(StatKeys<K> statKeys) {
        return lessEquals(statKeys) && !equals(statKeys);
    }
}
