package platform.server.data.query;

import platform.server.data.expr.KeyExpr;

import java.util.Map;

public interface MapKeysInterface<T> {

    Map<T, KeyExpr> getMapKeys();
}
