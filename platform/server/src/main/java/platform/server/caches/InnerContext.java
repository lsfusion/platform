package platform.server.caches;

import platform.base.Result;
import platform.base.BaseUtils;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.caches.hash.*;
import platform.server.classes.ConcreteClass;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.SortedMap;

public abstract class InnerContext<I extends InnerContext<I>> extends InnerHashContext {

    public abstract Set<ValueExpr> getValues();
    
    public abstract I translateInner(MapTranslate translate);
    // проверка на соответствие если одинаковые контексты
    public abstract boolean equalsInner(I object);

    public MapTranslate mapInner(I object, boolean values) {
        Result<MapTranslate> mapTranslate = new Result<MapTranslate>();
        if(mapInner(object, values, mapTranslate)!=null)
            return mapTranslate.result;
        else
            return null;
    }

    public I mapInner(I object, boolean values, Result<MapTranslate> mapTranslate) {
        for(MapTranslate translator : new MapParamsIterable(this, object, values)) {
            I transContext = translateInner(translator);
            if(transContext.equalsInner(object)) {
                mapTranslate.set(translator);
                return transContext;
            }
        }
        return null;
    }

    public int hashInner(boolean values) {
        return hashInner(values?new HashMapValues(getValueComponents()):HashCodeValues.instance);
    }

    private Map<ValueExpr, Integer> valueComponents;
    @ManualLazy
    public Map<ValueExpr, Integer> getValueComponents() {
        if(valueComponents==null) {
            final HashMapKeys hashKeys = new HashMapKeys(BaseUtils.toMap(getKeys(), 5));
            valueComponents = BaseUtils.getComponents(new BaseUtils.HashInterface<ValueExpr, ConcreteClass>() {
                public SortedMap<ConcreteClass,Set<ValueExpr>> getParams() {
                    return AbstractMapValues.getSortedParams(getValues());
                }

                public int hashParams(Map<ValueExpr, Integer> map) {
                    return hashInner(new HashContext(hashKeys, map.size()>0?new HashMapValues(map):HashCodeValues.instance));
                }
            }).map;
        }

        return valueComponents;
    }

}
