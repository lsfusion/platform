package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;

public interface ValuesContext<T extends ValuesContext<T>> extends TranslateValues<T> {

    int hashValues(HashValues hashValues);

    ImSet<Value> getContextValues();

    BaseUtils.HashComponents<Value> getValueComponents(); // по сути protected
}
