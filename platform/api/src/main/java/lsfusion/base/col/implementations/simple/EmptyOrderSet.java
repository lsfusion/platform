package lsfusion.base.col.implementations.simple;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

public class EmptyOrderSet<K> extends AOrderSet<K> {

    private final static EmptyOrderSet<Object> instance = new EmptyOrderSet<Object>();
    public static <K> EmptyOrderSet<K> INSTANCE() {
        return (EmptyOrderSet<K>) instance;
    }
    private EmptyOrderSet() {
    }

    public int size() {
        return 0;
    }

    public K get(int i) {
        throw new UnsupportedOperationException();
    }

    public ImSet<K> getSet() {
        return SetFact.EMPTY();
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return EmptyOrderMap.INSTANCE();
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() {
        return EmptyRevMap.INSTANCE();
    }
}
