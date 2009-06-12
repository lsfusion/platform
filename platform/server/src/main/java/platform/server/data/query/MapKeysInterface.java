package platform.server.data.query;

import platform.server.data.query.exprs.KeyExpr;

import java.util.Map;

public interface MapKeysInterface<T> {

    Map<T, KeyExpr> getMapKeys();
}
