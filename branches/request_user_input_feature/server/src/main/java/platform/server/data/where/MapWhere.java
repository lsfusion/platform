package platform.server.data.where;

import platform.base.QuickMap;

public class MapWhere<T> extends QuickMap<T,Where> {

    protected Where addValue(T key, Where prevValue, Where newValue) {
        return prevValue.or(newValue);
    }

    protected boolean containsAll(Where who, Where what) {
        throw new RuntimeException("not supported");
    }
}
