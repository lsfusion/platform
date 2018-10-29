package lsfusion.base.col.interfaces.mutable;

public interface MAddExclSet<K> {
    void exclAdd(K key);

    K get(int i);
    int size();
}
