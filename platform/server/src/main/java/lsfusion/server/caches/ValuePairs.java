package lsfusion.server.caches;

import lsfusion.base.GlobalObject;
import lsfusion.base.GroupPairs;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.Settings;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.translator.MapValuesTranslator;

public class ValuePairs extends GroupPairs<GlobalObject, Value, MapValuesTranslate> {

    protected MapValuesTranslate createI(ImRevMap<Value, Value> map) {
        return new MapValuesTranslator(map);
    }

    public ValuePairs(ImMap<Value, GlobalObject> map1, ImMap<Value, GlobalObject> map2) {
        super(map1, map2, true, Settings.get().getMapInnerMaxIterations());
    }
}
