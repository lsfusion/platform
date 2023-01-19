package lsfusion.server.base.version.impl.changes;

import java.util.List;

public interface NFComplexOrderSetChange<T> extends NFChange<T> {

    void proceedComplexOrderSet(List<T> list, List<Integer> groupList);
}
