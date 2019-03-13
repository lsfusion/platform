package lsfusion.server.base.caches;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.caches.hash.HashValues;
import lsfusion.server.data.Value;

public interface ValuesContext<T extends ValuesContext<T>> extends TranslateValues<T> {

    int hashValues(HashValues hashValues);

    ImSet<Value> getContextValues();

    BaseUtils.HashComponents<Value> getValueComponents(); // по сути protected
}
