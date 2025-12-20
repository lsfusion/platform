package lsfusion.server.base.version.impl.changes;

import lsfusion.server.base.version.Version;

import java.util.List;

public interface NFComplexOrderSetChange<T> extends NFChange<T> {

    void proceedComplexOrderSet(List<T> list, List<Integer> groupList, NFComplexOrderSetChange<T> nextChange, Version version);

    T getRemoveElement(); // optimization, for adding and then immediate moving / removing
}
