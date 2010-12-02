package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.GroupPairs;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;
import platform.server.Settings;

import java.util.Map;
import java.util.Set;

public class ValuePairs extends GroupPairs<Integer, ValueExpr, MapValuesTranslate> {

    protected MapValuesTranslate createI(Map<ValueExpr, ValueExpr> map) {
        return new MapValuesTranslator(map);
    }

    public ValuePairs(Map<ValueExpr, Integer> map1, Map<ValueExpr, Integer> map2) {
        super(map1, map2, true, Settings.instance.getMapInnerMaxIterations());
    }
}
