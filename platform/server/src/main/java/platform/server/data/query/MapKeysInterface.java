package platform.server.data.query;

import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.data.expr.KeyExpr;

public interface MapKeysInterface<T> {

    ImRevMap<T, KeyExpr> getMapKeys();
}
