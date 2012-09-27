package platform.server.data;

import platform.base.*;
import platform.server.caches.AbstractValuesContext;
import platform.server.caches.ManualLazy;
import platform.server.caches.ValuesContext;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteClass;
import platform.server.classes.ConcreteObjectClass;
import platform.server.classes.DataClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.FormulaExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.Join;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.CompileSource;
import platform.server.data.query.Query;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.type.ObjectType;
import platform.server.data.type.ParseInterface;
import platform.server.data.type.StringParseInterface;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ObjectClassProperty;
import platform.server.session.DataSession;
import platform.server.session.SessionModifier;

import java.sql.SQLException;
import java.util.*;

import static java.util.Collections.singletonMap;
import static platform.base.BaseUtils.hashEquals;
import static platform.base.BaseUtils.merge;
import static platform.base.BaseUtils.singleKey;

public class SessionTable extends Table implements ValuesContext<SessionTable>, Value {// в явную хранимые ряды

    public final int count; // вообще должен быть точным, или как минимум пессимистичным, чтобы в addObjects учитываться

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
    public SessionTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Integer count, FillTemporaryTable fill, Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> queryClasses, Object owner) throws SQLException {
        this(session, keys, properties, count, fill, new Result<Integer>(), queryClasses.first, queryClasses.second, owner);
    }

    // создает таблицу batch'ем
    public static SessionTable create(final SQLSession session, final List<KeyField> keys, Set<PropertyField> properties, final Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, Object owner) throws SQLException {
        // прочитаем классы
        return new SessionTable(session, keys, properties, rows.size(), new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                session.insertBatchRecords(name, keys, rows);
                return null;
            }
        }, SessionRows.getClasses(properties, rows), owner);
    }

    public SessionTable(String name, List<KeyField> keys, Set<PropertyField> properties, Integer count, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(name, keys, properties, classes, propertyClasses);

        this.count = count;
    }
    public SessionTable(String name, List<KeyField> keys, Set<PropertyField> properties, Integer count, Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> tableClasses) {
        this(name, keys, properties, count, tableClasses.first, tableClasses.second);
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

    public SessionTable translateRemoveValues(MapValuesTranslate translate) {
        return translateValues(translate);
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

    public static Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orFieldsClassWheres(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orClasses) {
        Map<PropertyField, ClassWhere<Field>> orPropertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(Map.Entry<PropertyField, ClassWhere<Field>> propertyClass : propertyClasses.entrySet())
            orPropertyClasses.put(propertyClass.getKey(), propertyClass.getValue().or(orClasses.second.get(propertyClass.getKey())));
        return new Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>>(classes.or(orClasses.first), orPropertyClasses);
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

    public static Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> removeFieldsClassWheres(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Set<KeyField> keyFields, Set<PropertyField> propFields) {
        if(keyFields.isEmpty())
            return new Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>>(classes, BaseUtils.filterNotKeys(propertyClasses, propFields));
        else {
            Map<PropertyField, ClassWhere<Field>> removePropClasses = new HashMap<PropertyField, ClassWhere<Field>>();
            for(Map.Entry<PropertyField, ClassWhere<Field>> propClass : propertyClasses.entrySet())
                if(!propFields.contains(propClass.getKey()))
                    removePropClasses.put(propClass.getKey(), propClass.getValue().remove(keyFields));
            return new Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>>(classes.remove(keyFields), removePropClasses);
        }
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

    public SessionTable modifyRecord(final SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, Modify type, final Object owner) throws SQLException {

        if(type==Modify.DELETE) { // статистику пока не учитываем
            return new SessionTable(name, keys, properties, count - deleteRecords(session, keyFields), classes, propertyClasses).
                    updateStatistics(session, count, owner);
        }

        boolean update = (type== Modify.UPDATE);
        if(type== Modify.MODIFY || type== Modify.LEFT) {
            if(session.isRecord(this, keyFields)) {
                if(type== Modify.MODIFY)
                    update = true;
                else
                    return this;
            }
        }

        if(update)
            session.updateRecords(this, keyFields, propFields);
        else
            session.insertRecord(this, keyFields, propFields);

        return new SessionTable(name, keys, properties, count + (update?0:1),
                        orFieldsClassWheres(classes, propertyClasses, keyFields, propFields)).
                            updateStatistics(session, count, owner);
    }

    public SessionTable modifyRows(SQLSession session, Query<KeyField, PropertyField> query, Modify type, QueryEnvironment env, Object owner) throws SQLException {

        if(query.where.isFalse()) // оптимизация
            return this;

        ModifyQuery modify = new ModifyQuery(this, query, env);
        int inserted;
        switch (type) {
            case MODIFY:
                inserted = session.modifyRecords(modify);
                break;
            case LEFT:
                inserted = session.insertLeftSelect(modify, true);
                break;
            case ADD:
                inserted = session.insertSelect(modify);
                break;
            case UPDATE:
                session.updateRecords(modify);
                inserted = 0;
                break;
            case DELETE:
                return new SessionTable(name, keys, properties, count - session.deleteRecords(modify), classes, propertyClasses).
                        updateStatistics(session, count, owner);
            default:
                throw new RuntimeException("should not be");
        }
        return new SessionTable(name, keys, properties, count + inserted,
                        orFieldsClassWheres(classes, propertyClasses, SessionData.getQueryClasses(query))).
                            updateStatistics(session, count, owner);
    }
    public void updateAdded(SQLSession session, BaseClass baseClass, PropertyField field, int count) throws SQLException {
        Query<KeyField, PropertyField> query = new Query<KeyField, PropertyField>(this);
        platform.server.data.query.Join<PropertyField> join = join(query.mapKeys);
        query.properties.put(field, FormulaExpr.create2("prm1+prm2", baseClass.unknown, join.getExpr(field), ObjectType.idClass.getStaticExpr(count)));
        query.and(join.getWhere());
        session.updateRecords(new ModifyQuery(this, query));
    }

    public SessionTable updateCurrentClasses(DataSession session) throws SQLException {
        Map<KeyField, KeyExpr> mapKeys = getMapKeys();
        platform.server.data.query.Join<PropertyField> join = join(mapKeys);

        Map<Field, Expr> mapExprs = new HashMap<Field, Expr>();
        Map<Field, DataClass> mapData = new HashMap<Field, DataClass>();
        ClassWhere<KeyField> updatedClasses = ClassWhere.STATIC(false);
        for(KeyField key : keys)
            if(key.type instanceof ObjectType)
                mapExprs.put(key, mapKeys.get(key));
            else
                mapData.put(key, (DataClass) key.type);
        Map<PropertyField, ClassWhere<Field>> updatedPropertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(PropertyField property : properties) {
            if(property.type instanceof ObjectType)
                mapExprs.put(property, join.getExpr(property));
            else
                mapData.put(property, (DataClass) property.type);
            updatedPropertyClasses.put(property, ClassWhere.<Field>STATIC(false));
        }

        // пока исходим из assertion'а что не null, потом надо будет разные делать
        for(Map<Field, ConcreteObjectClass> diffClasses : session.readDiffClasses(join.getWhere(), new HashMap<Field, Expr>(), mapExprs)) {
            Map<Field, ConcreteClass> result = BaseUtils.merge(diffClasses, mapData);
            updatedClasses = updatedClasses.or(new ClassWhere<KeyField>(BaseUtils.filterKeys(result, mapKeys.keySet())));
            for(Map.Entry<PropertyField, ClassWhere<Field>> propertyClass : updatedPropertyClasses.entrySet())
                propertyClass.setValue(propertyClass.getValue().or(new ClassWhere<Field>(
                        BaseUtils.filterKeys(result, BaseUtils.add(mapKeys.keySet(), propertyClass.getKey())))));
        }
        return new SessionTable(name, keys, properties, count, updatedClasses, updatedPropertyClasses);
    }

    public SessionTable updateStatistics(final SQLSession session, int prevCount, final Object owner) throws SQLException {
        if(!SQLTemporaryPool.getDBStatistics(count).equals(SQLTemporaryPool.getDBStatistics(prevCount))) {
            return new SessionTable(session, keys, properties, count, new FillTemporaryTable() {
                public Integer fill(String name) throws SQLException {
                    Query<KeyField, PropertyField> moveData = new Query<KeyField, PropertyField>(keys);
                    platform.server.data.query.Join<PropertyField> prevJoin = join(BaseUtils.filterKeys(moveData.mapKeys, SessionTable.this.keys));
                    moveData.and(prevJoin.getWhere());
                    moveData.properties.putAll(prevJoin.getExprs());
                    session.insertSessionSelect(name, moveData, QueryEnvironment.empty);
                    session.returnTemporaryTable(SessionTable.this, owner);
                    return null;
                }
            }, classes, propertyClasses, owner);
        }
        return this;
    }

    public int deleteRecords(SQLSession session, Map<KeyField, DataObject> keys) throws SQLException {
        return session.deleteKeyRecords(this, DataObject.getMapValues(keys));
    }

    public SessionTable deleteRows(SQLSession session, Query<KeyField, PropertyField> query, QueryEnvironment env, Object owner) throws SQLException {
        int deleted = session.deleteRecords(new ModifyQuery(this, query, env));

        return new SessionTable(name, keys, properties, count - deleted,
                orFieldsClassWheres(classes, propertyClasses, SessionData.getQueryClasses(query))).
                updateStatistics(session, count, owner);
    }


    public SessionTable addFields(final SQLSession session, final List<KeyField> keys, final Map<KeyField, DataObject> addKeys, final Map<PropertyField, ObjectValue> addProps, final Object owner) throws SQLException {
        if(addKeys.isEmpty() && addProps.isEmpty())
            return this;

        return new SessionTable(session, keys, BaseUtils.mergeSet(properties, addProps.keySet()), count, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                // записать в эту таблицу insertSessionSelect из текущей + default поля
                Query<KeyField, PropertyField> moveData = new Query<KeyField, PropertyField>(keys, addKeys);
                platform.server.data.query.Join<PropertyField> prevJoin = join(BaseUtils.filterKeys(moveData.getMapExprs(), SessionTable.this.keys));
                moveData.and(prevJoin.getWhere());
                moveData.properties.putAll(prevJoin.getExprs());
                moveData.properties.putAll(DataObject.getMapExprs(addProps));
                session.insertSessionSelect(name, moveData, QueryEnvironment.empty);
                session.returnTemporaryTable(SessionTable.this, owner);
                return null;
            }
        }, andFieldsClassWheres(classes, propertyClasses, addKeys, addProps), owner);
    }

    public SessionTable removeFields(final SQLSession session, Set<KeyField> removeKeys, Set<PropertyField> removeProps, final Object owner) throws SQLException {
        if(removeKeys.isEmpty() && removeProps.isEmpty())
            return this;

        // assert что удаляемые ключи с одинаковыми значениями, но вообще может использоваться как слияние
        final List<KeyField> remainKeys = BaseUtils.filterNotList(keys, removeKeys);
        final Set<PropertyField> remainProps = BaseUtils.filterNotSet(properties, removeProps);
        return new SessionTable(session, remainKeys, remainProps, count, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                // записать в эту таблицу insertSessionSelect из текущей + default поля
                Query<KeyField, PropertyField> moveData = new Query<KeyField, PropertyField>(remainKeys);

                if(remainKeys.size()==keys.size()) { // для оптимизации
                    platform.server.data.query.Join<PropertyField> prevJoin = join(moveData.mapKeys);
                    moveData.and(prevJoin.getWhere());
                    moveData.properties.putAll(BaseUtils.filterKeys(prevJoin.getExprs(), remainProps));
                } else {
                    Map<KeyField,KeyExpr> tableKeys = getMapKeys();
                    platform.server.data.query.Join<PropertyField> prevJoin = join(tableKeys);
                    Map<KeyField, KeyExpr> groupKeys = BaseUtils.filterKeys(tableKeys, remainKeys);
                    moveData.and(GroupExpr.create(groupKeys, prevJoin.getWhere(), moveData.mapKeys).getWhere());
                    for(PropertyField prop : remainProps)
                        moveData.properties.put(prop, GroupExpr.create(groupKeys, prevJoin.getExpr(prop), GroupType.ANY, moveData.mapKeys));
                }
                session.insertSessionSelect(name, moveData, QueryEnvironment.empty);
                session.returnTemporaryTable(SessionTable.this, owner);
                return null;
            }
        }, removeFieldsClassWheres(classes, propertyClasses, removeKeys, removeProps), owner);
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getValueComponents() {
        if (components == null)
            components = AbstractValuesContext.getComponents(this);
        return components;
    }

    public void drop(SQLSession session, Object owner) throws SQLException {
        session.returnTemporaryTable(this, owner);
    }
    public void rollDrop(SQLSession session, Object owner) throws SQLException {
        session.rollReturnTemporaryTable(this, owner);
    }

    // см. usage
    public SessionTable fixKeyClasses(ClassWhere<KeyField> fixClasses) {
        assert propertyClasses.isEmpty();
        ClassWhere<KeyField> fixedClasses = classes.and(fixClasses);
        if(hashEquals(fixedClasses, classes))
            return this;
        else
            return new SessionTable(name, keys, properties, count, fixedClasses, propertyClasses);
    }
}
