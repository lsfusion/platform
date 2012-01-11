package platform.server.caches;

import platform.base.*;
import platform.server.caches.hash.*;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractInnerContext<I extends InnerContext<I>> extends AbstractKeysValuesContext<I> implements InnerContext<I>, TwinImmutableInterface {

    public I translateInner(MapTranslate translate) {
        return aspectTranslate(translate);
    }

    public int hashInner(HashContext hashContext) {
        return aspectHash(hashContext);
    }

    public QuickSet<Value> getInnerValues() {
        return aspectGetValues();
    }

    public QuickSet<KeyExpr> getInnerKeys() {
        return aspectGetKeys();
    }

    private final AbstractInnerHashContext inherit = new AbstractInnerHashContext() {
        public int hashInner(HashContext hashContext) {
            return AbstractInnerContext.this.hashInner(hashContext);
        }
        public QuickSet<KeyExpr> getInnerKeys() {
            return AbstractInnerContext.this.getInnerKeys();
        }
    };

    public int hashValues(HashValues hashValues) {
        return inherit.hashValues(hashValues);
    }
    public BaseUtils.HashComponents<KeyExpr> getComponents(HashValues hashValues) {
        return inherit.getComponents(hashValues);
    }

    private BaseUtils.HashComponents<KeyExpr> innerComponents, valuesInnerComponents;
    @ManualLazy
    public BaseUtils.HashComponents<KeyExpr> getInnerComponents(boolean values) {
        if(values) {
            if(valuesInnerComponents == null)
                valuesInnerComponents = aspectGetInnerComponents(values);
            return valuesInnerComponents;
        } else {
            if(innerComponents == null)
                innerComponents = aspectGetInnerComponents(values);
            return innerComponents;
        }
    }
    private static BaseUtils.HashComponents<KeyExpr> translate(BaseUtils.HashComponents<KeyExpr> components, MapTranslate translator) {
        return new BaseUtils.HashComponents<KeyExpr>(translator.translateMapKeys(components.map), components.hash);
    }
    private BaseUtils.HashComponents<KeyExpr> aspectGetInnerComponents(boolean values) {
        I from = getFrom();
        MapTranslate translator = getTranslator();
        if(from!=null && translator!=null && (values || translator.identityValues(from.getInnerValues()))) // объект не ушел
            return translate(from.getInnerComponents(values), translator);

        return calculateInnerComponents(values);
    }
    private BaseUtils.HashComponents<KeyExpr> calculateInnerComponents(boolean values) {
        return getComponents(values ? new HashMapValues(getValuesMap()) : HashCodeValues.instance);
    }

    public int hashInner(boolean values) {
        return getInnerComponents(values).hash;
    }
    public QuickMap<KeyExpr, GlobalObject> getInnerMap(boolean values) {
        return getInnerComponents(values).map;
    }

    public static Map<Value, Value> getBigValues(QuickSet<Value> values) {
        QuickSet<Value> usedValues = new QuickSet<Value>(values);

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
        return getBigValues(getInnerValues());
    }

    public I translateValues(MapValuesTranslate mapValues) {
        return translateInner(mapValues.mapKeys());
    }

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

    private BaseUtils.HashComponents<Value> valueComponents;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getValueComponents() {
        if(valueComponents==null)
            valueComponents = aspectGetValueComponents();
        return valueComponents;
    }
    public static BaseUtils.HashComponents<Value> translate(MapTranslate translator, BaseUtils.HashComponents<Value> components) {
        return new BaseUtils.HashComponents<Value>(translator.translateValuesMapKeys(components.map), components.hash);
    }
    private BaseUtils.HashComponents<Value> aspectGetValueComponents() {
        I from = getFrom();
        MapTranslate translator = getTranslator();
        if(from!=null && translator!=null) // объект не ушел
            return translate(translator, from.getValueComponents());

        return calculateValueComponents();
    }
    private final static GlobalInteger keyValueHash = new GlobalInteger(5);
    public BaseUtils.HashComponents<Value> calculateValueComponents() {
        final HashMapKeys hashKeys = new HashMapKeys(getInnerKeys().toQuickMap(keyValueHash));
        return BaseUtils.getComponents(new BaseUtils.HashInterface<Value, GlobalObject>() {
            public QuickMap<Value, GlobalObject> getParams() {
                return AbstractValuesContext.getParamClasses(getInnerValues());
            }

            public int hashParams(QuickMap<Value, ? extends GlobalObject> map) {
                return hashInner(new HashContext(hashKeys, map.size>0?new HashMapValues(map):HashCodeValues.instance));
            }
        });
    }
    public int hashValues() {
        return AbstractValuesContext.hash(this);
    }
    public QuickMap<Value, GlobalObject> getValuesMap() {
        return AbstractValuesContext.getMap(this);
    }

    public boolean twins(TwinImmutableInterface o) {
        return mapInner((I) o,false)!=null;
    }

    public int immutableHashCode() {
        return hashInner(false);
    }

    public QuickSet<Value> getContextValues() {
        return getInnerValues();
    }
}
