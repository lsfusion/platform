package platform.server.session;

import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.data.expr.ValueExpr;

import java.util.Map;

public class DataChangesModifier extends AbstractPropertyChangesModifier<ClassPropertyInterface,DataProperty,DataChanges,DataChangesModifier.UsedChanges> {

    public DataChangesModifier(Modifier modifier, DataChanges changes) {
        super(modifier, changes);
    }

    protected static class UsedChanges extends AbstractPropertyChangesModifier.UsedChanges<ClassPropertyInterface,DataProperty,DataChanges,UsedChanges> {

        public UsedChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) {
            super(new DataChanges(property, change));
        }

        public UsedChanges() {
            super(new DataChanges());
        }

        protected UsedChanges(UsedChanges usedChanges, Map<ValueExpr, ValueExpr> mapValues) {
            super(usedChanges, mapValues);
        }

        public UsedChanges translate(Map<ValueExpr, ValueExpr> mapValues) {
            return new UsedChanges(this, mapValues);
        }
    }

    public UsedChanges newChanges() {
        return new UsedChanges();
    }
    
    protected UsedChanges createChanges(DataProperty property, PropertyChange<ClassPropertyInterface> change) {
        return new UsedChanges(property, change);
    }

    protected PropertyChange<ClassPropertyInterface> getPropertyChange(Property property) {
        if(property instanceof DataProperty)
            return changes.get((DataProperty) property);
        else
            return null;
    }
}
