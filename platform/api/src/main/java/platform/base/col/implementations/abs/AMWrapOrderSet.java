package platform.base.col.implementations.abs;

import platform.base.col.implementations.HSet;
import platform.base.col.interfaces.immutable.ImSet;

// если аналогичный Set сам по себе ordered
public abstract class AMWrapOrderSet<K, W extends AMSet<K>> extends AMOrderSet<K> {

    public final W wrapSet; // public чисто для "дружественного" класса *OrderMap

    public AMWrapOrderSet(W wrapSet) {
        this.wrapSet = wrapSet;
    }

    public K get(int i) {
        return wrapSet.get(i);
    }

    public int size() {
        return wrapSet.size();
    }

    public ImSet<K> getSet() {
        return wrapSet;
    }

    public void add(K key) {
        wrapSet.add(key);
    }

    public void exclAdd(K key) {
        wrapSet.exclAdd(key);
    }
}
