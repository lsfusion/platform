package lsfusion.server.logics.mutables.impl.changes;

import java.util.List;

public interface NFOrderSetChange<T> extends NFSetChange<T> {
    
    void proceedOrderSet(List<T> list);
}
