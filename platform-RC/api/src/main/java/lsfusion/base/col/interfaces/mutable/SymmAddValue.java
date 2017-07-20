package lsfusion.base.col.interfaces.mutable;

public abstract class SymmAddValue<K, V> extends SimpleAddValue<K, V> {

    public boolean reversed() {
        return true;
    }

    public AddValue<K, V> reverse() {
        return this;
    }
}
