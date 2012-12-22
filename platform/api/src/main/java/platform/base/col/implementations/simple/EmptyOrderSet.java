package platform.base.col.implementations.simple;

import platform.base.col.SetFact;
import platform.base.col.implementations.abs.AOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import platform.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

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
