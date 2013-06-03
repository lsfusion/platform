package lsfusion.base.col.implementations.simple;

import lsfusion.base.col.implementations.abs.AOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

public class SingletonOrderSet<K> extends AOrderSet<K> {

    private SingletonSet<K> set;

    public SingletonOrderSet(K key) {
        set = new SingletonSet<K>(key);
    }

    public SingletonOrderSet(SingletonSet<K> set) {
        this.set = set;
    }

    public int size() {
        return 1;
    }

    public K get(int i) {
        assert i==0;
        return set.get(0);
    }

    public ImSet<K> getSet() {
        return set;
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new SingletonOrderMap<K, M>(set.get(0));
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() {
        return new SingletonRevMap<K, M>(set.get(0));
    }
}
