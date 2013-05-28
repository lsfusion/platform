package platform.base.col.interfaces.mutable;

public abstract class SimpleAddValue<K, V> implements AddValue<K, V> {

    public boolean stopWhenNull() {
        return false;
    }
}
