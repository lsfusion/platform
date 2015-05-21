package lsfusion.server.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.SimpleAddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.server.caches.IdentityQuickLazy;
import lsfusion.server.logics.property.CalcProperty;

public class StructChanges extends TwinImmutableObject {

    public final static GetValue<ChangeType, ModifyChange> getType = new GetValue<ChangeType, ModifyChange>() {
        public ChangeType getMapValue(ModifyChange modify) {
            return modify.getChangeType();
        }};

    // не используется
    private final static AddValue<CalcProperty, ChangeType> addValue = new SimpleAddValue<CalcProperty, ChangeType>() {
        public ChangeType addValue(CalcProperty key, ChangeType prevValue, ChangeType newValue) {
            if(prevValue.isFinal() || !newValue.isFinal())
                return prevValue;
            return newValue;
        }

        public AddValue<CalcProperty, ChangeType> reverse() {
            throw new UnsupportedOperationException();
        }

        public boolean reversed() {
            return false;
        }
    };

    private final ImMap<CalcProperty, ChangeType> changes;
    
    private StructChanges(StructChanges changes1, StructChanges changes2) {
        changes = changes1.changes.merge(changes2.changes, addValue);
    }
    public StructChanges add(StructChanges add) {
        if(isEmpty())
            return add;
        if(add.isEmpty())
            return this;
        if(BaseUtils.hashEquals(this, add))
            return this;
        return new StructChanges(this, add);
    }

    public StructChanges remove(CalcProperty property) {
        assert changes.containsKey(property);
        return new StructChanges(changes.remove(property));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }
    public int size() {
        return changes.size();
    }

    public boolean hasChanges(ImSet<CalcProperty> props) {
        for(int i=0,size=props.size();i<size;i++)
            if(changes.get(props.get(i))!= ChangeType.NOUPDATE)
                return true;
        return false;
    }

    public StructChanges(ImMap<CalcProperty, ChangeType> changes) {
        this.changes = changes;
    }

    public StructChanges filterForProperty(CalcProperty<?> prop) {
        return new StructChanges(transformSetOrDropped(prop, changes.filter(prop.getRecDepends())));
    }

    private static ImMap<CalcProperty, ChangeType> transformSetOrDropped(CalcProperty<?> prop, ImMap<CalcProperty, ChangeType> filteredChanges) {
        // assert что recDepends включает SetOrDroppedDepends, внутри вызова есть
        ImMap<CalcProperty, Boolean> setDroppedDepends = prop.getSetOrDroppedDepends();

        if(!setDroppedDepends.keys().intersect(filteredChanges.keys())) // оптимизация
            return filteredChanges;

        ImFilterValueMap<CalcProperty, ChangeType> transformedChanges = filteredChanges.mapFilterValues();
        for(int i=0,size=filteredChanges.size();i<size;i++) {
            CalcProperty property = filteredChanges.getKey(i);
            ChangeType type = filteredChanges.getValue(i);
            Boolean changeSetDropped = type.getSetOrDropped();
            if (changeSetDropped != null) {
                Boolean setDropped = setDroppedDepends.get(property);
                if(setDropped == null || setDropped.equals(changeSetDropped)) // оптимизация
                    type = ChangeType.get(type.isFinal(), null);
            }

            transformedChanges.mapValue(i, type);
        }
        return transformedChanges.immutableValue();
    }

    public ChangeType getUsedChange(CalcProperty property) {
        return changes.get(property);
    }

    public ImSet<CalcProperty> getUsedChanges(ImCol<CalcProperty> col) {
        MSet<CalcProperty> mResult = SetFact.mSet();
        for(CalcProperty<?> property : col)
            mResult.addAll(property.getUsedChanges(this));
        return mResult.immutable();
    }

    public ImSet<CalcProperty> getUsedDataChanges(ImCol<CalcProperty> col) {
        MSet<CalcProperty> mResult = SetFact.mSet();
        for(CalcProperty<?> property : col)
            mResult.addAll(property.getUsedDataChanges(this));
        return mResult.immutable();
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return changes.equals(((StructChanges)o).changes);
    }

    public int immutableHashCode() {
        return changes.hashCode();
    }
}
