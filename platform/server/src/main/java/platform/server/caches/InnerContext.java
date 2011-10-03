package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.base.GlobalObject;
import platform.base.GlobalInteger;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashMapKeys;
import platform.server.caches.hash.HashMapValues;
import platform.server.data.Value;
import platform.server.data.translator.MapTranslate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class InnerContext<I extends InnerContext<I>> extends InnerHashContext {

    public abstract Set<Value> getValues();

    public static Map<Value, Value> getBigValues(Set<Value> values) {
        Set<Value> usedValues = new HashSet<Value>(values);

        Map<Value, Value> result = new HashMap<Value, Value>();
        for(Value value : values) {
            Value removeValue = value.removeBig(usedValues);
            if(removeValue!=null) {
                result.put(value, removeValue);
                usedValues.add(removeValue);
            }
        }
        if(result.isEmpty())
            return null;

        for(Value value : values)
            if(!result.containsKey(value))
                result.put(value, value);
        return result;
    }

    public Map<Value, Value> getBigValues() {
        return getBigValues(getValues());
    }

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

    private final GlobalInteger keyValueHash = new GlobalInteger(5);

    private Map<Value, GlobalObject> valueComponents;
    @ManualLazy
    public Map<Value, GlobalObject> getValueComponents() {
        if(valueComponents==null) {
            final HashMapKeys hashKeys = new HashMapKeys(BaseUtils.toMap(getKeys(), keyValueHash));
            valueComponents = BaseUtils.getComponents(new BaseUtils.HashInterface<Value, GlobalObject>() {
                public Map<Value, GlobalObject> getParams() {
                    return AbstractMapValues.getParamClasses(getValues());
                }

                public int hashParams(Map<Value, ? extends GlobalObject> map) {
                    return hashInner(new HashContext(hashKeys, map.size()>0?new HashMapValues(map):HashCodeValues.instance));
                }
            }).map;
        }

        return valueComponents;
    }

}
