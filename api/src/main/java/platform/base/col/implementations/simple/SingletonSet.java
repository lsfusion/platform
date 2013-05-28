package platform.base.col.implementations.simple;

import platform.base.col.implementations.abs.ASet;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class SingletonSet<K> extends ASet<K> {
    
    private final K key;

    public SingletonSet(K key) {
//        assert !(key instanceof ImmutableObject);
        this.key = key;
    }

    public int size() {
        return 1;
    }

    public K get(int i) {
        assert i==0;
        return key;
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new SingletonRevMap<K, M>(key);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new SingletonRevMap<K, M>(key);
    }

    @Override
    public ImOrderSet<K> toOrderSet() {
        return new SingletonOrderSet<K>(this);
    }
}
