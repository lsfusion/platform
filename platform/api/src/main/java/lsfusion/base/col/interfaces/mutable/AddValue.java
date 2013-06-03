package lsfusion.base.col.interfaces.mutable;

public interface AddValue<K, V> {

    V addValue(K key, V prevValue, V newValue); // если возвращает null

    boolean symmetric();
    boolean stopWhenNull();
}
