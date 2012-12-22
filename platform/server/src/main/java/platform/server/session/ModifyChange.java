package platform.server.session;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.AddValue;
import platform.base.col.interfaces.mutable.SimpleAddValue;
import platform.server.caches.AbstractValuesContext;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.property.PropertyInterface;

public class ModifyChange<P extends PropertyInterface> extends AbstractValuesContext<ModifyChange<P>> {
    public final PropertyChange<P> change;
    public final boolean isFinal;

    public ModifyChange(PropertyChange<P> change, boolean isFinal) {
        assert change!=null;
        this.change = change;
        this.isFinal = isFinal;
    }

    public int hash(HashValues hashValues) {
        return change.hashValues(hashValues) * 31 + (isFinal?1:0);
    }

    public ImSet<Value> getValues() {
        return change.getInnerValues();
    }

    @Override
    public ModifyChange<P> translate(MapValuesTranslate mapValues) {
        return new ModifyChange<P>(change.translateValues(mapValues), isFinal);
    }

    @Override
    public boolean twins(TwinImmutableObject o) {
        return change.equals(((ModifyChange)o).change) && isFinal == ((ModifyChange)o).isFinal;
    }

    public ModifyChange<P> add(ModifyChange<P> modify) {
        if(isFinal)
            return this;
        return new ModifyChange<P>(change.add(modify.change), modify.isFinal);
    }

    public static <P extends PropertyInterface> ModifyChange<P> addNull(ModifyChange<P> change1, ModifyChange<P> change2) {
        if(change1==null)
            return change2;
        if(change2==null)
            return change1;
        return change1.add(change2);
    }

    public boolean isEmpty() {
        return change.where.isFalse();
    }

    public final static AddValue<Object, ModifyChange<PropertyInterface>> addValue = new SimpleAddValue<Object, ModifyChange<PropertyInterface>>() {
        public ModifyChange<PropertyInterface> addValue(Object key, ModifyChange<PropertyInterface> prevValue, ModifyChange<PropertyInterface> newValue) {
            return prevValue.add(newValue);
        }

        public boolean symmetric() {
            return false;
        }
    };
    public static <M> AddValue<M, ModifyChange> addValue() {
        return BaseUtils.immutableCast(addValue);
    }

}
