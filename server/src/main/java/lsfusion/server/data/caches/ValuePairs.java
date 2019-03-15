package lsfusion.server.data.caches;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.comb.GroupPairs;
import lsfusion.base.comb.map.GlobalObject;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.data.value.Value;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.data.translator.RemapValuesTranslator;

public class ValuePairs extends GroupPairs<GlobalObject, Value, MapValuesTranslate> {

    protected MapValuesTranslate createI(ImRevMap<Value, Value> map) {
        return new RemapValuesTranslator(map);
    }

    public ValuePairs(ImMap<Value, GlobalObject> map1, ImMap<Value, GlobalObject> map2) {
        super(map1, map2, true, Settings.get().getMapInnerMaxIterations());
    }
}
