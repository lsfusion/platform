package platform.server.session;

import org.apache.poi.ss.formula.Formula;
import platform.base.BaseUtils;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValues;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.FormulaProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;

// вообщем то public потому как иначе aspect не ловит
public class IncrementApply extends AbstractIncrementProps<KeyField, IncrementApply.UsedChanges> {

    public static class UsedChanges extends AbstractIncrementProps.UsedChanges<UsedChanges> {

        private final Map<Property, MapValues> previous;
        private UsedChanges() {
            previous = new HashMap<Property, MapValues>();
        }
        protected final static UsedChanges EMPTY = new UsedChanges();

        public UsedChanges(IncrementApply modifier) {
            super(modifier);
            previous = new HashMap<Property, MapValues>();
        }
        public UsedChanges(IncrementApply modifier, boolean applyStart) {
            assert applyStart;
            previous = new HashMap<Property, MapValues>();
            for(Map.Entry<Property, SinglePropertyTableUsage<? extends PropertyInterface>> prevTable : modifier.previous.entrySet())
                previous.put(prevTable.getKey(), prevTable.getValue().getUsage());
        }

        @Override
        public boolean modifyUsed() {
            return super.modifyUsed() || !previous.isEmpty();
        }

        @Override
        public boolean hasChanges() {
            return super.hasChanges() || !previous.isEmpty();
        }

        @Override
        protected boolean modifyEquals(UsedChanges changes) {
            return super.modifyEquals(changes) && previous.equals(changes.previous);
        }

        @Override
        @HashLazy
        public int hashValues(HashValues hashValues) {
            return 31 * super.hashValues(hashValues) + MapValuesIterable.hash(previous, hashValues);
        }

        @Override
        @IdentityLazy
        public Set<Value> getValues() {
            Set<Value> result = new HashSet<Value>();
            result.addAll(super.getValues());
            MapValuesIterable.enumValues(result, previous);
            return result;
        }

        public UsedChanges(Property property, SessionTableUsage<?, Property> incrementTable) {
            super(property, incrementTable);
            previous = new HashMap<Property, MapValues>();
        }

        public UsedChanges(Property property) {
            super(property);
            previous = new HashMap<Property, MapValues>();
        }

        public UsedChanges(SinglePropertyTableUsage<? extends PropertyInterface> previousTable, Property property) {
            previous = Collections.singletonMap(property, previousTable.getUsage());
        }

        private UsedChanges(UsedChanges changes, Changes merge) {
            super(changes, merge);
            previous = changes.previous;
        }
        protected UsedChanges calculateAddChanges(Changes changes) {
            return new UsedChanges(this, changes);
        }

        public UsedChanges(UsedChanges changes, UsedChanges merge) {
            super(changes, merge);
            previous = BaseUtils.merge(changes.previous, merge.previous);
        }
        protected UsedChanges calculateAdd(UsedChanges changes) {
            return new UsedChanges(this, changes);
        }

        private UsedChanges(UsedChanges usedChanges, MapValuesTranslate mapValues) {
            super(usedChanges, mapValues);
            previous = mapValues.translateValues(usedChanges.previous);
        }
        public UsedChanges translate(MapValuesTranslate mapValues) {
            return new UsedChanges(this, mapValues);
        }
    }

    protected UsedChanges newUsedChanges(Property property, SessionTableUsage<KeyField, Property> incrementTable) {
        return new UsedChanges(property, incrementTable);
    }

    protected UsedChanges newUsedChanges(Property property) {
        return new UsedChanges(property);
    }

    public UsedChanges newChanges() {
        return UsedChanges.EMPTY;
    }

    protected UsedChanges newFullChanges() {
        return new UsedChanges(this);
    }

    public final DataSession session;
    public IncrementApply(DataSession session) {
        this.session = session;
    }

    public ExprChanges getSession() {
        return session;
    }

    public SQLSession getSql() {
        return session.sql;
    }

    public QueryEnvironment getEnv() {
        return session.env;
    }

