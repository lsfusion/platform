package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.base.GlobalObject;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashMapValues;
import platform.server.data.Value;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public abstract class AbstractMapValues<U extends AbstractMapValues<U>> extends ImmutableObject implements MapValues<U>  {

    boolean hashCoded = false;
    int hashCode;
    @Override
    public int hashCode() {
        if(!hashCoded) {
            hashCode = hashValues(HashCodeValues.instance);
            hashCoded = true;
        }
        return hashCode;
    }

    public static SortedMap<GlobalObject, Set<Value>> getSortedParams(Set<Value> values) {
        return BaseUtils.groupSortedSet(new BaseUtils.Group<GlobalObject, Value>() {
            public GlobalObject group(Value key) {
                return key.getValueClass();
            }
        }, values, GlobalObject.comparator);
    }

    // множественные наследование
    public static BaseUtils.HashComponents<Value> getComponents(final MapValues<?> mapValues) {
        return BaseUtils.getComponents(new BaseUtils.HashInterface<Value, GlobalObject>() {
            public SortedMap<GlobalObject,Set<Value>> getParams() {
                return getSortedParams(mapValues.getValues());
            }

            public int hashParams(Map<Value, Integer> map) {
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
