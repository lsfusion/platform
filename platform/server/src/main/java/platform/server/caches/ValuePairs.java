package platform.server.caches;

import platform.base.GroupPairs;
import platform.server.Settings;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;

import java.util.Map;

public class ValuePairs extends GroupPairs<Integer, Value, MapValuesTranslate> {

    protected MapValuesTranslate createI(Map<Value, Value> map) {
        return new MapValuesTranslator(map);
    }

    public ValuePairs(Map<Value, Integer> map1, Map<Value, Integer> map2) {
        super(map1, map2, true, Settings.instance.getMapInnerMaxIterations());
    }
}
