package platform.server.caches;

import platform.base.*;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashMapValues;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

import java.util.Collection;
import java.util.Map;

public abstract class AbstractValuesContext<U extends ValuesContext<U>> extends AbstractTranslateContext<U, MapValuesTranslate, HashValues> implements ValuesContext<U> {

    protected U aspectContextTranslate(MapValuesTranslate translator) {
        QuickSet<Value> values = aspectGetValues();

        QuickSet<Value> transValues = translator.translateValues(values);
        if(transValues.equals(values))
            return (U) this;
        else {
            AbstractValuesContext result = (AbstractValuesContext) translate(translator);
            result.values = transValues;
            return (U) result;
        }
    }

    protected HashValues aspectContextHash(HashValues hash) {
        return hash.filterValues(aspectGetValues());
    }

    public U translateValues(MapValuesTranslate translate) {
        return aspectTranslate(translate);
    }

    public int hashValues(HashValues hashValues) {
        return aspectHash(hashValues);
    }

    public QuickSet<Value> getContextValues() {
        return aspectGetValues();
    }

    public int immutableHashCode() {
        return hashValues(HashCodeValues.instance);
    }

    public Map<Value, Value> getBigValues() {
        return AbstractInnerContext.getBigValues(getContextValues());
    }

    public static QuickMap<Value, GlobalObject> getParamClasses(QuickSet<Value> values) {
        QuickMap<Value, GlobalObject> result = new SimpleMap<Value, GlobalObject>();
        for(Value value : values)
            result.add(value, value.getValueClass());
        return result;
    }

    // множественные наследование
    public static BaseUtils.HashComponents<Value> getComponents(final ValuesContext<?> valuesContext) {
        return BaseUtils.getComponents(new BaseUtils.HashInterface<Value, GlobalObject>() {
            public QuickMap<Value, GlobalObject> getParams() {
                return getParamClasses(valuesContext.getContextValues());
            }

            public int hashParams(QuickMap<Value, ? extends GlobalObject> map) {
                return valuesContext.hashValues(map.size>0?new HashMapValues(map): HashCodeValues.instance);
            }
        });
    }
    public static int hash(ValuesContext<?> valuesContext) {
        return valuesContext.getValueComponents().hash;
    }
    public static QuickMap<Value, GlobalObject> getMap(ValuesContext<?> valuesContext) {
        return valuesContext.getValueComponents().map;
    }

    private BaseUtils.HashComponents<Value> valueComponents;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getValueComponents() {
        if(valueComponents==null)
            valueComponents = aspectGetValueComponents();
        return valueComponents;
    }
    private static BaseUtils.HashComponents<Value> translate(BaseUtils.HashComponents<Value> components, MapValuesTranslate translator) {
        return new BaseUtils.HashComponents<Value>(translator.translateValuesMapKeys(components.map), components.hash);
    }
    private BaseUtils.HashComponents<Value> aspectGetValueComponents() {
        U from = getFrom();
        MapValuesTranslate translator = getTranslator();
        if(from!=null && translator!=null) // объект не ушел
            return translate(from.getValueComponents(), translator);

        return calculateValueComponents();
    }
    public BaseUtils.HashComponents<Value> calculateValueComponents() {
        return getComponents(this);
    }
    public int hashValues() {
        return hash(this);
    }
    public QuickMap<Value, GlobalObject> getValuesMap() {
        return getMap(this);
    }

    public static int hashValues(Collection<? extends Value> set, HashValues hashValues) {
        int hash = 0;
        for(Value element : set)
            hash += hashValues.hash(element);
        return hash;
    }

    protected HashValues reverseTranslate(HashValues hash, MapValuesTranslate translator) {
        return hash.reverseTranslate(translator, aspectGetValues());
    }
}
