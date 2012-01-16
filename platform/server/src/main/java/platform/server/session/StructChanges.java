package platform.server.session;

import platform.base.*;
import platform.server.logics.property.Property;

import java.util.Collection;

public class StructChanges extends TwinImmutableObject {

    public StructChanges(PropertyChanges propChanges) {
        changes = new SimpleMap<Property, Type>();
        for(Property prop : propChanges.getProperties()) {
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
    private final QuickMap<Property, Type> changes;
    
    public boolean isEmpty() {
        return changes.isEmpty();
    }
    public int size() {
        return changes.size;
    }
    
    public boolean hasChanges(QuickSet<Property> props) {
        for(int i=0;i<props.size;i++)
            if(changes.get(props.get(i))!=Type.NOUPDATE)
                return true;
        return false;
    }

    public QuickSet<Property> getUsedChanges(Property property) {
        Type propChange = changes.get(property);
        return QuickSet.add(propChange == null ? QuickSet.<Property>EMPTY() : new QuickSet<Property>(property),
                propChange != null && propChange.isFinal() ? QuickSet.<Property>EMPTY() : property.getUsedDerivedChange(this));
    }

    public QuickSet<Property> getUsedChanges(Collection<Property> col) {
        QuickSet<Property> result = new QuickSet<Property>();
        for(Property<?> property : col)
            result.addAll(property.getUsedChanges(this));
        return result;
    }

    public QuickSet<Property> getUsedDataChanges(Collection<Property> col) {
        QuickSet<Property> result = new QuickSet<Property>();
        for(Property<?> property : col)
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
