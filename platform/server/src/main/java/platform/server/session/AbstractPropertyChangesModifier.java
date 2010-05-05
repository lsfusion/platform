package platform.server.session;

import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.Property;
import platform.server.caches.hash.HashValues;
import platform.server.caches.Lazy;
import platform.server.caches.GenericImmutable;
import platform.server.caches.GenericLazy;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.data.query.Join;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import net.jcip.annotations.Immutable;

public abstract class AbstractPropertyChangesModifier<P extends PropertyInterface, T extends Property<P>, AC extends AbstractPropertyChanges<P,T,AC>,
                                            UC extends AbstractPropertyChangesModifier.UsedChanges<P,T,AC,UC>> extends Modifier<UC> {

    final SessionChanges session;
    final AC changes;

    public AbstractPropertyChangesModifier(Modifier modifier, AC changes) {
        this.session = modifier.getSession();
        this.changes = changes;
    }

    public SessionChanges getSession() {
        return session;
    }

    @GenericImmutable
    protected abstract static class UsedChanges<P extends PropertyInterface, T extends Property<P>, AC extends AbstractPropertyChanges<P,T,AC>,
                                    UC extends UsedChanges<P,T,AC,UC>> extends Changes<UC> {
        AC changes;

        protected UsedChanges(AC changes) {
            this.changes = changes;
        }

        @Override
        public boolean hasChanges() {
            return super.hasChanges() || changes.hasChanges();
        }

        @Override
        public void add(UC add) {
            super.add(add);
            changes = changes.add(add.changes);
        }

        @Override
        public boolean equals(Object o) {
            return this==o || o instanceof UsedChanges && changes.equals(((UsedChanges)o).changes) && super.equals(o);
        }

        @Override
        @GenericLazy
        public int hashValues(HashValues hashValues) {
            return super.hashValues(hashValues) * 31 + changes.hashValues(hashValues);
        }

        @Override
        public Set<ValueExpr> getValues() {
            Set<ValueExpr> result = new HashSet<ValueExpr>();
            result.addAll(super.getValues());
            result.addAll(changes.getValues());
            return result;
        }

        protected UsedChanges(UC usedChanges, Map<ValueExpr, ValueExpr> mapValues) {
            super(usedChanges, mapValues);
            changes = usedChanges.changes.translate(mapValues);
        }
    }

    protected abstract UC createChanges(T property, PropertyChange<P> change);
    protected abstract PropertyChange<P> getPropertyChange(Property property);

    public UC used(Property property, UC usedChanges) {
        PropertyChange<P> propertyChange;
        if((propertyChange = getPropertyChange(property))!=null)
            usedChanges = createChanges((T) property, propertyChange);
        return usedChanges;
    }

    // переносим DataProperty на выход, нужно на самом деле сделать calculateExpr и if равняется то keyExpr, иначе старое значение но по старому значению будет false
    public <E extends PropertyInterface> Expr changed(Property<E> property, Map<E, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        PropertyChange<E> dataChange;
        if((dataChange = (PropertyChange<E>) getPropertyChange(property))!=null) {
            Join<String> join = dataChange.getQuery("value").join(joinImplement);
            if(changedWhere !=null) changedWhere.add(join.getWhere());
            return join.getExpr("value");
        } else // иначе не трогаем
            return null;
    }

}
