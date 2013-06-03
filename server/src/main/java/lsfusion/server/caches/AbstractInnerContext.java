package lsfusion.server.caches;

import lsfusion.base.*;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.*;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.MapValuesTranslate;

public abstract class AbstractInnerContext<I extends InnerContext<I>> extends AbstractKeysValuesContext<I> implements InnerContext<I> {

    public I translateInner(MapTranslate translate) {
        return aspectTranslate(translate);
    }

    public int hashInner(HashContext hashContext) {
        return aspectHash(hashContext);
    }

    public ImSet<Value> getInnerValues() {
        return aspectGetValues();
    }

    public ImSet<ParamExpr> getInnerKeys() {
        return aspectGetKeys();
    }

    private final AbstractInnerHashContext inherit = new AbstractInnerHashContext() {
        public int hashInner(HashContext hashContext) {
            return AbstractInnerContext.this.hashInner(hashContext);
        }
        public ImSet<ParamExpr> getInnerKeys() {
            return AbstractInnerContext.this.getInnerKeys();
        }
        public ImSet<Value> getInnerValues() {
            return AbstractInnerContext.this.getInnerValues();
        }
        protected boolean isComplex() {
            return AbstractInnerContext.this.isComplex();
        }
    };

    public int hashValues(HashValues hashValues) {
        return inherit.hashValues(hashValues);
    }
    public BaseUtils.HashComponents<ParamExpr> getComponents(HashValues hashValues) {
        return inherit.getComponents(hashValues);
    }

    private BaseUtils.HashComponents<ParamExpr> innerComponents, valuesInnerComponents;
    @ManualLazy
    public BaseUtils.HashComponents<ParamExpr> getInnerComponents(boolean values) {
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
    private static BaseUtils.HashComponents<ParamExpr> translate(BaseUtils.HashComponents<ParamExpr> components, MapTranslate translator) {
        return new BaseUtils.HashComponents<ParamExpr>(translator.translateExprKeys(components.map), components.hash);
    }
    private BaseUtils.HashComponents<ParamExpr> aspectGetInnerComponents(boolean values) {
        I from = getFrom();
        MapTranslate translator = getTranslator();
        if(from!=null && translator!=null && (values || translator.identityValues(from.getInnerValues()))) // объект не ушел
            return translate(from.getInnerComponents(values), translator);

        return calculateInnerComponents(values);
    }
    private BaseUtils.HashComponents<ParamExpr> calculateInnerComponents(boolean values) {
        return getComponents(values ? HashMapValues.create(getValueComponents().map) : HashCodeValues.instance);
    }

    public I translateValues(MapValuesTranslate mapValues) {
        return translateInner(mapValues.mapKeys());
    }

    public I translateRemoveValues(MapValuesTranslate translate) {
        return translateValues(translate);
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
        final HashKeys hashKeys = HashMapKeys.create(getInnerKeys().toMap(keyValueHash));
        return BaseUtils.getComponents(new BaseUtils.HashInterface<Value, GlobalObject>() {
            public ImMap<Value, GlobalObject> getParams() {
                return AbstractValuesContext.getParamClasses(getInnerValues());
            }

            public int hashParams(ImMap<Value, ? extends GlobalObject> map) {
                return hashInner(HashContext.create(hashKeys, HashMapValues.create(map)));
            }
        });
    }

    public boolean twins(TwinImmutableObject o) {
        return mapInner((I) o,false)!=null;
    }

    public int immutableHashCode() {
        return getInnerComponents(false).hash;
    }

    public ImSet<Value> getContextValues() {
        return getInnerValues();
    }
}
