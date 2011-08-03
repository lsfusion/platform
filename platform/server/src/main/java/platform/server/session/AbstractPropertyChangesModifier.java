package platform.server.session;

import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractPropertyChangesModifier<P extends PropertyInterface, T extends Property<P>, AC extends AbstractPropertyChanges<P,T,AC>,
                                            UC extends AbstractPropertyChangesModifier.UsedChanges<P,T,AC,UC>> extends Modifier<UC> {

    final ExprChanges session;
    final AC changes;

    public AbstractPropertyChangesModifier(Modifier modifier, AC changes) {
        this.session = modifier.getSession();
        this.changes = changes;
    }

    public ExprChanges getSession() {
        return session;
    }

    protected abstract static class UsedChanges<P extends PropertyInterface, T extends Property<P>, AC extends AbstractPropertyChanges<P,T,AC>,
                                    UC extends UsedChanges<P,T,AC,UC>> extends Changes<UC> {
        AC changes;

        protected UsedChanges(AC changes) {
            this.changes = changes;
        }

        protected UsedChanges(AbstractPropertyChangesModifier<P,T,AC,UC> modifier) {
            super(modifier);
            this.changes = modifier.changes; // immutable не страшно
        }

        @Override
        public boolean modifyUsed() {
            return !changes.isEmpty();
        }

        @Override
        public boolean hasChanges() {
            return super.hasChanges() || modifyUsed();
        }

        protected UsedChanges(UC changes, Changes merge) {
            super(changes, merge, true);
            this.changes = changes.changes;
        }

        protected UsedChanges(UC changes, UC merge) {
            super(changes, merge);
            this.changes = changes.changes.add(merge.changes);
        }

        @Override
        protected boolean modifyEquals(UC changes) {
            return this.changes.equals(changes.changes);
        }

        @Override
        @IdentityLazy
        public int hashValues(HashValues hashValues) {
            return super.hashValues(hashValues) * 31 + changes.hashValues(hashValues);
        }

        @Override
        @IdentityLazy
        public Set<Value> getValues() {
            Set<Value> result = new HashSet<Value>();
            result.addAll(super.getValues());
            result.addAll(changes.getValues());
            return result;
        }

        protected UsedChanges(UC usedChanges, MapValuesTranslate mapValues) {
            super(usedChanges, mapValues);
            changes = usedChanges.changes.translate(mapValues);
        }
    }

    protected abstract UC createChanges(T property, PropertyChange<P> change);
    protected abstract PropertyChange<P> getPropertyChange(Property property);

    public UC preUsed(Property property) {
        PropertyChange<P> propertyChange;
        if((propertyChange = getPropertyChange(property))!=null)
            return createChanges((T) property, propertyChange);
        return null;
    }

    // переносим DataProperty на выход, нужно на самом деле сделать calculateExpr и if равняется то keyExpr, иначе старое значение но по старому значению будет false
    public <E extends PropertyInterface> Expr changed(Property<E> property, Map<E, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        PropertyChange<E> dataChange;
        if((dataChange = (PropertyChange<E>) getPropertyChange(property))!=null) {
            Join<String> join = dataChange.join(joinImplement);
            if(changedWhere !=null) changedWhere.add(join.getWhere());
            return join.getExpr("value");
        } else // иначе не трогаем
            return null;
    }

}
