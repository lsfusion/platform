package lsfusion.base.col.interfaces.mutable.add;

public interface MAddCol<K> {

    Iterable<K> it(); // редкое использование поэтому не extends
    boolean isEmpty();

    void add(K element);
}
