package platform.server.session;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.MFilterSet;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
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
import platform.server.logics.property.PropertyInterface;

public class PropertyChanges extends AbstractValuesContext<PropertyChanges> {

    private final ImMap<CalcProperty, ModifyChange> changes;

    public PropertyChanges(ImMap<CalcProperty, ModifyChange> changes) {
        this.changes = changes;
    }

    public PropertyChanges replace(ImMap<CalcProperty, ModifyChange> replace) {

        MFilterSet<CalcProperty> mReallyChanged = SetFact.mFilter(replace);
        MExclMap<CalcProperty, ModifyChange> mResult = MapFact.mExclMap();
        for(int i=0;i<replace.size();i++) {
            CalcProperty property = replace.getKey(i);
            ModifyChange modifyChange = replace.getValue(i);

            ModifyChange<PropertyInterface> prevChange = getModify(property);
            if(!BaseUtils.nullEquals(prevChange, modifyChange)) { // чтобы сохранять ссылки
                if (modifyChange != null && (modifyChange.isFinal || !modifyChange.isEmpty())) // в общем-то почти никогда не срабатывает, на всякий случай
                    mResult.exclAdd(property, modifyChange);
                mReallyChanged.keep(property);
            }
        }
        ImSet<CalcProperty> reallyChanged = SetFact.imFilter(mReallyChanged, replace);

        if(reallyChanged.size()>0) {
            mResult.exclAddAll(changes.remove(reallyChanged));
            return new PropertyChanges(mResult.immutable());
        } else // чтобы сохранить ссылку
            return this;
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

    public <P extends PropertyInterface> Expr getChangeExpr(CalcProperty<P> property, ImMap<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
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
