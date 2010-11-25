package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.GroupPairs;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;
import platform.server.Settings;

import java.util.Map;
import java.util.Set;

public class ValuePairs extends GroupPairs<ConcreteClass, ValueExpr, MapValuesTranslate> {

    protected MapValuesTranslate createI(Map<ValueExpr, ValueExpr> map) {
        return new MapValuesTranslator(map);
    }

    public ValuePairs(Set<ValueExpr> values1, Set<ValueExpr> values2) {
        super(new BaseUtils.Group<ConcreteClass, ValueExpr>() {
            public ConcreteClass group(ValueExpr key) {
                return key.objectClass;
            }
        }, values1, values2, Settings.instance.getMapInnerMaxIterations());
    }
}
