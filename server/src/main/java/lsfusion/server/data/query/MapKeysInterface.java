package lsfusion.server.data.query;

import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.expr.KeyExpr;

public interface MapKeysInterface<T> {

    ImRevMap<T, KeyExpr> getMapKeys();
}
