package platform.server.data.expr.where;

import platform.base.QuickMap;
import platform.server.data.where.Where;

public class MapWhere<T> extends QuickMap<T,Where> {

    protected Where addValue(Where prevValue, Where newValue) {
        return prevValue.or(newValue);
    }

    protected boolean containsAll(Where who, Where what) {
        throw new RuntimeException("not supported");
    }
}
