package lsfusion.server.base.version.impl.changes;

import java.util.Set;

public interface NFSetChange<T> extends NFChange<T> {

    void proceedSet(Set<T> mSet);
}
