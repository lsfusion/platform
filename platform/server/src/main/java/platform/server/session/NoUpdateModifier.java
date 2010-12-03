package platform.server.session;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.*;

public abstract class NoUpdateModifier extends Modifier<NoUpdateModifier.UsedChanges> {

    public Collection<Property> hintsNoUpdate;

    protected NoUpdateModifier() {
        hintsNoUpdate = new HashSet<Property>();
    }
    protected NoUpdateModifier(Collection<Property> hintsNoUpdate) {
        this.hintsNoUpdate = hintsNoUpdate;
    }

    public static class UsedChanges extends Changes<UsedChanges> {
        final Set<Property> noUpdateProps;

        private UsedChanges() {
            noUpdateProps = new HashSet<Property>();
        }
        private final static UsedChanges EMPTY = new UsedChanges();

        public UsedChanges(NoUpdateModifier modifier) {
            super(modifier);
            noUpdateProps = new HashSet<Property>(modifier.hintsNoUpdate);
        }

        public UsedChanges(Property property) {
            noUpdateProps = Collections.singleton(property);            
        }

        private UsedChanges(UsedChanges changes, SessionChanges merge) {
            super(changes, merge);
            noUpdateProps = changes.noUpdateProps;
        }
        public UsedChanges addChanges(SessionChanges changes) {
            return new UsedChanges(this, changes);
        }

        private UsedChanges(UsedChanges changes, UsedChanges merge) {
            super(changes, merge);
            noUpdateProps = BaseUtils.mergeSet(changes.noUpdateProps,merge.noUpdateProps);
        }
        public UsedChanges add(UsedChanges changes) {
            return new UsedChanges(this, changes);
        }

        @Override
        public boolean modifyUsed() {
            return !noUpdateProps.isEmpty();
        }

        @Override
        protected boolean modifyEquals(UsedChanges changes) {
            return noUpdateProps.equals(changes.noUpdateProps);
        }

        @Override
        @IdentityLazy
        public int hashValues(HashValues hashValues) {
            return super.hashValues(hashValues) * 31 + noUpdateProps.hashCode();
        }

        private UsedChanges(UsedChanges usedChanges, MapValuesTranslate mapValues) {
            super(usedChanges, mapValues);
            noUpdateProps = usedChanges.noUpdateProps;
        }

        public UsedChanges translate(MapValuesTranslate mapValues) {
            return new UsedChanges(this, mapValues);
        }
    }

    public UsedChanges newFullChanges() {
        return new UsedChanges(this);
    }

    public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        if(hintsNoUpdate.contains(property)) // если так то ничего не менять
            return Expr.NULL;
        else
            return null;
    }

    public UsedChanges used(Property property, UsedChanges changes) {
        if(changes.hasChanges() && hintsNoUpdate.contains(property))
            return new UsedChanges(property);
        else
            return changes;
    }

    public UsedChanges newChanges() {
        return UsedChanges.EMPTY;
    }

    public boolean neededClass(Changes changes) {
        return changes instanceof UsedChanges;
    }
}
