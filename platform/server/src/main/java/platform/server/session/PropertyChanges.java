package platform.server.session;

import platform.base.*;
import platform.server.caches.AbstractValuesContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.Collection;
import java.util.Map;

public class PropertyChanges extends AbstractValuesContext<PropertyChanges> {

    protected static class Changes extends QuickMap<Property, ModifyChange> {

        private Changes() {
        }

        private Changes(QuickMap<? extends Property, ? extends ModifyChange> set) {
            super(set);
        }

        protected ModifyChange<PropertyInterface> addValue(Property key, ModifyChange prevValue, ModifyChange newValue) {
            return prevValue.add(newValue);
        }

        public Changes translate(MapValuesTranslate mapValues) {
            Changes result = new Changes();
            for(int i=0;i<size;i++)
                result.add(getKey(i),getValue(i).translate(mapValues));
            return result;
        }

        protected boolean containsAll(ModifyChange who, ModifyChange what) {
            throw new RuntimeException("not supported");
        }
    }
    private final Changes changes;

    private <P extends PropertyInterface> void addChange(Property<P> property, PropertyChange<P> propertyChange) {
        addChange(property, new ModifyChange<P>(propertyChange, true));
    }

    private <P extends PropertyInterface> void addChange(Property<P> property, ModifyChange<P> modifyChange) {
        if(modifyChange.isFinal || !modifyChange.isEmpty()) // в общем-то почти никогда не срабатывает, на всякий случай
            changes.add(property, modifyChange);
    }

    private void addChanges(PropertyChanges propChanges, QuickSet<Property> skip) {
        changes.addAll(propChanges.changes, skip);
    }

    public PropertyChanges replace(Map<Property, ModifyChange> replace) {
        QuickSet<Property> reallyChanged = new QuickSet<Property>();
        PropertyChanges result = new PropertyChanges();
        for(Map.Entry<Property, ModifyChange> change : replace.entrySet()) {
            Property property = change.getKey();
            ModifyChange modifyChange = change.getValue();

            ModifyChange<PropertyInterface> prevChange = getModify(property);
            if(!BaseUtils.nullEquals(prevChange, modifyChange)) { // чтобы сохранять ссылки
                if(modifyChange!=null)
                    result.addChange(property, modifyChange);
                reallyChanged.add(property);
            }
        }
        if(reallyChanged.size>0) {
            result.addChanges(this, reallyChanged);
            return result;
        } else // чтобы сохранить ссылку
            return this;
    }

    public PropertyChanges filter(Collection<? extends Property> properties) {
        PropertyChanges result = new PropertyChanges();
        for(Property property : properties) {
            ModifyChange<PropertyInterface> change = getModify(property);
            if(change!=null)
                result.changes.add(property, change);
        }
        return result;
    }

    public PropertyChanges filter(QuickSet<? extends Property> properties) {
        PropertyChanges result = new PropertyChanges();
        for(int i=0;i<properties.size;i++) {
            Property property = properties.get(i);
            ModifyChange<PropertyInterface> change = getModify(property);
            if(change!=null)
                result.changes.add(property, change);
        }
        return result;
    }

    public PropertyChanges() {
        changes = new Changes();
    }
    public final static PropertyChanges EMPTY = new PropertyChanges();

    public <T extends PropertyInterface> PropertyChanges(Property<T> property, PropertyChange<T> change) {
        this(property, new ModifyChange<T>(change, true));
    }

    public <T extends PropertyInterface> PropertyChanges(Property<T> property, ModifyChange<T> change) {
        this();
        addChange(property, change);
    }

    public PropertyChanges(QuickMap<? extends Property, ? extends PropertyChange> mapChanges) {
        this();
        for(int i=0;i<mapChanges.size;i++)
            addChange(mapChanges.getKey(i), mapChanges.getValue(i));
    }

    public PropertyChanges(QuickMap<? extends Property, ? extends PropertyChange> mapChanges, boolean isFinal) {
        this();
        for(int i=0;i<mapChanges.size;i++)
            addChange(mapChanges.getKey(i), new ModifyChange(mapChanges.getValue(i), isFinal));
    }

    protected PropertyChanges(PropertyChanges changes1, PropertyChanges changes2) {
        changes = new Changes(changes1.changes);
        changes.addAll(changes2.changes);
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

    private boolean isEmpty() {
        return changes.isEmpty();
    }

    public <P extends PropertyInterface> ModifyChange<P> getModify(Property<P> property) {
        return (ModifyChange<P>)changes.getObject(property);
    }

    public <P extends PropertyInterface> PropertyChange<P> getChange(Property<P> property) {
        ModifyChange<P> propChange = getModify(property);
        return PropertyChange.addNull(propChange==null? null : propChange.change, propChange != null && propChange.isFinal ? null : property.getDerivedChange(this));
    }

    public <P extends PropertyInterface> Expr getChangeExpr(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        PropertyChange<P> propChange = getChange(property);
        if(propChange!=null)
            return propChange.getExpr(joinImplement, changedWhere);
        else
            return null;
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

    public QuickSet<Value> getValues() {
        return MapValuesIterable.getContextValues(changes);
    }

    private PropertyChanges(PropertyChanges propChanges, MapValuesTranslate mapValues) {
        changes = propChanges.changes.translate(mapValues);
    }
    public PropertyChanges translate(MapValuesTranslate mapValues) {
        return new PropertyChanges(this, mapValues);
    }

    public boolean twins(TwinImmutableInterface o) {
        return changes.equals(((PropertyChanges)o).changes);
    }

    public Collection<Property> getProperties() {
        return changes.keys();
    }
}
