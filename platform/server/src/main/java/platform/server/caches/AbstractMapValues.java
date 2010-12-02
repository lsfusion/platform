package platform.server.caches;

import platform.base.ImmutableObject;
import platform.base.BaseUtils;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashMapKeys;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashMapValues;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.classes.*;

import java.util.*;

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

    public static SortedMap<ConcreteClass, Set<ValueExpr>> getSortedParams(Set<ValueExpr> values) {
        return BaseUtils.groupSortedSet(new BaseUtils.Group<ConcreteClass, ValueExpr>() {
            public ConcreteClass group(ValueExpr key) {
                return key.objectClass;
            }
        }, values, ConcreteClass.comparator);
    }

    // множественные наследование
    public static BaseUtils.HashComponents<ValueExpr> getComponents(final MapValues<?> mapValues) {
        return BaseUtils.getComponents(new BaseUtils.HashInterface<ValueExpr, ConcreteClass>() {
            public SortedMap<ConcreteClass,Set<ValueExpr>> getParams() {
                return getSortedParams(mapValues.getValues());
            }

            public int hashParams(Map<ValueExpr, Integer> map) {
                return mapValues.hashValues(map.size()>0?new HashMapValues(map): HashCodeValues.instance);
            }
        });
    }

    private BaseUtils.HashComponents<ValueExpr> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<ValueExpr> getComponents() {
        if(components==null)
            components = getComponents(this);
        return components;
    }
}
