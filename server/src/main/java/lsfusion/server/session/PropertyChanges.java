package lsfusion.server.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.SFunctionSet;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MFilterSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.server.caches.AbstractValuesContext;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.MapValuesIterable;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapValuesTranslate;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;

public class PropertyChanges extends AbstractValuesContext<PropertyChanges> {

    private final ImMap<CalcProperty, ModifyChange> changes;

    public PropertyChanges(ImMap<CalcProperty, ModifyChange> changes) {
        this.changes = changes;
    }

    private final static SFunctionSet<ModifyChange> emptyChanges = new SFunctionSet<ModifyChange>() {
        public boolean contains(ModifyChange element) {
        return element==null || (!element.isFinal && element.isEmpty());
        }
    };
    public PropertyChanges replace(ImMap<CalcProperty, ModifyChange> replace) {
        ImSet<CalcProperty> keys = replace.filterFnValues(emptyChanges).keys();
        return new PropertyChanges(changes.remove(keys).merge(replace.remove(keys), MapFact.<CalcProperty, ModifyChange>overridePrevRef())); // override с оставлением ссылки
    }

    @IdentityLazy
    public PropertyChanges filter(ImSet<? extends CalcProperty> properties) {
        ImFilterValueMap<CalcProperty, ModifyChange> mvResult = ((ImSet<CalcProperty>)properties).mapFilterValues();
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

    public <T extends PropertyInterface> PropertyChanges(CalcProperty<T> property, PropertyChange<T> change) {
        this(property, new ModifyChange<T>(change, true));
    }

    public <T extends PropertyInterface> PropertyChanges(CalcProperty<T> property, ModifyChange<T> change) {
        changes = MapFact.<CalcProperty, ModifyChange>singleton(property, change);
    }

    public PropertyChanges(ImMap<? extends CalcProperty, ? extends PropertyChange> mapChanges, final boolean isFinal) {
        changes = ((ImMap<CalcProperty, PropertyChange>)mapChanges).mapValues(new GetValue<ModifyChange, PropertyChange>() {
                            public ModifyChange getMapValue(PropertyChange value) {
                                return new ModifyChange(value, isFinal);
                            }});
    }

    protected PropertyChanges(PropertyChanges changes1, PropertyChanges changes2) {
        changes = changes1.changes.merge(changes2.changes, ModifyChange.<CalcProperty>addValue());
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

    public PropertyChanges remove(CalcProperty property) {
        assert changes.containsKey(property);
        return new PropertyChanges(changes.remove(property));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public <P extends PropertyInterface> ModifyChange<P> getModify(CalcProperty<P> property) {
        return (ModifyChange<P>)changes.getObject(property);
    }

    private StructChanges struct;
    @ManualLazy
    public StructChanges getStruct() {
        if(struct==null)
            struct = new StructChanges(this);
        return struct;
    }

    protected int hash(HashValues hash) {
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

    public boolean twins(TwinImmutableObject o) {
        return changes.equals(((PropertyChanges)o).changes);
    }

    public ImSet<CalcProperty> getProperties() {
        return changes.keys();
    }
}
