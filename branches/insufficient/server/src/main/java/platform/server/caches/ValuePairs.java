package platform.server.caches;

import platform.base.GroupPairs;
import platform.base.BaseUtils;
import platform.base.GlobalObject;
import platform.server.Settings;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;

import java.util.Map;

public class ValuePairs extends GroupPairs<GlobalObject, Value, MapValuesTranslate> {

    protected MapValuesTranslate createI(Map<Value, Value> map) {
        return new MapValuesTranslator(map);
    }

    public ValuePairs(Map<Value, GlobalObject> map1, Map<Value, GlobalObject> map2) {
        super(map1, map2, true, Settings.instance.getMapInnerMaxIterations());
    }
}
