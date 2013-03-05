package platform.server.session;

import platform.base.BaseUtils;
import platform.base.TwinImmutableObject;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.AddValue;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.SimpleAddValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.logics.property.CalcProperty;

public class StructChanges extends TwinImmutableObject {

    public StructChanges(final PropertyChanges propChanges) {
        changes = propChanges.getProperties().mapValues(new GetValue<Type, CalcProperty>() {
            public Type getMapValue(CalcProperty value) {
                ModifyChange modify = propChanges.getModify(value);
                Type type;
                if (modify.isFinal) {
                    if (modify.isEmpty())
                        type = Type.NOUPDATE;
                    else
                        type = Type.FINAL;
                } else {
                    type = Type.NOTFINAL;
                    assert !modify.isEmpty();
                }
                return type;
            }
        });
    }

    private enum Type {
        FINAL, NOUPDATE, NOTFINAL;

        public boolean isFinal() {
            return this==FINAL || this==NOUPDATE;
        }
    }

    private final static AddValue<CalcProperty, Type> addValue = new SimpleAddValue<CalcProperty, Type>() {
        public Type addValue(CalcProperty key, Type prevValue, Type newValue) {
            if(prevValue.equals(Type.FINAL) || prevValue.equals(Type.NOUPDATE) || newValue.equals(Type.NOTFINAL))
                return prevValue;
            return newValue;
        }

        public boolean symmetric() {
            return false;
        }
    };

    private final ImMap<CalcProperty, Type> changes;
    
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

    public StructChanges(ImSet<? extends CalcProperty> props) {
        changes = MapFact.toMap(props, Type.NOTFINAL);
    }

    public StructChanges(CalcProperty property) {
        changes = MapFact.singleton(property, Type.NOTFINAL);
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }
    public int size() {
        return changes.size();
    }

    public boolean hasChanges(ImSet<CalcProperty> props) {
        for(int i=0,size=props.size();i<size;i++)
            if(changes.get(props.get(i))!=Type.NOUPDATE)
                return true;
        return false;
    }

    private StructChanges(ImMap<CalcProperty, Type> changes) {
        this.changes = changes;
    }

    public StructChanges filter(ImSet<CalcProperty> props) {
        return new StructChanges(changes.filter(props));
    }

    public ImSet<CalcProperty> getUsedChanges(CalcProperty property, boolean cascade) {
        Type propChange = changes.get(property);
        return SetFact.add(propChange == null ? SetFact.<CalcProperty>EMPTY() : SetFact.<CalcProperty>singleton(property),
                propChange != null && propChange.isFinal() ? SetFact.<CalcProperty>EMPTY() : property.getUsedEventChange(this, cascade));
    }

    public ImSet<CalcProperty> getUsedChanges(ImCol<CalcProperty> col) {
        return getUsedChanges(col, false);
    }

    public ImSet<CalcProperty> getUsedChanges(ImCol<CalcProperty> col, boolean cascade) {
        MSet<CalcProperty> mResult = SetFact.mSet();
        for(CalcProperty<?> property : col)
            mResult.addAll(property.getUsedChanges(this, cascade));
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
