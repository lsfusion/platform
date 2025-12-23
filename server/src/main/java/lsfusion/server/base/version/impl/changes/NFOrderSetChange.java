package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;

import java.util.List;

public interface NFOrderSetChange<T> extends NFSetChange<T> {
    
    void proceedOrderSet(List<T> list, Version version);
}
