package platform.server.session;

import platform.base.BaseUtils;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValues;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.KeyField;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;

// modifier который выключает инкрементность \ перечитывает в таблицы
public abstract class AbstractIncrementProps<T, U extends AbstractIncrementProps.UsedChanges<U>> extends Modifier<U> {
    protected Map<Property, PropertyGroup<T>> incrementGroups = new HashMap<Property, PropertyGroup<T>>();
    protected Map<PropertyGroup<T>, SessionTableUsage<T, Property>> tables = new HashMap<PropertyGroup<T>, SessionTableUsage<T,Property>>();

    public SessionTableUsage<T, Property> getTable(Property property) {
        PropertyGroup<T> incrementGroup = incrementGroups.get(property);
        if(incrementGroup!=null)
            return tables.get(incrementGroup);
        else
            return null;
    }

    public <V extends PropertyInterface> SessionTableUsage<V, Property> getMapTable(Property<V> property) {
        SessionTableUsage<T, Property> table = getTable(property);
        if(table!=null) {
            PropertyGroup<T> group = incrementGroups.get(property);
            return table.map(BaseUtils.reverse(group.getPropertyMap(property)));
        } else
            return null;
    }

    protected Set<Property> noUpdate = new HashSet<Property>();

    public abstract ExprChanges getSession();
    public abstract SQLSession getSql();
    public abstract QueryEnvironment getEnv();

    protected AbstractIncrementProps() {
        this(new HashSet<Property>());
    }

    protected AbstractIncrementProps(Set<Property> noUpdate) {
        this.noUpdate = noUpdate;
    }

    public abstract static class UsedChanges<U extends UsedChanges<U>> extends Changes<U> {
        private final Map<Property, MapValues> increment;
        private final Set<Property> noUpdate;

        protected UsedChanges() {
            increment = new HashMap<Property, MapValues>();
            noUpdate = new HashSet<Property>();
        }

        public <T> UsedChanges(AbstractIncrementProps<T, U> modifier) {
            super(modifier);
            increment = new HashMap<Property, MapValues>();
            for(Map.Entry<Property, PropertyGroup<T>> incrementEntry : modifier.incrementGroups.entrySet())
                increment.put(incrementEntry.getKey(), modifier.tables.get(incrementEntry.getValue()).getUsage());
            noUpdate = new HashSet<Property>(modifier.noUpdate);
        }

        @Override
        public boolean modifyUsed() {
            return !increment.isEmpty() || !noUpdate.isEmpty();
        }

        public boolean hasChanges() {
            return super.hasChanges() || !increment.isEmpty();
        }

        protected UsedChanges(U changes, Changes merge) {
            super(changes, merge, true);
            increment = changes.increment;
            noUpdate = changes.noUpdate;
        }

        protected UsedChanges(U changes, U merge) {
            super(changes, merge);
            increment = BaseUtils.merge(changes.increment, merge.increment);
            noUpdate = BaseUtils.mergeSet(changes.noUpdate, merge.noUpdate);
        }

        @Override
        protected boolean modifyEquals(U changes) {
            return increment.equals(changes.increment) && noUpdate.equals(changes.noUpdate);
        }

        @Override
        @HashLazy
        public int hashValues(HashValues hashValues) {
            return 31 * (super.hashValues(hashValues) * 31 + MapValuesIterable.hash(increment, hashValues)) + noUpdate.hashCode();
        }

        @Override
        @IdentityLazy
        public Set<Value> getValues() {
            Set<Value> result = new HashSet<Value>();
            result.addAll(super.getValues());
            MapValuesIterable.enumValues(result, increment);
            return result;
        }

        public UsedChanges(Property property, SessionTableUsage<?, Property> incrementTable) {
            increment = Collections.singletonMap(property, incrementTable.getUsage());
            noUpdate = new HashSet<Property>();
        }

        public UsedChanges(Property property) {
            increment = new HashMap<Property, MapValues>();
            noUpdate = Collections.singleton(property);
        }

        protected UsedChanges(U usedChanges, MapValuesTranslate mapValues) {
            super(usedChanges, mapValues);
            increment = mapValues.translateValues(usedChanges.increment);
            noUpdate = usedChanges.noUpdate;
        }
    }

