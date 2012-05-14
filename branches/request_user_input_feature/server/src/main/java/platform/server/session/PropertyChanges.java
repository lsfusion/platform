package platform.server.session;

import platform.base.*;
import platform.server.caches.AbstractValuesContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;

import java.util.Collection;
import java.util.Map;

public class PropertyChanges extends AbstractValuesContext<PropertyChanges> {

    protected static class Changes extends QuickMap<CalcProperty, ModifyChange> {

        private Changes() {
        }

        private Changes(QuickMap<? extends CalcProperty, ? extends ModifyChange> set) {
            super(set);
        }

        protected ModifyChange<PropertyInterface> addValue(CalcProperty key, ModifyChange prevValue, ModifyChange newValue) {
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

    private <P extends PropertyInterface> void addChange(CalcProperty<P> property, PropertyChange<P> propertyChange) {
        addChange(property, new ModifyChange<P>(propertyChange, true));
    }

    private <P extends PropertyInterface> void addChange(CalcProperty<P> property, ModifyChange<P> modifyChange) {
        if(modifyChange.isFinal || !modifyChange.isEmpty()) // в общем-то почти никогда не срабатывает, на всякий случай
            changes.add(property, modifyChange);
    }

    private void addChanges(PropertyChanges propChanges, QuickSet<CalcProperty> skip) {
        changes.addAll(propChanges.changes, skip);
    }

    public PropertyChanges replace(Map<CalcProperty, ModifyChange> replace) {
        QuickSet<CalcProperty> reallyChanged = new QuickSet<CalcProperty>();
        PropertyChanges result = new PropertyChanges();
        for(Map.Entry<CalcProperty, ModifyChange> change : replace.entrySet()) {
            CalcProperty property = change.getKey();
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

    @IdentityLazy
    public PropertyChanges filter(Collection<? extends CalcProperty> properties) {
        PropertyChanges result = new PropertyChanges();
        for(CalcProperty property : properties) {
            ModifyChange<PropertyInterface> change = getModify(property);
            if(change!=null)
                result.changes.add(property, change);
        }
        return result;
    }

    @IdentityLazy
    public PropertyChanges filter(QuickSet<? extends CalcProperty> properties) {
        PropertyChanges result = new PropertyChanges();
        for(int i=0;i<properties.size;i++) {
            CalcProperty property = properties.get(i);
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

    public <T extends PropertyInterface> PropertyChanges(CalcProperty<T> property, PropertyChange<T> change) {
        this(property, new ModifyChange<T>(change, true));
    }

    public <T extends PropertyInterface> PropertyChanges(CalcProperty<T> property, ModifyChange<T> change) {
        this();
        addChange(property, change);
    }

    public PropertyChanges(QuickMap<? extends CalcProperty, ? extends PropertyChange> mapChanges) {
        this();
        for(int i=0;i<mapChanges.size;i++)
            addChange(mapChanges.getKey(i), mapChanges.getValue(i));
    }

    public PropertyChanges(QuickMap<? extends CalcProperty, ? extends PropertyChange> mapChanges, boolean isFinal) {
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

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public <P extends PropertyInterface> ModifyChange<P> getModify(CalcProperty<P> property) {
        return (ModifyChange<P>)changes.getObject(property);
    }

    public <P extends PropertyInterface> PropertyChange<P> getChange(CalcProperty<P> property) {
        ModifyChange<P> propChange = getModify(property);
        return PropertyChange.addNull(propChange == null ? null : propChange.change, !(property instanceof DataProperty) ||
                (propChange != null && propChange.isFinal) ? null : (PropertyChange<P>) ((DataProperty)property).getEventChange(this));
    }

    public <P extends PropertyInterface> Expr getChangeExpr(CalcProperty<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
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

    public Collection<CalcProperty> getProperties() {
        return changes.keys();
    }
}
