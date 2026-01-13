package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.server.base.version.Version;

public interface NFColChange<T> extends NFChange<T> {
    
    void proceedCol(MCol<T> mCol, Version version);
}