    public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        SessionTableUsage<P, Property> incrementTable = getMapTable(property);
        if(incrementTable!=null) { // если уже все посчитано - просто возвращаем его
            Join<Property> incrementJoin = incrementTable.join(joinImplement);
            changedWhere.add(incrementJoin.getWhere());
            return incrementJoin.getExpr(property);
        }

        if(noUpdate.contains(property)) // если так то ничего не менять
            return Expr.NULL;

        return null;
    }

    public boolean neededClass(Changes changes) {
        return changes instanceof UsedChanges;
    }

    protected abstract U newUsedChanges(Property property, SessionTableUsage<T, Property> incrementTable);
    public U preUsed(Property property) {
        SessionTableUsage<T, Property> incrementTable = getTable(property);
        if(incrementTable!=null)
            return newUsedChanges(property, incrementTable);
        return null;
    }
    protected abstract U newUsedChanges(Property property);
    @Override
    public U postUsed(Property property, U changes) {
        if(noUpdate.contains(property) && changes.hasChanges())
            return newUsedChanges(property);
        else
            return changes;
    }

    public interface PropertyGroup<T> {
            List<T> getKeys();
            Type.Getter<T> typeGetter();
            <P extends PropertyInterface> Map<P, T> getPropertyMap(Property<P> property);
    }

    private <T, P extends PropertyInterface> Expr genericGetIncrementExpr(PropertyGroup<T> group, Map<T, ? extends Expr> joinExprs, Property<P> property, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return property.getIncrementExpr(BaseUtils.join(group.getPropertyMap(property), joinExprs), modifier, changedWhere);
    }

    @Message("message.increment.read.properties")
    private <T> SessionTableUsage<T, Property> readTable(PropertyGroup<T> propertyGroup, @ParamMessage Collection<Property> properties, BaseClass baseClass) throws SQLException {
        SessionTableUsage<T, Property> changeTable =
                new SessionTableUsage<T, Property>(propertyGroup.getKeys(), new ArrayList<Property>(properties), propertyGroup.typeGetter(),
                                                          new Type.Getter<Property>() {
                                                              public Type getType(Property key) {
                                                                  return key.getType();
                                                              }
                                                          });

        // подготавливаем запрос
        Query<T, Property> changesQuery = new Query<T, Property>(propertyGroup.getKeys());
        WhereBuilder changedWhere = new WhereBuilder();
        for (Property<?> property : properties)
            changesQuery.properties.put(property, genericGetIncrementExpr(propertyGroup, changesQuery.mapKeys, property, this, changedWhere));
        changesQuery.and(changedWhere.toWhere());

        // подготовили - теперь надо сохранить в курсор и записать классы
        changeTable.writeRows(getSql(), changesQuery, baseClass, getEnv());
        return changeTable;
    }

    private void add(PropertyGroup<T> propertyGroup, Collection<Property> properties, SessionTableUsage<T, Property> changeTable) {
        for (Property property : properties)
            incrementGroups.put(property, propertyGroup);
        tables.put(propertyGroup, changeTable);
    }

    public void remove(Property property) { // assert что в incrementGroups больше нет Property
        PropertyGroup<T> prevGroup = incrementGroups.remove(property);
        assert !incrementGroups.containsValue(prevGroup);
        if(prevGroup!=null)
            tables.remove(prevGroup);
    }

    public void add(Property property, SessionTableUsage<T, Property> changeTable) { // assert что в incrementGroups больше нет Property
        add(property.getTableGroup(), Collections.singleton(property), changeTable);
    }

    public SessionTableUsage<KeyField, Property> readTable(Property property, BaseClass baseClass) throws SQLException {
        return readTable(property.getTableGroup(), Collections.singleton(property), baseClass);
    }

    public SessionTableUsage<T, Property> read(PropertyGroup<T> propertyGroup, Collection<Property> properties, BaseClass baseClass) throws SQLException {
        // создаем таблицу
        SessionTableUsage<T, Property> changeTable = readTable(propertyGroup, properties, baseClass);
        add(propertyGroup, properties, changeTable);
        return changeTable;
    }

    public void cleanIncrementTables() throws SQLException {
        for (SessionTableUsage<T, Property> addTable : tables.values()) {
            addTable.drop(getSql());
        }
        tables = new HashMap<PropertyGroup<T>, SessionTableUsage<T,Property>>();
        incrementGroups = new HashMap<Property, PropertyGroup<T>>();
    }
}
