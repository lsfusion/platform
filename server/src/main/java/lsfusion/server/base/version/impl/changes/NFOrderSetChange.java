package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;

import java.util.Set;

public interface NFOrderSetChange<T> extends NFSetChange<T> {

    // the ordered replay is a LinkedHashSet, so for every change but a copy - which reads a different view of its source -
    // it is the same operation on the same interface as the unordered one
    default void proceedOrderSet(Set<T> set, Version version) {
        proceedSet(set, version);
    }
}
