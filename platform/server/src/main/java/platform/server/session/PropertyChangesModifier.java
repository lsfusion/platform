package platform.server.session;

import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.data.expr.ValueExpr;
import platform.server.caches.Lazy;

import net.jcip.annotations.Immutable;

import java.util.Map;

@Immutable
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

        protected UsedChanges(UsedChanges usedChanges, Map<ValueExpr,ValueExpr> mapValues) {
            super(usedChanges, mapValues);
        }
        
        public UsedChanges translate(Map<ValueExpr,ValueExpr> mapValues) {
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
            return new UsedChanges(this,changes);
        }
    }

    public UsedChanges newChanges() {
        return UsedChanges.EMPTY;
    }

    @Lazy
    public UsedChanges fullChanges() {
        return new UsedChanges(this);
    }

    protected UsedChanges createChanges(Property<PropertyInterface> property, PropertyChange<PropertyInterface> change) {
        return new UsedChanges(property, change);
    }

    protected PropertyChange<PropertyInterface> getPropertyChange(Property property) {
        return changes.get(property);
    }
}
