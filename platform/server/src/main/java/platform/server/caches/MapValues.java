package platform.server.caches;

import platform.server.caches.hash.HashValues;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapValuesTranslate;
import platform.base.BaseUtils;

import java.util.Set;

public interface MapValues<T extends MapValues<T>> {

    int hashValues(HashValues hashValues);

    Set<ValueExpr> getValues();

    T translate(MapValuesTranslate mapValues);

    BaseUtils.HashComponents<ValueExpr> getComponents();    
}
