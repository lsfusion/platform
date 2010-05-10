package platform.server.view.form;

import platform.server.session.Modifier;
import platform.server.session.Changes;
import platform.server.session.SessionChanges;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.caches.hash.HashValues;
import platform.server.caches.Lazy;
import platform.base.BaseUtils;

import java.util.*;

import net.jcip.annotations.Immutable;

public abstract class NoUpdateModifier extends Modifier<NoUpdateModifier.UsedChanges> {

    public Collection<Property> hintsNoUpdate;

    protected NoUpdateModifier() {
        hintsNoUpdate = new HashSet<Property>();
    }
    protected NoUpdateModifier(Collection<Property> hintsNoUpdate) {
        this.hintsNoUpdate = hintsNoUpdate;
    }

    @Immutable
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
        public boolean equals(Object o) {
            return this==o || o instanceof UsedChanges && noUpdateProps.equals(((UsedChanges)o).noUpdateProps) && super.equals(o);
        }

        @Override
        @Lazy
        public int hashValues(HashValues hashValues) {
            return 31 * super.hashValues(hashValues) + noUpdateProps.hashCode();
        }

        private UsedChanges(UsedChanges usedChanges, Map<ValueExpr,ValueExpr> mapValues) {
            super(usedChanges, mapValues);
            noUpdateProps = usedChanges.noUpdateProps;
        }

        public UsedChanges translate(Map<ValueExpr,ValueExpr> mapValues) {
            return new UsedChanges(this, mapValues);
        }
    }

    public UsedChanges fullChanges() {
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

}
