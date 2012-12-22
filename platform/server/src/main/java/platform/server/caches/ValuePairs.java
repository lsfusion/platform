package platform.server.caches;

import platform.base.GlobalObject;
import platform.base.GroupPairs;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.Settings;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;

public class ValuePairs extends GroupPairs<GlobalObject, Value, MapValuesTranslate> {

    protected MapValuesTranslate createI(ImRevMap<Value, Value> map) {
        return new MapValuesTranslator(map);
    }

    public ValuePairs(ImMap<Value, GlobalObject> map1, ImMap<Value, GlobalObject> map2) {
        super(map1, map2, true, Settings.instance.getMapInnerMaxIterations());
    }
}
