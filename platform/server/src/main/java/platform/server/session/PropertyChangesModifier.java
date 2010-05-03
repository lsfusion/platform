package platform.server.session;

import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.data.expr.ValueExpr;

import java.util.Map;

public class PropertyChangesModifier extends AbstractPropertyChangesModifier<PropertyInterface, Property<PropertyInterface>, PropertyChanges, PropertyChangesModifier.UsedChanges> {

    public PropertyChangesModifier(Modifier modifier, PropertyChanges changes) {
        super(modifier, changes);
    }

    protected static class UsedChanges extends AbstractPropertyChangesModifier.UsedChanges<PropertyInterface,Property<PropertyInterface>,PropertyChanges,UsedChanges> {

        public UsedChanges(Property property, PropertyChange<PropertyInterface> change) {
            super(new PropertyChanges(change, property));
        }

        public UsedChanges() {
            super(new PropertyChanges());
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

    protected UsedChanges createChanges(Property<PropertyInterface> property, PropertyChange<PropertyInterface> change) {
        return new UsedChanges(property, change);
    }

    protected PropertyChange<PropertyInterface> getPropertyChange(Property property) {
        return changes.get(property);
    }
}
