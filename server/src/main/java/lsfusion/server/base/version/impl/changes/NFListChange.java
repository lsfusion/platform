package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.base.version.Version;

public interface NFListChange<T> extends NFColChange<T> {
    
    void proceedList(MList<T> list, Version version);
}
