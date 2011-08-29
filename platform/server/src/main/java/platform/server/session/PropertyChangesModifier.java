package platform.server.session;

import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

public class PropertyChangesModifier extends AbstractPropertyChangesModifier<PropertyInterface, Property<PropertyInterface>, PropertyChanges, PropertyChangesModifier.UsedChanges> {

    public PropertyChangesModifier(Modifier modifier, PropertyChanges changes) {
        super(modifier, changes);
    }

    protected static class UsedChanges extends AbstractPropertyChangesModifier.UsedChanges<PropertyInterface,Property<PropertyInterface>,PropertyChanges,UsedChanges> {

        public UsedChanges(Property property, PropertyChange<PropertyInterface> change) {
            super(new PropertyChanges(change, property));
        }

        private UsedChanges() {
            super(new PropertyChanges());
        }
        private static final UsedChanges EMPTY = new UsedChanges();

        public UsedChanges(PropertyChangesModifier modifier) {
            super(modifier);
        }

        protected UsedChanges(UsedChanges usedChanges, MapValuesTranslate mapValues) {
            super(usedChanges, mapValues);
        }
        
        public UsedChanges translate(MapValuesTranslate mapValues) {
            return new UsedChanges(this, mapValues);
        }

        private UsedChanges(UsedChanges changes, Changes merge) {
            super(changes, merge);
        }
        public UsedChanges calculateAddChanges(Changes changes) {
            return new UsedChanges(this, changes);
        }

        private UsedChanges(UsedChanges changes, UsedChanges merge) {
            super(changes, merge);
        }
        public UsedChanges calculateAdd(UsedChanges changes) {
            return new UsedChanges(this,changes);
        }
    }

    public UsedChanges newChanges() {
        return UsedChanges.EMPTY;
    }

    public UsedChanges newFullChanges() {
        return new UsedChanges(this);
    }

    protected UsedChanges createChanges(Property<PropertyInterface> property, PropertyChange<PropertyInterface> change) {
        return new UsedChanges(property, change);
    }

    protected PropertyChange<PropertyInterface> getPropertyChange(Property property) {
        return changes.get(property);
    }

    @Override
    public boolean neededClass(Changes changes) {
        return changes instanceof UsedChanges;
    }
}
