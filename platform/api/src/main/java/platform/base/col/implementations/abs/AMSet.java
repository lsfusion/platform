package platform.base.col.implementations.abs;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MExclSet;
import platform.base.col.interfaces.mutable.MFilterSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.add.MAddSet;

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

    public void keep(K element) {
        exclAdd(element);
    }
}
