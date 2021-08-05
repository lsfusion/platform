package lsfusion.server.logics.action.session.change;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.SimpleAddValue;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.AbstractValuesContext;
import lsfusion.server.data.caches.hash.HashValues;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.value.Value;
import lsfusion.server.logics.action.data.PrereadRows;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class ModifyChange<P extends PropertyInterface> extends AbstractValuesContext<ModifyChange<P>> {
    public final PropertyChange<P> change;
    public final boolean isFinal;
    public final PrereadRows<P> preread;

    public ModifyChange(PropertyChange<P> change, boolean isFinal) {
        this(change, PrereadRows.EMPTY(), isFinal);
    }

    public ModifyChange(PropertyChange<P> change, PrereadRows<P> preread, boolean isFinal) {
        assert change!=null;
        this.change = change;
        this.isFinal = isFinal;
        this.preread = preread;
    }

    public int hash(HashValues hashValues) {
        return 31 * (change.hashValues(hashValues) * 31 + preread.hash(hashValues)) + (isFinal?1:0);
    }

    public ImSet<Value> getValues() {
        return change.getInnerValues().merge(preread.getContextValues());
    }

    @Override
    public ModifyChange<P> translate(MapValuesTranslate mapValues) {
        return new ModifyChange<>(change.translateValues(mapValues), preread.translateValues(mapValues), isFinal);
    }

    @Override
    public boolean calcTwins(TwinImmutableObject o) {
        return change.equals(((ModifyChange)o).change) && isFinal == ((ModifyChange)o).isFinal && preread.equals(((ModifyChange)o).preread);
    }

    public ModifyChange<P> add(ModifyChange<P> modify) {
        if(isFinal)
            return this;
        return new ModifyChange<>(change.add(modify.change), preread.add(modify.preread), modify.isFinal);
    }

    public final static AddValue<Object, ModifyChange<PropertyInterface>> addValue = new SimpleAddValue<Object, ModifyChange<PropertyInterface>>() {
        public ModifyChange<PropertyInterface> addValue(Object key, ModifyChange<PropertyInterface> prevValue, ModifyChange<PropertyInterface> newValue) {
            return prevValue.add(newValue);
        }

        public boolean reversed() {
            return false;
        }

        public AddValue<Object, ModifyChange<PropertyInterface>> reverse() {
            throw new UnsupportedOperationException();
        }
    };
    public static <M> AddValue<M, ModifyChange> addValue() {
        return BaseUtils.immutableCast(addValue);
    }

    @Override
    public String toString() {
        return change + ", f:" + isFinal + ", p:" + preread;
    }

    public boolean isNotFinalEmpty() { // fake change, optimization
        return !isFinal && change.where.isFalse() && preread.isEmpty();
    }
    public ChangeType getChangeType() {
        boolean hasChanges = !change.where.isFalse();
        return ChangeType.get(isFinal, hasChanges && preread.isEmpty() ? change.getSetOrDropped() : null, hasChanges, preread.hasPrev());
    }

    public ModifyChange<P> getPrev(Property<P> property) { // return null if isEmpty
        boolean hasPrev = preread.hasPrev();
        assert hasPrev == !preread.getPrev().isEmpty();
        if(!hasPrev)
            return null;
        return new ModifyChange<>(property.getNoChange(), preread.getPrev(), false); // forcing isFinal false
    }
}
