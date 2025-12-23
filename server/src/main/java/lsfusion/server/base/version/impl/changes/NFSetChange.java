package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;

import java.util.Set;

public interface NFSetChange<T> extends NFChange<T> {

    void proceedSet(Set<T> mSet, Version version);
}
