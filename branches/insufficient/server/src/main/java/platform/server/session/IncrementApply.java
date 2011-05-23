package platform.server.session;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValues;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.KeyField;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;

// вообщем то public потому как иначе aspect не ловит
public class IncrementApply extends Modifier<IncrementApply.UsedChanges> {
    private Collection<SessionTableUsage<KeyField, Property>> temporaryTables = new ArrayList<SessionTableUsage<KeyField, Property>>();
    Map<Property, SessionTableUsage<KeyField, Property>> tables = new HashMap<Property, SessionTableUsage<KeyField, Property>>();

    public final DataSession session;

    public ExprChanges getSession() {
        return session;
    }

    IncrementApply(DataSession session) {
        this.session = session;
        temporaryTables = new ArrayList<SessionTableUsage<KeyField, Property>>();
    }

    public static class UsedChanges extends Changes<UsedChanges> {
        final Map<Property, MapValues> increment;

        private UsedChanges() {
             increment = new HashMap<Property, MapValues>();
        }
        private final static UsedChanges EMPTY = new UsedChanges();

        public UsedChanges(IncrementApply modifier) {
            super(modifier);
            increment = new HashMap<Property, MapValues>();
            for(Map.Entry<Property, SessionTableUsage<KeyField, Property>> incrementEntry : modifier.tables.entrySet())
                increment.put(incrementEntry.getKey(), incrementEntry.getValue().getUsage());
        }

        @Override
        public boolean modifyUsed() {
            return !increment.isEmpty();
        }

        @Override
        public boolean hasChanges() {
            return super.hasChanges() || modifyUsed();
        }

        private UsedChanges(UsedChanges changes, Changes merge) {
            super(changes, merge, true);
            increment = changes.increment;
        }
        public UsedChanges addChanges(Changes changes) {
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
        public Set<Value> getValues() {
            Set<Value> result = new HashSet<Value>();
            result.addAll(super.getValues());
            MapValuesIterable.enumValues(result, increment);
            return result;
        }

        public UsedChanges(Property property, SessionTableUsage<KeyField, Property> incrementTable) {
            increment = Collections.singletonMap(property, incrementTable.getUsage());
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
        SessionTableUsage<KeyField, Property> incrementTable = tables.get(property);
        if(incrementTable!=null) { // если уже все посчитано - просто возвращаем его
            Join<Property> incrementJoin = incrementTable.join(crossJoin(property.mapTable.mapKeys, joinImplement));
            changedWhere.add(incrementJoin.getWhere());
            return incrementJoin.getExpr(property);
        } else
            return null;
    }

    public boolean neededClass(Changes changes) {
        return changes instanceof UsedChanges;
    }

    public UsedChanges used(Property property, UsedChanges usedChanges) {
        SessionTableUsage<KeyField, Property> incrementTable = tables.get(property);
        if(incrementTable!=null)
            return new UsedChanges(property, incrementTable);
        else
            return usedChanges;
    }

    public UsedChanges newChanges() {
        return UsedChanges.EMPTY;
    }

    public SessionTableUsage<KeyField, Property> read(ImplementTable implement, Collection<Property> properties, BaseClass baseClass) throws SQLException {
        if (properties.size() == 1) {
            SessionTableUsage<KeyField, Property> changeTable = tables.get(BaseUtils.single(properties));
            if (changeTable != null) {
                return changeTable;
            }
        }

        // создаем таблицу
        SessionTableUsage<KeyField, Property> changeTable =
                new SessionTableUsage<KeyField, Property>(implement.keys, new ArrayList<Property>(properties),
                                                          new Type.Getter<KeyField>() {
                                                              public Type getType(KeyField key) {
                                                                  return key.type;
                                                              }
                                                          },
                                                          new Type.Getter<Property>() {
                                                              public Type getType(Property key) {
                                                                  return key.getType();
                                                              }
                                                          });

        // подготавливаем запрос
        Query<KeyField, Property> changesQuery = new Query<KeyField, Property>(implement);
        WhereBuilder changedWhere = new WhereBuilder();
        for (Property property : properties) {
            changesQuery.properties.put(property, property.getIncrementExpr(changesQuery.mapKeys, this, changedWhere));
        }
        changesQuery.and(changedWhere.toWhere());

        // подготовили - теперь надо сохранить в курсор и записать классы
        changeTable.writeRows(session.sql, changesQuery, baseClass, session.env);

        for (Property property : properties) {
            tables.put(property, changeTable);
        }

        temporaryTables.add(changeTable);

        return changeTable;
    }

    public Map<ImplementTable, Collection<Property>> groupPropertiesByTables() {
       return BaseUtils.group(
                new BaseUtils.Group<ImplementTable, Property>() {
                    public ImplementTable group(Property key) {
                        return key.mapTable.table;
                    }
                }, tables.keySet());
    }

    public void dropTemporaryTables() throws SQLException {
        for (SessionTableUsage<KeyField, Property> addTable : temporaryTables) {
            addTable.drop(session.sql);
        }
    }
}
