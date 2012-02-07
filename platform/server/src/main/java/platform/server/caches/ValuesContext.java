package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.GlobalObject;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

import java.util.Map;
import java.util.Set;

public interface ValuesContext<T extends ValuesContext<T>> extends TranslateValues<T> {

    int hashValues(HashValues hashValues);

    QuickSet<Value> getContextValues();

    BaseUtils.HashComponents<Value> getValueComponents(); // по сути protected
}
