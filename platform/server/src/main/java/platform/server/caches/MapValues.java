package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.GlobalObject;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

import java.util.Set;

public interface MapValues<T extends MapValues<T>> {

    int hashValues(HashValues hashValues);

    Set<Value> getValues();

    T translate(MapValuesTranslate mapValues);

    BaseUtils.HashComponents<Value> getComponents();    
}
