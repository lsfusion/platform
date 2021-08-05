package lsfusion.server.logics.action.session.change;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.data.caches.AbstractValuesContext;
import lsfusion.server.data.caches.MapValuesIterable;
import lsfusion.server.data.caches.hash.HashValues;
import lsfusion.server.data.table.SessionTable;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.value.Value;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class PropertyChanges extends AbstractValuesContext<PropertyChanges> {

    private final ImMap<Property, ModifyChange> changes;

    @Override
    public String toString() {
        return changes.toString();
    }

    public PropertyChanges(ImMap<Property, ModifyChange> changes) {
        this.changes = changes;
    }

    private final static SFunctionSet<ModifyChange> emptyChanges = element -> element==null || element.isNotFinalEmpty();
    public PropertyChanges replace(ImMap<Property, ModifyChange> replace) {
        ImSet<Property> remove = replace.filterFnValues(emptyChanges).keys();
        ImMap<Property, ModifyChange> add = replace.remove(remove);
        PropertyChanges propertyChanges = new PropertyChanges(changes.remove(remove).merge(add, MapFact.overridePrevRef()));
        if(propertyChanges.struct != null) // optimization
            struct = propertyChanges.struct.replace(remove, add.mapValues(StructChanges.getType));;
        return propertyChanges; // override с оставлением ссылки
    }

    @IdentityLazy
    public PropertyChanges filter(ImSet<? extends Property> properties) {
        ImFilterValueMap<Property, ModifyChange> mvResult = ((ImSet<Property>)properties).mapFilterValues();
        for(int i=0,size=properties.size();i<size;i++) {
            ModifyChange<PropertyInterface> change = getModify(properties.get(i));
            if(change!=null)
                mvResult.mapValue(i, change);
        }
        return new PropertyChanges(mvResult.immutableValue());
    }

    public PropertyChanges() {
        changes = MapFact.EMPTY();
    }
    public final static PropertyChanges EMPTY = new PropertyChanges();

    public static PropertyChanges PREVEXPR(CalcType type, PropertyChanges propChanges) {
        if(!type.isExpr()) // no hints we don't need preread
            return PropertyChanges.EMPTY;

        return propChanges.getPrev();
    }

    @IdentityInstanceLazy
    public PropertyChanges getPrev() { // returns only preread without used changes
        ImFilterValueMap<Property, ModifyChange> mvResult = changes.mapFilterValues();
        for(int i=0,size=changes.size();i<size;i++) {
            ModifyChange<PropertyInterface> prevValue = changes.getValue(i).getPrev(changes.getKey(i));
            if(prevValue!=null)
                mvResult.mapValue(i, prevValue);
        }
        ImMap<Property, ModifyChange> changes = mvResult.immutableValue();
        if(changes.isEmpty()) // optimization of the most common case
            return PropertyChanges.EMPTY;

        return new PropertyChanges(changes);
    }

    public <T extends PropertyInterface> PropertyChanges(Property<T> property, PropertyChange<T> change) {
        this(property, new ModifyChange<>(change, true));
    }

    public <T extends PropertyInterface> PropertyChanges(Property<T> property, ModifyChange<T> change) {
        changes = MapFact.singleton(property, change);
    }

    public PropertyChanges(ImMap<? extends Property, ? extends PropertyChange> mapChanges, final boolean isFinal) {
        assert isFinal;
        changes = ((ImMap<Property, PropertyChange>)mapChanges).mapValues(value -> new ModifyChange(value, isFinal));
    }

    protected PropertyChanges(PropertyChanges changes1, PropertyChanges changes2) {
        changes = changes1.changes.merge(changes2.changes, ModifyChange.addValue());
    }
    public PropertyChanges add(PropertyChanges add) {
        if(isEmpty())
            return add;
        if(add.isEmpty())
            return this;
        if(BaseUtils.hashEquals(this, add))
            return this;
        return new PropertyChanges(this, add);
    }

    public PropertyChanges remove(Property property) {
        assert changes.containsKey(property);
        return new PropertyChanges(changes.remove(property));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public <P extends PropertyInterface> ModifyChange<P> getModify(Property<P> property) {
        return (ModifyChange<P>)changes.get(property);
    }

    private StructChanges struct;
    @ManualLazy
    public StructChanges getStruct() {
        if(struct==null)
            struct = new StructChanges(changes.mapValues(StructChanges.getType));
        return struct;
    }

    public int hash(HashValues hash) {
        return MapValuesIterable.hash(changes, hash);
    }

    public ImSet<Value> getValues() {
        return MapValuesIterable.getContextValues(changes);
    }

    private PropertyChanges(PropertyChanges propChanges, MapValuesTranslate mapValues) {
        changes = mapValues.translateValues(propChanges.changes);
    }
    public PropertyChanges translate(MapValuesTranslate mapValues) {
        return new PropertyChanges(this, mapValues);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return changes.equals(((PropertyChanges)o).changes);
    }

    public ImSet<Property> getProperties() {
        return changes.keys();
    }

    public String exToString() {
        String result = "";
        for(int i=0,size=changes.size();i<size;i++) {
            Property key = changes.getKey(i);
            ModifyChange change = changes.getValue(i);
            result += "PROP : " + key + ", CHANGE : " + change + ", HASH : " + change.getValueComponents().hash;

            ImSet<Value> values = change.getValues();
            for(Value value : values) {
                if(value instanceof SessionTable)
                    result += value + " " + ((SessionTable)value).getValueClass();
            }
            result += '\n';
        }
        return result;
    }
}
