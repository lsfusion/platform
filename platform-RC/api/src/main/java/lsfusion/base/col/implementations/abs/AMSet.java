package lsfusion.base.col.implementations.abs;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MFilterSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;

public abstract class AMSet<K> extends ASet<K> implements MSet<K>, MExclSet<K>, MAddSet<K>, MFilterSet<K> {

    public void exclAdd(K key) {
        boolean was = add(key);
        assert !was;
    }

    public void exclAddAll(ImSet<? extends K> set) {
        for(int i=0,size=set.size();i<size;i++)
            exclAdd(set.get(i));
    }

    public void addAll(ImSet<? extends K> set) {
        for(int i=0,size=set.size();i<size;i++)
            add(set.get(i));
    }

    public Iterable<K> it() {
        return this;
    }

    public void keep(K element) {
        exclAdd(element);
    }
}
