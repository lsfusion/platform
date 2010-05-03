package platform.server.caches;

import platform.server.data.expr.ValueExpr;
import platform.server.caches.hash.HashValues;

import java.util.Set;
import java.util.Map;

public interface MapValues<T extends MapValues<T>> {

    int hashValues(HashValues hashValues);

    Set<ValueExpr> getValues();

    T translate(Map<ValueExpr,ValueExpr> mapValues);
}
