package lsfusion.base.col.implementations.simple;

import lsfusion.base.col.implementations.abs.ASet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class EmptySet<K> extends ASet<K> {

    private final static EmptySet<Object> instance = new EmptySet<Object>();
    public static <K> EmptySet<K> INSTANCE() {
        return (EmptySet<K>) instance;
    }
    private EmptySet() {
    }

    public int size() {
        return 0;
    }

    public K get(int i) {
        throw new UnsupportedOperationException();
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return EmptyRevMap.INSTANCE();
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return EmptyRevMap.INSTANCE();
    }
}
