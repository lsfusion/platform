package platform.server.session;

import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.WhereBuilder;
import platform.server.data.PropertyField;
import platform.server.data.KeyField;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.classes.BaseClass;
import platform.base.BaseUtils;

import java.util.*;
import java.sql.SQLException;

// вообщем то public потому как иначе aspect не ловит
public class IncrementApply extends Modifier<IncrementApply.UsedChanges> {

    Map<Property, IncrementChangeTable> tables = new HashMap<Property, IncrementChangeTable>();

    public final DataSession session;

    public SessionChanges getSession() {
        return session.changes;
    }

    IncrementApply(DataSession session) {
        this.session = session;
    }

    public static class UsedChanges extends Changes<UsedChanges> {
        final Map<Property, IncrementChangeTable> increment;

        private UsedChanges() {
             increment = new HashMap<Property, IncrementChangeTable>();
        }
        private final static UsedChanges EMPTY = new UsedChanges();

        public UsedChanges(IncrementApply modifier) {
             super(modifier);
             increment = new HashMap<Property, IncrementChangeTable>(modifier.tables);
        }

        @Override
        public boolean modifyUsed() {
            return !increment.isEmpty();
        }

        @Override
        public boolean hasChanges() {
            return super.hasChanges() || modifyUsed();
        }

        private UsedChanges(UsedChanges changes, SessionChanges merge) {
            super(changes, merge);
            increment = changes.increment;
        }
        public UsedChanges addChanges(SessionChanges changes) {
            return new UsedChanges(this, changes);
        }

        private UsedChanges(UsedChanges changes, UsedChanges merge) {
            super(changes, merge);
            increment = BaseUtils.merge(changes.increment, merge.increment);
        }
        public UsedChanges add(UsedChanges changes) {
            return new UsedChanges(this, changes);
        }

        @Override
        protected boolean modifyEquals(UsedChanges changes) {
            return increment.equals(changes.increment);
        }

        @Override
        @IdentityLazy
        public int hashValues(HashValues hashValues) {
            return super.hashValues(hashValues) * 31 + MapValuesIterable.hash(increment,hashValues);
        }

        @Override
        @IdentityLazy
        public Set<ValueExpr> getValues() {
            Set<ValueExpr> result = new HashSet<ValueExpr>();
            result.addAll(super.getValues());
            MapValuesIterable.enumValues(result, increment);
            return result;
        }

        public UsedChanges(Property property, IncrementChangeTable table) {
            increment = Collections.singletonMap(property, table);
        }

        private UsedChanges(UsedChanges usedChanges, MapValuesTranslate mapValues) {
            super(usedChanges, mapValues);
            increment = mapValues.translateValues(usedChanges.increment);
        }

        public UsedChanges translate(MapValuesTranslate mapValues) {
            return new UsedChanges(this, mapValues);
        }


    }

    public UsedChanges newFullChanges() {
        return new UsedChanges(this);
    }

    public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        IncrementChangeTable incrementTable = tables.get(property);
        if(incrementTable!=null) { // если уже все посчитано - просто возвращаем его
            Join<PropertyField> incrementJoin = incrementTable.join(BaseUtils.join(BaseUtils.reverse(BaseUtils.join(property.mapTable.mapKeys, incrementTable.mapKeys)), joinImplement));
            changedWhere.add(incrementJoin.getWhere());
            return incrementJoin.getExpr(incrementTable.changes.get(property));
        } else
            return null;
    }

    public boolean neededClass(Changes changes) {
        return changes instanceof UsedChanges;
    }

    public UsedChanges used(Property property, UsedChanges usedChanges) {
        IncrementChangeTable incrementTable = tables.get(property);
        if(incrementTable!=null)
            return new UsedChanges(property, incrementTable);
        else
            return usedChanges;
    }

    public UsedChanges newChanges() {
        return UsedChanges.EMPTY;
    }

    public IncrementChangeTable read(Collection<Property> properties, BaseClass baseClass) throws SQLException {
        // создаем таблицу
        IncrementChangeTable changeTable = new IncrementChangeTable(properties);
        session.createTemporaryTable(changeTable);

        // подготавливаем запрос
        Query<KeyField,PropertyField> changesQuery = new Query<KeyField, PropertyField>(changeTable);
        WhereBuilder changedWhere = new WhereBuilder();
        for(Map.Entry<Property,PropertyField> change : changeTable.changes.entrySet())
            changesQuery.properties.put(change.getValue(),
                    change.getKey().getIncrementExpr(BaseUtils.join(changeTable.mapKeys, changesQuery.mapKeys), this, changedWhere));
        changesQuery.and(changedWhere.toWhere());

        // подготовили - теперь надо сохранить в курсор и записать классы
        changeTable = changeTable.writeRows(session, changesQuery, baseClass);

        for(Property property : properties)
            tables.put(property,changeTable);

        return changeTable;
    }
}
