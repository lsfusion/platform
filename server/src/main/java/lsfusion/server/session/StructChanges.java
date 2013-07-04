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
import lsfusion.server.logics.property.CalcProperty;

public class StructChanges extends TwinImmutableObject {

    public StructChanges(final PropertyChanges propChanges) {
        changes = propChanges.getProperties().mapValues(new GetValue<ChangeType, CalcProperty>() {
            public ChangeType getMapValue(CalcProperty value) {
                ModifyChange modify = propChanges.getModify(value);
                ChangeType type;
                if (modify.isFinal) {
                    if (modify.isEmpty())
                        type = ChangeType.NOUPDATE;
                    else
                        type = ChangeType.FINAL;
                } else {
                    type = ChangeType.NOTFINAL;
                    assert !modify.isEmpty();
                }
                return type;
            }
        });
    }

    private final static AddValue<CalcProperty, ChangeType> addValue = new SimpleAddValue<CalcProperty, ChangeType>() {
        public ChangeType addValue(CalcProperty key, ChangeType prevValue, ChangeType newValue) {
            if(prevValue.equals(ChangeType.FINAL) || prevValue.equals(ChangeType.NOUPDATE) || newValue.equals(ChangeType.NOTFINAL))
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

    private StructChanges(ImMap<CalcProperty, ChangeType> changes) {
        this.changes = changes;
    }

    public StructChanges filter(ImSet<CalcProperty> props) {
        return new StructChanges(changes.filter(props));
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

    public boolean twins(TwinImmutableObject o) {
        return changes.equals(((StructChanges)o).changes);
    }

    public int immutableHashCode() {
        return changes.hashCode();
    }
}
