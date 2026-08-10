package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;

import java.util.Set;

public class NFAdd<T> implements NFListChange<T>, NFOrderSetChange<T> {
    public final T element;

    public NFAdd(T element) {
        this.element = element;
    }

    public void proceedCol(MCol<T> mCol, Version version) {
        mCol.add(element);
    }
    
    public void proceedSet(Set<T> mSet, Version version) {
        mSet.add(element); // LinkedHashSet.add keeps the position of an element that is already there, which is what the ordered replay needs
    }

    public void proceedList(MList<T> list, Version version) {
        list.add(element);
    }
}
