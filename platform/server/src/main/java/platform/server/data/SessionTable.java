package platform.server.data;

import platform.base.*;
import platform.interop.Compare;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.Expr;
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

public class SessionTable extends Table implements SessionData<SessionTable>, Value {// в явную хранимые ряды

    public SessionTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Object owner) throws SQLException {
        this(session, keys, properties, classes, propertyClasses, new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(), owner);
    }

    public SessionTable(SQLSession session, Object owner) throws SQLException { // создает пустую таблицу с одной записью
        this(session, new ArrayList<KeyField>(), new HashSet<PropertyField>(),
                Collections.<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>singletonMap(new HashMap<KeyField, DataObject>(), new HashMap<PropertyField, ObjectValue>()), owner);
    }

    public SessionTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, Object owner) throws SQLException {
        this(session, keys, properties, getFieldsClassWheres(rows), rows, owner);
    }

    private SessionTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> filedsWheres, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, Object owner) throws SQLException {
        this(session, keys, properties, filedsWheres.first, filedsWheres.second, rows, owner);
    }

    public SessionTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, Object owner) throws SQLException {
        super(session.createTemporaryTable(keys, properties, owner), keys, properties, classes, propertyClasses);
        for (Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> row : rows.entrySet()) {
            session.insertRecord(this, row.getKey(), row.getValue());
        }
    }

    public SessionTable(String name, List<KeyField> keys, Set<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(name, keys, properties, classes, propertyClasses);
    }

    public List<KeyField> getKeys() {
        return keys;
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

    @Override
    public int hashOuter(HashContext hashContext) {
        return hashValues(hashContext.values);
    }

    @Override
    public Table translateOuter(MapTranslate translator) {
        return translate(translator.mapValues());
    }

    @Override
    public void enumInnerValues(Set<Value> values) {
        values.add(this);
    }

    @IdentityLazy
    public int hashValues(HashValues hashValues) {
        return hashValues.hash(this);
    }

    public Set<Value> getValues() {
        return Collections.singleton((Value) this);
    }

    public SessionTable translate(MapValuesTranslate mapValues) {
        return mapValues.translate(this);
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

        private Struct(List<KeyField> keys, Collection<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses) {
            this.keys = keys;
            this.properties = properties;
            this.classes = classes;
            this.propertyClasses = propertyClasses;
        }

        public boolean twins(TwinImmutableInterface o) {
            return classes.equals(((Struct) o).classes) && keys.equals(((Struct) o).keys) && properties.equals(((Struct) o).properties) && propertyClasses.equals(((Struct) o).propertyClasses);
        }

        public int immutableHashCode() {
            return 31 * (31 * (31 * keys.hashCode() + properties.hashCode()) + classes.hashCode()) + propertyClasses.hashCode();
        }
    }

    private Struct struct = null;

    @ManualLazy
    public GlobalObject getValueClass() {
        if (struct == null) {
            struct = new Struct(keys, properties, classes, propertyClasses);
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
            andPropertyClasses.put(propertyClass.getKey(), propertyClass.getValue().and(BaseUtils.<ClassWhere<KeyField>,ClassWhere<Field>>immutableCast(addKeyClasses)));
        for(Map.Entry<PropertyField, ObjectValue> addProp : propFields.entrySet()) // добавляем новые
            andPropertyClasses.put(addProp.getKey(), !(addProp.getValue() instanceof DataObject)?ClassWhere.<Field>STATIC(false):
                        new ClassWhere<Field>(Collections.<Field, ConcreteClass>singletonMap(addProp.getKey(), ((DataObject) addProp.getValue()).objectClass)).
                                and(BaseUtils.<ClassWhere<KeyField>,ClassWhere<Field>>immutableCast(andKeyClasses)));
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

    public SessionTable insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, Object owner) throws SQLException {

        Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orClasses = orFieldsClassWheres(classes, propertyClasses, keyFields, propFields);

        if (update) {
            session.updateInsertRecord(this, keyFields, propFields);
        } else {
            session.insertRecord(this, keyFields, propFields);
        }

        return new SessionTable(name, keys, properties, orClasses.first, orClasses.second);
    }

    // "обновляет" ключи в таблице
    public SessionData rewrite(SQLSession session, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException {
        return SessionRows.rewrite(this, session, writeRows, owner);
    }

    public SessionData rewrite(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        return SessionRows.rewrite(this, session, query, baseClass, env, owner);
    }

    public SessionTable deleteRecords(SQLSession session, Map<KeyField, DataObject> keys) throws SQLException {
        session.deleteKeyRecords(this, DataObject.getMapValues(keys));
        return this;
    }

    public SessionTable deleteAllRecords(SQLSession session) throws SQLException {
        session.deleteAllRecords(this);
        return this;
    }

    public SessionTable deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException {
        session.deleteKeyRecords(this, Collections.singletonMap(mapField, object.object));
        return this;
    }

    public SessionTable deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException {
        Query<KeyField, PropertyField> dropValues = new Query<KeyField, PropertyField>(this);
        platform.server.data.query.Join<PropertyField> dataJoin = joinAnd(dropValues.mapKeys);
        dropValues.and(dataJoin.getExpr(property).compare(object, Compare.EQUALS));
        dropValues.properties.put(property, Expr.NULL);
        session.updateRecords(new ModifyQuery(this, dropValues));
        return this;
    }

    public SessionTable addFields(SQLSession session, List<KeyField> keys, Map<KeyField, DataObject> addKeys, Map<PropertyField, ObjectValue> addProps) throws SQLException {
        if(addKeys.isEmpty() && addProps.isEmpty())
            return this;

        Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> andClasses = andFieldsClassWheres(classes, propertyClasses, addKeys, addProps);
        SessionTable newTable = new SessionTable(name, keys, BaseUtils.mergeSet(properties, addProps.keySet()), andClasses.first, andClasses.second);

        session.addKeyColumns(name, DataObject.getMapValues(addKeys), keys);
        for(Map.Entry<PropertyField, ObjectValue> addProp : addProps.entrySet())
            session.addColumn(getName(session.syntax), addProp.getKey());

        if(!addProps.isEmpty()) { // для assertion'а
            Query<KeyField, PropertyField> updateProps = new Query<KeyField, PropertyField>(newTable);
            updateProps.and(newTable.join(updateProps.mapKeys).getWhere());
            updateProps.properties.putAll(DataObject.getMapExprs(addProps));
            session.updateRecords(new ModifyQuery(newTable, updateProps));
        }

        return newTable;
    }

    private BaseUtils.HashComponents<Value> components = null;

    @ManualLazy
    public BaseUtils.HashComponents<Value> getComponents() {
        if (components == null) {
            components = AbstractMapValues.getComponents(this);
        }
        return components;
    }

    public void drop(SQLSession session, Object owner) throws SQLException {
        session.dropTemporaryTable(this, owner);
    }
}
