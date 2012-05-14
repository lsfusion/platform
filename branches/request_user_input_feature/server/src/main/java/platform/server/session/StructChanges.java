package platform.server.session;

import platform.base.*;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.CalcProperty;

import java.util.Collection;

public class StructChanges extends TwinImmutableObject {

    public StructChanges(PropertyChanges propChanges) {
        changes = new SimpleMap<CalcProperty, Type>();
        for(CalcProperty prop : propChanges.getProperties()) {
            ModifyChange modify = propChanges.getModify(prop);
            Type type;
            if(modify.isFinal) {
                if(modify.isEmpty())
                    type = Type.NOUPDATE;
                else
                    type = Type.FINAL;
            } else {
                type = Type.NOTFINAL;
                assert !modify.isEmpty();
            }
            changes.add(prop, type);
        }
    }

    private enum Type {
        FINAL, NOUPDATE, NOTFINAL;

        public boolean isFinal() {
            return this==FINAL || this==NOUPDATE;
        }
    }
    private final QuickMap<CalcProperty, Type> changes;
    
    public boolean isEmpty() {
        return changes.isEmpty();
    }
    public int size() {
        return changes.size;
    }
    
    public boolean hasChanges(QuickSet<CalcProperty> props) {
        for(int i=0;i<props.size;i++)
            if(changes.get(props.get(i))!=Type.NOUPDATE)
                return true;
        return false;
    }

    public QuickSet<CalcProperty> getUsedChanges(CalcProperty property, boolean cascade) {
        Type propChange = changes.get(property);
        return QuickSet.add(propChange == null ? QuickSet.<CalcProperty>EMPTY() : new QuickSet<CalcProperty>(property),
                propChange != null && propChange.isFinal() ? QuickSet.<CalcProperty>EMPTY() : property.getUsedEventChange(this, cascade));
    }

    public QuickSet<CalcProperty> getUsedChanges(Collection<CalcProperty> col) {
        return getUsedChanges(col, false);
    }

    public QuickSet<CalcProperty> getUsedChanges(Collection<CalcProperty> col, boolean cascade) {
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
        for(CalcProperty<?> property : col)
            result.addAll(property.getUsedChanges(this, cascade));
        return result;
    }

    public QuickSet<CalcProperty> getUsedDataChanges(Collection<CalcProperty> col) {
        QuickSet<CalcProperty> result = new QuickSet<CalcProperty>();
        for(CalcProperty<?> property : col)
            result.addAll(property.getUsedDataChanges(this));
        return result;
    }

    public boolean twins(TwinImmutableInterface o) {
        return changes.equals(((StructChanges)o).changes);
    }

    public int immutableHashCode() {
        return changes.hashCode();
    }
}
