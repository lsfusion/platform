package platform.server.session;

import net.jcip.annotations.Immutable;
import platform.server.caches.Lazy;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.DataProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.UserProperty;

@Immutable
public class DataChangesModifier extends AbstractPropertyChangesModifier<ClassPropertyInterface,UserProperty,DataChanges,DataChangesModifier.UsedChanges> {

    public DataChangesModifier(Modifier modifier, DataChanges changes) {
        super(modifier, changes);
    }

    protected static class UsedChanges extends AbstractPropertyChangesModifier.UsedChanges<ClassPropertyInterface,UserProperty,DataChanges,UsedChanges> {

        public UsedChanges(UserProperty property, PropertyChange<ClassPropertyInterface> change) {
            super(new DataChanges(property, change));
        }

        private UsedChanges() {
            super(new DataChanges());
        }
        private final static UsedChanges EMPTY = new UsedChanges();

        public UsedChanges(DataChangesModifier modifier) {
            super(modifier);
        }

        protected UsedChanges(UsedChanges usedChanges, MapValuesTranslate mapValues) {
            super(usedChanges, mapValues);
        }

        public UsedChanges translate(MapValuesTranslate mapValues) {
            return new UsedChanges(this, mapValues);
        }

        private UsedChanges(UsedChanges changes, SessionChanges merge) {
            super(changes, merge);
        }
        public UsedChanges addChanges(SessionChanges changes) {
            return new UsedChanges(this, changes);
        }

        private UsedChanges(UsedChanges changes, UsedChanges merge) {
            super(changes, merge);
        }
        public UsedChanges add(UsedChanges changes) {
            return new UsedChanges(this, changes);
        }
    }

    public UsedChanges newChanges() {
        return UsedChanges.EMPTY;
    }

    @Lazy
    public UsedChanges fullChanges() {
        return new UsedChanges(this);
    }

    protected UsedChanges createChanges(UserProperty property, PropertyChange<ClassPropertyInterface> change) {
        return new UsedChanges(property, change);
    }

    protected PropertyChange<ClassPropertyInterface> getPropertyChange(Property property) {
        if(property instanceof DataProperty)
            return changes.get((DataProperty) property);
        else
            return null;
    }

    public boolean neededClass(Changes changes) {
        return changes instanceof UsedChanges;
    }
}
