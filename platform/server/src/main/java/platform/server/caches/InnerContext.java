package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.GlobalObject;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashMapKeys;
import platform.server.caches.hash.HashMapValues;
import platform.server.data.Value;
import platform.server.data.translator.MapTranslate;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public abstract class InnerContext<I extends InnerContext<I>> extends InnerHashContext {

    public abstract Set<Value> getValues();
    
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
        for(MapTranslate translator : new MapContextIterable(this, object, values)) {
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

    private Map<Value, Integer> valueComponents;
    @ManualLazy
    public Map<Value, Integer> getValueComponents() {
        if(valueComponents==null) {
            final HashMapKeys hashKeys = new HashMapKeys(BaseUtils.toMap(getKeys(), 5));
            valueComponents = BaseUtils.getComponents(new BaseUtils.HashInterface<Value, GlobalObject>() {
                public SortedMap<GlobalObject,Set<Value>> getParams() {
                    return AbstractMapValues.getSortedParams(getValues());
                }

                public int hashParams(Map<Value, Integer> map) {
                    return hashInner(new HashContext(hashKeys, map.size()>0?new HashMapValues(map):HashCodeValues.instance));
                }
            }).map;
        }

        return valueComponents;
    }

}
