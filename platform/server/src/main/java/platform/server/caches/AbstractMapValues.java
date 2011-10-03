package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.base.GlobalObject;
import platform.base.TwinImmutableObject;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashMapValues;
import platform.server.data.Value;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public abstract class AbstractMapValues<U extends AbstractMapValues<U>> extends TwinImmutableObject implements MapValues<U>  {

    public int immutableHashCode() {
        return hashValues(HashCodeValues.instance);
    }

    public Map<Value, Value> getBigValues() {
        return InnerContext.getBigValues(getValues());
    }

    public static Map<Value, GlobalObject> getParamClasses(Set<Value> values) {
        Map<Value, GlobalObject> result = new HashMap<Value, GlobalObject>();
        for(Value value : values)
            result.put(value, value.getValueClass());
        return result;
    }

    // множественные наследование
    public static BaseUtils.HashComponents<Value> getComponents(final MapValues<?> mapValues) {
        return BaseUtils.getComponents(new BaseUtils.HashInterface<Value, GlobalObject>() {
            public Map<Value, GlobalObject> getParams() {
                return getParamClasses(mapValues.getValues());
            }

            public int hashParams(Map<Value, ? extends GlobalObject> map) {
                return mapValues.hashValues(map.size()>0?new HashMapValues(map): HashCodeValues.instance);
            }
        });
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getComponents() {
        if(components==null)
            components = getComponents(this);
        return components;
    }
}
