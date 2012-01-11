package platform.server.data;

import platform.base.*;
import platform.interop.Compare;
import platform.server.caches.AbstractInnerContext;
import platform.server.caches.AbstractValuesContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.ValuesContext;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.CompileSource;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.ParseInterface;
import platform.server.data.type.StringParseInterface;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

import static java.util.Collections.singletonMap;
import static platform.base.BaseUtils.merge;

public class SessionTable extends Table implements ValuesContext<SessionTable>, Value {// в явную хранимые ряды

    public final int count;

    public StatKeys<KeyField> getStatKeys() {
        return getStatKeys(this, count);
    }

    public Value removeBig(QuickSet<Value> usedValues) {
        return null;
    }

    public Map<PropertyField, Stat> getStatProps() {
        return getStatProps(this, count);
    }

    // просто дебилизм, но с ограничениями конструктора по другому не сделаешь
    private SessionTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Integer count, FillTemporaryTable fill, Result<Integer> actual, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Object owner) throws SQLException {
        super(session.getTemporaryTable(keys, properties, fill, count, actual, owner), keys, properties, classes, propertyClasses);

        this.count = actual.result;
    }
    public SessionTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Integer count, FillTemporaryTable fill, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Object owner) throws SQLException {
        this(session, keys, properties, count, fill, new Result<Integer>(), classes, propertyClasses, owner);
    }

    // создает таблицу batch'ем
    public static SessionTable create(final SQLSession session, final List<KeyField> keys, Set<PropertyField> properties, final Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, boolean groupLast, Object owner) throws SQLException {
        // прочитаем классы
        Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orClasses = SessionRows.getClasses(properties, rows);
        return new SessionTable(session, keys, properties, rows.size(), new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                session.insertBatchRecords(name, keys, rows);
                return null;
            }
        }, orClasses.first, orClasses.second, owner);
    }

    public SessionTable(String name, List<KeyField> keys, Set<PropertyField> properties, Integer count, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(name, keys, properties, classes, propertyClasses);

        this.count = count;
    }

    public Set<PropertyField> getProperties() {
        return properties;
    }

    @Override
    public String getName(SQLSyntax syntax) {
        return syntax.getSessionTableName(name);
    }

    @Override
    public String getQueryName(CompileSource source) {
        assert source.params.containsKey(this);
        return source.params.get(this);
    }

    protected Table translate(MapTranslate translator) {
        return translateValues(translator.mapValues());
    }

    protected int hash(HashContext hashContext) {
        return hashValues(hashContext.values);
    }

    public QuickSet<Value> getValues() {
        return getContextValues();
    }

    public SessionTable translateValues(MapValuesTranslate mapValues) {
        return mapValues.translate(this);
    }

    public int hashValues(HashValues hashValues) {
        return hashValues.hash(this);
    }

    public QuickSet<Value> getContextValues() {
        return new QuickSet<Value>(this);
    }

    public ParseInterface getParseInterface() {
        return new StringParseInterface() {
            public String getString(SQLSyntax syntax) {
                return getName(syntax);
            }
        };
    }

    // теоретически достаточно только
    private static class Struct extends TwinImmutableObject implements GlobalObject {

        public final List<KeyField> keys; // List потому как в таком порядке индексы будут строиться
        public final Collection<PropertyField> properties;
        protected final ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
        protected final Map<PropertyField, ClassWhere<Field>> propertyClasses;

        protected final StatKeys<KeyField> statKeys;
        protected final Map<PropertyField, Stat> statProps;

        private Struct(List<KeyField> keys, Collection<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, StatKeys<KeyField> statKeys, Map<PropertyField, Stat> statProps) {
            this.keys = keys;
            this.properties = properties;
            this.classes = classes;
            this.propertyClasses = propertyClasses;

            this.statKeys = statKeys;
            this.statProps = statProps;
        }

        public boolean twins(TwinImmutableInterface o) {
            return classes.equals(((Struct) o).classes) && keys.equals(((Struct) o).keys) && properties.equals(((Struct) o).properties) && propertyClasses.equals(((Struct) o).propertyClasses) && statKeys.equals(((Struct)o).statKeys) && statProps.equals(((Struct)o).statProps);
        }

        public int immutableHashCode() {
            return 31 * (31 * (31 * (31 * (31 * keys.hashCode() + properties.hashCode()) + classes.hashCode()) + propertyClasses.hashCode()) + statKeys.hashCode()) + statProps.hashCode();
        }
    }

    private Struct struct = null;

    @ManualLazy
    public GlobalObject getValueClass() {
        if (struct == null) {
            struct = new Struct(keys, properties, classes, propertyClasses, getStatKeys(), getStatProps());
        }
        return struct;
    }

    public boolean twins(TwinImmutableInterface o) {
        return name.equals(((SessionTable) o).name) && getValueClass().equals(((SessionTable) o).getValueClass());
    }

    public int immutableHashCode() {
        return name.hashCode() * 31 + getValueClass().hashCode();
    }

    public static Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) {

        Map<KeyField, ConcreteClass> insertKeyClasses = DataObject.getMapClasses(keyFields);
        Map<PropertyField, ClassWhere<Field>> orPropertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();

        for (Map.Entry<PropertyField, ObjectValue> propertyField : propFields.entrySet()) {
            PropertyField propField = propertyField.getKey();
            ObjectValue propValue = propertyField.getValue();

            ClassWhere<Field> existedPropertyClasses = propertyClasses.get(propField);
            assert existedPropertyClasses != null;

            if (propValue instanceof DataObject) {
                orPropertyClasses.put(
                        propField,
                        existedPropertyClasses.or(
                                new ClassWhere<Field>(merge(insertKeyClasses, singletonMap(propField, ((DataObject) propValue).objectClass)))
                        )
                );
            } else {
                orPropertyClasses.put(propField, existedPropertyClasses);
            }
        }
        return new Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>>(
                classes.or(new ClassWhere<KeyField>(insertKeyClasses)), orPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> andFieldsClassWheres(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) {
        // определяем новые классы чтобы создать таблицу
        ClassWhere<KeyField> addKeyClasses = new ClassWhere<KeyField>(DataObject.getMapClasses(keyFields));

        ClassWhere<KeyField> andKeyClasses = classes.and(addKeyClasses);
        Map<PropertyField, ClassWhere<Field>> andPropertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(Map.Entry<PropertyField, ClassWhere<Field>> propertyClass : propertyClasses.entrySet()) // добавляем старые
            andPropertyClasses.put(propertyClass.getKey(), propertyClass.getValue().and(BaseUtils.<ClassWhere<Field>>immutableCast(addKeyClasses)));
        for(Map.Entry<PropertyField, ObjectValue> addProp : propFields.entrySet()) // добавляем новые
            andPropertyClasses.put(addProp.getKey(), !(addProp.getValue() instanceof DataObject)?ClassWhere.<Field>STATIC(false):
                        new ClassWhere<Field>(Collections.<Field, ConcreteClass>singletonMap(addProp.getKey(), ((DataObject) addProp.getValue()).objectClass)).
                                and(BaseUtils.<ClassWhere<Field>>immutableCast(andKeyClasses)));
        return new Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>>(andKeyClasses, andPropertyClasses);
    }

    public static Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> getFieldsClassWheres(Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> data) {
        ClassWhere<KeyField> keysClassWhere = new ClassWhere<KeyField>();
        Map<PropertyField, ClassWhere<Field>> propertiesClassWheres = new HashMap<PropertyField, ClassWhere<Field>>();
        for (Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> row : data.entrySet()) {
            Map<KeyField, DataObject> rowKeys = row.getKey();
            Map<PropertyField, ObjectValue> rowProps = row.getValue();

            Map<KeyField, ConcreteClass> rowKeyClasses = DataObject.getMapClasses(rowKeys);

            keysClassWhere = keysClassWhere.or(new ClassWhere(rowKeyClasses));

            for (Map.Entry<PropertyField, ObjectValue> entry : rowProps.entrySet()) {
                PropertyField propField = entry.getKey();
                ObjectValue propValue = entry.getValue();

                ClassWhere<Field> propClassWhere = propertiesClassWheres.containsKey(propField) ? propertiesClassWheres.get(propField) : new ClassWhere<Field>();
                if (propValue instanceof DataObject) {
                    propClassWhere = propClassWhere.or(
                            new ClassWhere<Field>(
                                    merge(rowKeyClasses, singletonMap(propField, ((DataObject) propValue).objectClass))
                            )
                    );
                }

                propertiesClassWheres.put(propField, propClassWhere);
            }
        }
        return new Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>>(keysClassWhere, propertiesClassWheres);
    }

    public SessionTable insertRecord(final SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, boolean groupLast, final Object owner) throws SQLException {

        Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orClasses = orFieldsClassWheres(classes, propertyClasses, keyFields, propFields);

        update = update && session.isRecord(this, keyFields);
        int newCount = count + (update?0:1);

        SessionTable result;
        if(!SQLTemporaryPool.getDBStatistics(newCount).equals(SQLTemporaryPool.getDBStatistics(count)))
            result = new SessionTable(session, keys, properties, newCount, new FillTemporaryTable() {
                public Integer fill(String name) throws SQLException {
                    Query<KeyField, PropertyField> moveData = new Query<KeyField, PropertyField>(keys);
                    platform.server.data.query.Join<PropertyField> prevJoin = join(BaseUtils.filterKeys(moveData.mapKeys, SessionTable.this.keys));
                    moveData.and(prevJoin.getWhere());
                    moveData.properties.putAll(prevJoin.getExprs());
                    session.insertSessionSelect(name, moveData, QueryEnvironment.empty);
                    session.returnTemporaryTable(SessionTable.this, owner);
                    return null;
                }
            }, orClasses.first, orClasses.second, owner);
        else
            result = new SessionTable(name, keys, properties, newCount, orClasses.first, orClasses.second);

        if(update)
            session.updateRecords(result, keyFields, propFields);
        else
            session.insertRecord(result, keyFields, propFields);
        return result;
    }

    public void deleteRecords(SQLSession session, Map<KeyField, DataObject> keys) throws SQLException {
        session.deleteKeyRecords(this, DataObject.getMapValues(keys));
    }

    public void deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException {
        session.deleteKeyRecords(this, Collections.singletonMap(mapField, object.object));
    }

    public void deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException {
        Query<KeyField, PropertyField> dropValues = new Query<KeyField, PropertyField>(this);
        platform.server.data.query.Join<PropertyField> dataJoin = joinAnd(dropValues.mapKeys);
        dropValues.and(dataJoin.getExpr(property).compare(object, Compare.EQUALS));
        dropValues.properties.put(property, Expr.NULL);
        session.updateRecords(new ModifyQuery(this, dropValues));
    }

    public SessionTable addFields(final SQLSession session, final List<KeyField> keys, final Map<KeyField, DataObject> addKeys, final Map<PropertyField, ObjectValue> addProps, final Object owner) throws SQLException {
        if(addKeys.isEmpty() && addProps.isEmpty())
            return this;

        Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> andClasses = andFieldsClassWheres(classes, propertyClasses, addKeys, addProps);
        return new SessionTable(session, keys, BaseUtils.mergeSet(properties, addProps.keySet()), count, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                // записать в эту таблицу insertSessionSelect из текущей + default поля
                Query<KeyField, PropertyField> moveData = new Query<KeyField, PropertyField>(keys);
                platform.server.data.query.Join<PropertyField> prevJoin = join(BaseUtils.filterKeys(moveData.mapKeys, SessionTable.this.keys));
                moveData.and(prevJoin.getWhere());
                moveData.putKeyWhere(addKeys);
                moveData.properties.putAll(prevJoin.getExprs());
                moveData.properties.putAll(DataObject.getMapExprs(addProps));
                session.insertSessionSelect(name, moveData, QueryEnvironment.empty);
                session.returnTemporaryTable(SessionTable.this, owner);
                return null;
            }
        }, andClasses.first, andClasses.second, owner);
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getValueComponents() {
        if (components == null)
            components = AbstractValuesContext.getComponents(this);
        return components;
    }
    public int hashValues() {
        return AbstractValuesContext.hash(this);
    }
    public QuickMap<Value, GlobalObject> getValuesMap() {
        return AbstractValuesContext.getMap(this);
    }
    public Map<Value, Value> getBigValues() {
        return AbstractInnerContext.getBigValues(getContextValues());
    }

    public void drop(SQLSession session, Object owner) throws SQLException {
        session.returnTemporaryTable(this, owner);
    }
}
