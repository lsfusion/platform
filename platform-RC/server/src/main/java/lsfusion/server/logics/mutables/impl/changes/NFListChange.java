package lsfusion.server.logics.mutables.impl.changes;

import lsfusion.base.col.interfaces.mutable.MList;

public interface NFListChange<T> extends NFColChange<T> {
    
    void proceedList(MList<T> list);
}
