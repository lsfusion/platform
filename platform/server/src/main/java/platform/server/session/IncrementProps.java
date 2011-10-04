package platform.server.session;

import com.sun.servicetag.SystemEnvironment;
import platform.base.BaseUtils;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValues;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.Value;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.BusinessLogics;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;

// modifier который выключает инкрементность \ перечитывает в таблицы
public class IncrementProps<T> extends Modifier<IncrementProps.UsedChanges> {
    protected Map<Property, PropertyGroup<T>> incrementGroups = new HashMap<Property, PropertyGroup<T>>();
    protected Map<PropertyGroup<T>, SessionTableUsage<T, Property>> tables = new HashMap<PropertyGroup<T>, SessionTableUsage<T,Property>>();

    protected Set<Property> noUpdate = new HashSet<Property>();

    public final DataSession session;

    public ExprChanges getSession() {
        return session;
    }

    public IncrementProps(DataSession session) {
        this.session = session;
        noUpdate = new HashSet<Property>();
    }

    public IncrementProps(Set<Property> noUpdate) { // идиотизм конечно, но в противном случае нужно будет сложную структуру делать
        session = null;
        this.noUpdate = noUpdate;
    }

    public static class UsedChanges extends Changes<UsedChanges> {
        private final Map<Property, MapValues> increment;
        private final Set<Property> noUpdate;

        private UsedChanges() {
            increment = new HashMap<Property, MapValues>();
            noUpdate = new HashSet<Property>();
        }
        private final static UsedChanges EMPTY = new UsedChanges();

        public <T> UsedChanges(IncrementProps<T> modifier) {
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

        @Override
        public boolean hasChanges() {
            return super.hasChanges() || modifyUsed();
        }

        private UsedChanges(UsedChanges changes, Changes merge) {
            super(changes, merge, true);
            increment = changes.increment;
            noUpdate = changes.noUpdate;
        }
        public UsedChanges calculateAddChanges(Changes changes) {
            return new UsedChanges(this, changes);
        }

        private UsedChanges(UsedChanges changes, UsedChanges merge) {
            super(changes, merge);
            increment = BaseUtils.merge(changes.increment, merge.increment);
            noUpdate = BaseUtils.mergeSet(changes.noUpdate, merge.noUpdate);
        }
        public UsedChanges calculateAdd(UsedChanges changes) {
            return new UsedChanges(this, changes);
        }

        @Override
        protected boolean modifyEquals(UsedChanges changes) {
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

        private UsedChanges(UsedChanges usedChanges, MapValuesTranslate mapValues) {
            super(usedChanges, mapValues);
            increment = mapValues.translateValues(usedChanges.increment);
            noUpdate = usedChanges.noUpdate;
        }

        public UsedChanges translate(MapValuesTranslate mapValues) {
            return new UsedChanges(this, mapValues);
        }
    }

    public UsedChanges newFullChanges() {
        return new UsedChanges(this);
    }

    public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
        PropertyGroup<T> incrementGroup = incrementGroups.get(property);
        if(incrementGroup!=null) { // если уже все посчитано - просто возвращаем его
            Join<Property> incrementJoin = tables.get(incrementGroup).join(crossJoin(incrementGroup.getPropertyMap(property), joinImplement));
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

    public UsedChanges preUsed(Property property) {
        PropertyGroup<T> incrementGroup = incrementGroups.get(property);
        if(incrementGroup!=null)
            return new UsedChanges(property, tables.get(incrementGroup));
        return null;
    }
    @Override
    public UsedChanges postUsed(Property property, UsedChanges changes) {
        if(noUpdate.contains(property) && changes.hasChanges())
            return new UsedChanges(property);
        else
            return changes;
    }

    public UsedChanges newChanges() {
        return UsedChanges.EMPTY;
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
    public void read(PropertyGroup<T> propertyGroup, @ParamMessage Collection<Property> properties, BaseClass baseClass) throws SQLException {
        // создаем таблицу
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
        changeTable.writeRows(session.sql, changesQuery, baseClass, session.env);

        for (Property property : properties)
            incrementGroups.put(property, propertyGroup);
        tables.put(propertyGroup, changeTable);
    }

    public void cleanIncrementTables() throws SQLException {
        for (SessionTableUsage<T, Property> addTable : tables.values()) {
            addTable.drop(session.sql);
        }
        tables = new HashMap<PropertyGroup<T>, SessionTableUsage<T,Property>>();
        incrementGroups = new HashMap<Property, PropertyGroup<T>>();
    }

}