    // для singleApply нужен
    public Map<Property, SinglePropertyTableUsage<? extends PropertyInterface>> previous = new HashMap<Property, SinglePropertyTableUsage<? extends PropertyInterface>>();
    public <P extends PropertyInterface> void readApplyStart(Property<P> property, Modifier<? extends Changes> modifier, SessionTableUsage<KeyField, Property> tableChange) throws SQLException {
        SinglePropertyTableUsage<P> prevTable = (SinglePropertyTableUsage<P>) previous.get(property);
        if(prevTable==null) {
            prevTable = property.createChangeTable();
            previous.put(property, prevTable);
        }

        Map<P, KeyExpr> mapKeys = property.getMapKeys();

        WhereBuilder changedWhere = new WhereBuilder();
        if(tableChange!=null)
            changedWhere.add(tableChange.join(BaseUtils.crossJoin(property.mapTable.mapKeys, mapKeys)).getWhere());
        else
            property.getIncrementExpr(mapKeys, modifier, changedWhere);

        prevTable.addRows(session.sql, mapKeys, property.getExpr(mapKeys), changedWhere.toWhere(), session.baseClass, session.env); // если он уже был в базе он не заместится
    }

    public void cleanIncrementTables() throws SQLException {
        for (SinglePropertyTableUsage<? extends PropertyInterface> prevTable : previous.values()) {
            prevTable.drop(getSql());
        }
        previous = new HashMap<Property, SinglePropertyTableUsage<? extends PropertyInterface>>();
        super.cleanIncrementTables();
    }

    private final Modifier<UsedChanges> applyStart = new Modifier<UsedChanges>() {

        public UsedChanges newChanges() {
            return UsedChanges.EMPTY;
        }

        protected UsedChanges newFullChanges() {
            return new UsedChanges(IncrementApply.this, true);
        }

        public ExprChanges getSession() {
            return ExprChanges.EMPTY;
        }

        public UsedChanges preUsed(Property property) {
            if(property instanceof FormulaProperty)
                return null;

            SinglePropertyTableUsage<? extends PropertyInterface> prevTable = previous.get(property);
            if(prevTable!=null)
                return new UsedChanges(prevTable, property);
            return UsedChanges.EMPTY;
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            if(property instanceof FormulaProperty)
                return null;

            SinglePropertyTableUsage<P> prevTable = (SinglePropertyTableUsage<P>) previous.get(property);
            if(prevTable==null) // если уже все посчитано - просто возвращаем его
                prevTable = property.createChangeTable();
            return prevTable.getExpr(joinImplement, changedWhere);
        }

        public boolean neededClass(Changes changes) {
            return changes instanceof UsedChanges;
        }
    };

    public Modifier<UsedChanges> getApplyStart() {
        return applyStart; // возвращаем modifier который изменяет previous "назад" на старые значения базы
    }

    @Override
    public UsedChanges getApplyUsedChanges(Set<Property> props) {
        return getUsedChanges(props).add(getApplyStart().getUsedChanges(props));
    }

    // assert что в properties содержатся
    public SessionTableUsage<KeyField, Property> read(final ImplementTable implement, Collection<Property> properties, BaseClass baseClass) throws SQLException {
        if (properties.size() == 1) {
            SessionTableUsage<KeyField, Property> changeTable = getTable(BaseUtils.single(properties));
            if (changeTable != null)
                return changeTable;
        }

        // если слишком много групп, разделим на несколько read'ов
        final int split = Settings.instance.getSplitIncrementApply();
        if(properties.size() > split) {
            // вообще тут пока используется дополнительный assertion, но пока это не так важно
            final List<Property> propertyList = new ArrayList<Property>(properties);
            for(Collection<Property> groupProps : BaseUtils.<Integer, Property>group(new BaseUtils.Group<Integer, Property>() {
                    public Integer group(Property key) {
                        return propertyList.indexOf(key) / split;
                    }
                }, propertyList).values())
                read(implement, groupProps, baseClass);
        }

        PropertyGroup<KeyField> groupTable = new PropertyGroup<KeyField>() {
            public List<KeyField> getKeys() {
                return implement.keys;
            }

            public Type.Getter<KeyField> typeGetter() {
                return Field.typeGetter();
            }

            public <P extends PropertyInterface> Map<P, KeyField> getPropertyMap(Property<P> property) {
                return property.mapTable.mapKeys;
            }
        };
        return read(groupTable, properties, baseClass);
    }

    public Map<ImplementTable, Collection<Property>> groupPropertiesByTables() {
       return BaseUtils.group(
                new BaseUtils.Group<ImplementTable, Property>() {
                    public ImplementTable group(Property key) {
                        return key.mapTable.table;
                    }
                }, incrementGroups.keySet());
    }
}
