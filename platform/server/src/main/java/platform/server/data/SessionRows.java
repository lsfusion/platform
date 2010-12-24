package platform.server.data;

import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.query.Query;
import platform.server.data.query.Join;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.caches.IdentityLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.ManualLazy;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.hash.HashValues;
import platform.server.caches.hash.HashCodeValues;
import platform.server.classes.ConcreteClass;
import platform.server.classes.BaseClass;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Pair;

import java.util.*;
import java.sql.SQLException;

public class SessionRows implements SessionData<SessionRows> {

    private List<KeyField> keys;
    private Set<PropertyField> properties;

    public List<KeyField> getKeys() {
        return keys;
    }

    public Set<PropertyField> getProperties() {
        return properties;
    }

    public Map<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(keys);
    }

    protected ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
    protected Map<PropertyField,ClassWhere<Field>> propertyClasses;

    protected final Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> rows;

    public SessionRows(List<KeyField> keys) {
        this(keys, new HashSet<PropertyField>());
    }

    public SessionRows(List<KeyField> keys, Set<PropertyField> properties) {
        this(keys, properties, ClassWhere.<KeyField>STATIC(false), ClassWhere.<PropertyField, Field>STATIC(properties, false), new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>());
    }

    public SessionRows(List<KeyField> keys, Set<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        this.keys = keys;
        this.properties = properties;
        this.classes = classes;
        this.propertyClasses = propertyClasses;
        this.rows = rows;
    }

    public Join<PropertyField> join(final Map<KeyField, ? extends Expr> joinImplement) {
        return new platform.server.data.query.Join<PropertyField>() {

            public Expr getExpr(PropertyField property) {
                ExprCaseList result = new ExprCaseList();
                for(Map.Entry<Map<KeyField, DataObject>,Map<PropertyField, ObjectValue>> row : rows.entrySet())
                    result.add(CompareWhere.compareValues(joinImplement,row.getKey()),row.getValue().get(property).getExpr());
                return result.getExpr();
            }

            public Where getWhere() {
                Where result = Where.FALSE;
                for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> row : rows.entrySet())
                    result = result.or(CompareWhere.compareValues(joinImplement,row.getKey()));
                return result;
            }

            public Collection<PropertyField> getProperties() {
                return properties;
            }
        };
    }

    @IdentityLazy
    public int hashValues(HashValues hashValues) {
        int hash = 0;
        for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> row : rows.entrySet())
            hash += MapValuesIterable.hash(row.getKey(),hashValues) ^ MapValuesIterable.hash(row.getValue(),hashValues);
        return hash;
    }

    public Set<Value> getValues() {
        Set<Value> result = new HashSet<Value>();
        for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> row : rows.entrySet()) {
            MapValuesIterable.enumValues(result,row.getKey());
            MapValuesIterable.enumValues(result,row.getValue());
        }
        return result;
    }

    public SessionRows translate(MapValuesTranslate mapValues) {
        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> transRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>();
        for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> row : rows.entrySet())
            transRows.put(mapValues.translateValues(row.getKey()), mapValues.translateValues(row.getValue()));
        return new SessionRows(keys, properties, classes, propertyClasses, transRows);
    }

    @Override
    public boolean equals(Object obj) {
        return obj==this || obj instanceof SessionRows && keys.equals(((SessionRows)obj).keys) && properties.equals(((SessionRows)obj).properties) && rows.equals(((SessionRows)obj).rows);
    }

    boolean hashCoded = false;
    int hashCode;

    @Override
    public int hashCode() { // можно было бы взять из AbstractMapValues но без мн-го наследования
        if(!hashCoded) {
            hashCode = hashValues(HashCodeValues.instance);
            hashCoded = true;
        }
        return hashCode;
    }

    public final static int MAX_ROWS = 1;

    public static Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> insertRecord(ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields) {
        Map<KeyField, ConcreteClass> insertKeyClasses = DataObject.getMapClasses(keyFields);
        Map<PropertyField, ClassWhere<Field>> orPropertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(Map.Entry<PropertyField,ObjectValue> propertyField : propFields.entrySet()) {
            ClassWhere<Field> existedPropertyClasses = propertyClasses.get(propertyField.getKey());
            assert existedPropertyClasses!=null;
            if(propertyField.getValue() instanceof DataObject)
                orPropertyClasses.put(propertyField.getKey(), existedPropertyClasses.or(new ClassWhere<Field>(BaseUtils.merge(insertKeyClasses,
                                Collections.singletonMap(propertyField.getKey(),((DataObject)propertyField.getValue()).objectClass)))));
            else
                orPropertyClasses.put(propertyField.getKey(), existedPropertyClasses);
        }
        return new Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>>(
                classes.or(new ClassWhere<KeyField>(insertKeyClasses)), orPropertyClasses);
    }
    
    public SessionData insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, Object owner) throws SQLException {

        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> orRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        Map<PropertyField, ObjectValue> prevValue = orRows.put(keyFields,propFields);
        assert update || prevValue==null;

        Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orClasses = insertRecord(classes, propertyClasses, keyFields, propFields);
        
        if(orRows.size()>MAX_ROWS) // если превысили количество рядов "переходим" в таблицу
            return new SessionTable(session, keys, properties, orClasses.first, orClasses.second, orRows, owner);
        else
            return new SessionRows(keys, properties, orClasses.first, orClasses.second, orRows);
    }

    public static SessionData write(SQLSession session, List<KeyField> keys, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException {
        ClassWhere<KeyField> writeClasses = new ClassWhere<KeyField>();
        for(Map<KeyField, DataObject> row : writeRows)
            writeClasses = writeClasses.or(new ClassWhere<KeyField>(DataObject.getMapClasses(row)));

        Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> propRows = BaseUtils.toMap(writeRows, (Map<PropertyField, ObjectValue>) new HashMap<PropertyField, ObjectValue>());
        if(writeRows.size()>MAX_ROWS)
            return new SessionTable(session, keys, new HashSet<PropertyField>(), writeClasses, new HashMap<PropertyField, ClassWhere<Field>>(), propRows, owner);
        else
            return new SessionRows(keys, new HashSet<PropertyField>(), writeClasses, new HashMap<PropertyField, ClassWhere<Field>>(), propRows);

    }

    public static SessionData rewrite(SessionData<?> data, SQLSession session, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException {
        assert data.getProperties().isEmpty();
        data.drop(session, owner);
        return write(session, data.getKeys(), writeRows, owner);
    }

    // "обновляет" ключи в таблице
    public SessionData rewrite(SQLSession session, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException {
        return rewrite(this, session, writeRows, owner);
    }

    public void drop(SQLSession session, Object owner) {
    }

    public static SessionData write(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {

        // читаем классы не считывая данные
        Map<PropertyField,ClassWhere<Field>> insertClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(PropertyField field : query.properties.keySet())
            insertClasses.put(field,query.<Field>getClassWhere(Collections.singleton(field)));

        SessionTable table = new SessionTable(session, keys, properties, query.<KeyField>getClassWhere(new ArrayList<PropertyField>()), insertClasses, new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(), owner);
        // нужно прочитать то что записано
        if(session.insertSelect(new ModifyQuery(table,query,env)) > MAX_ROWS)
            return table;
        else {
            OrderedMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> readRows = table.read(session, baseClass);

            table.drop(session, owner); // выкидываем таблицу

            // надо бы batch update сделать, то есть зная уже сколько запискй
            SessionData sessionRows = new SessionRows(keys, properties);
            for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> writeRow : readRows.entrySet())
                sessionRows = sessionRows.insertRecord(session, writeRow.getKey(), writeRow.getValue(), false, owner);
            return sessionRows;
        }
    }

    public static SessionData rewrite(SessionData<?> data, SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        data.drop(session, owner);
        return write(session, data.getKeys(), data.getProperties(), query, baseClass, env, owner);
    }

    public SessionData rewrite(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        return rewrite(this, session, query, baseClass, env, owner);
    }

    public SessionData deleteRecords(SQLSession session, Map<KeyField,DataObject> keys) throws SQLException {
        return new SessionRows(this.keys, properties, classes, propertyClasses, BaseUtils.removeKey(rows, keys));
    }

    public SessionData deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException {
        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> removeRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        Iterator<Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>>> iterator = removeRows.entrySet().iterator();
        while(iterator.hasNext())
            if(iterator.next().getKey().get(mapField).equals(object))
                iterator.remove();
        return new SessionRows(keys, properties, classes, propertyClasses, removeRows);
    }

    public SessionData deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException {
        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> removeRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        Iterator<Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>>> iterator = removeRows.entrySet().iterator();
        while(iterator.hasNext())
            if(iterator.next().getValue().get(property).equals(object))
                iterator.remove();
        return new SessionRows(keys, properties, classes, propertyClasses, removeRows);
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getComponents() {
        if(components==null)
            components = AbstractMapValues.getComponents(this);
        return components;
    }

}
