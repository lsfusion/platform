package platform.server.data;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.MapValuesIterable;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashValues;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.HashLazy;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

public class SessionRows extends ImmutableObject implements SessionData<SessionRows> {

    public final static int MAX_ROWS = 1;

    private List<KeyField> keys;
    private Set<PropertyField> properties;

    protected final Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> rows;

    public SessionRows(List<KeyField> keys, Set<PropertyField> properties) {
        this(keys, properties, new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>());
    }

    public SessionRows(List<KeyField> keys, Set<PropertyField> properties, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        this.keys = keys;
        this.properties = properties;
        this.rows = rows;
    }

    public List<KeyField> getKeys() {
        return keys;
    }

    public Set<PropertyField> getProperties() {
        return properties;
    }

    public Map<KeyField, KeyExpr> getMapKeys() {
        return KeyExpr.getMapKeys(keys);
    }

    public Join<PropertyField> join(final Map<KeyField, ? extends Expr> joinImplement) {
        return new platform.server.data.query.Join<PropertyField>() {

            public Expr getExpr(PropertyField property) {
                CaseExprInterface cases = Expr.newCases();
                for(Map.Entry<Map<KeyField, DataObject>,Map<PropertyField, ObjectValue>> row : rows.entrySet())
                    cases.add(CompareWhere.compareValues(joinImplement,row.getKey()),row.getValue().get(property).getExpr());
                return cases.getFinal();
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

    @HashLazy
    public int hashValues(HashValues hashValues) {
        int hash = 0;
        for(Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> row : rows.entrySet())
            hash += MapValuesIterable.hash(row.getKey(),hashValues) ^ MapValuesIterable.hash(row.getValue(),hashValues);
        return hash;
    }

    @IdentityLazy
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
        return new SessionRows(keys, properties, transRows);
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

    public SessionData insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, boolean groupLast, Object owner) throws SQLException {

        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> orRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        Map<PropertyField, ObjectValue> prevValue = orRows.put(keyFields,propFields);
        assert update || prevValue==null;

        if(orRows.size()>MAX_ROWS) // если превысили количество рядов "переходим" в таблицу
            return new SessionDataTable(session, keys, properties, orRows, groupLast, owner);
        else
            return new SessionRows(keys, properties, orRows);
    }

    public static SessionData write(SQLSession session, List<KeyField> keys, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException {
        Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> data = BaseUtils.toMap(writeRows, (Map<PropertyField, ObjectValue>) new HashMap<PropertyField, ObjectValue>());

        if(writeRows.size()>MAX_ROWS)
            return new SessionDataTable(session, keys, new HashSet<PropertyField>(), data, true, owner);
        else
            return new SessionRows(keys, new HashSet<PropertyField>(), data);

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

    public static SessionData write(final SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Query<KeyField, PropertyField> query, BaseClass baseClass, final QueryEnvironment env, Object owner) throws SQLException {

        assert properties.equals(query.properties.keySet());

        Map<KeyField, Expr> keyExprValues = new HashMap<KeyField, Expr>();
        Map<PropertyField, Expr> propExprValues = new HashMap<PropertyField, Expr>();
        final Query<KeyField, PropertyField> pullQuery = query.pullValues(keyExprValues, propExprValues);

        Map<KeyField, DataObject> keyValues = new HashMap<KeyField, DataObject>();
        for(Map.Entry<KeyField, ObjectValue> keyValue : Expr.readValues(session, baseClass, keyExprValues, env).entrySet())
            if(keyValue.getValue() instanceof DataObject)
                keyValues.put(keyValue.getKey(), (DataObject) keyValue.getValue());
            else
                return new SessionRows(keys, properties); // если null в ключах можно валить
        Map<PropertyField, ObjectValue> propValues = Expr.readValues(session, baseClass, propExprValues, env);

        // читаем классы не считывая данные
        Map<PropertyField,ClassWhere<Field>> insertClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(PropertyField field : pullQuery.properties.keySet())
            insertClasses.put(field,pullQuery.<Field>getClassWhere(Collections.singleton(field)));

        SessionTable table = new SessionTable(session, BaseUtils.filterList(keys, pullQuery.mapKeys.keySet()), pullQuery.properties.keySet(), null, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                return session.insertSessionSelect(name, pullQuery, env);
            }
        }, pullQuery.<KeyField>getClassWhere(new ArrayList<PropertyField>()), insertClasses, owner);
        // нужно прочитать то что записано
        if(table.count > MAX_ROWS)
            return new SessionDataTable(table, keys, keyValues, propValues);
        else {
            OrderedMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> readRows = table.read(session, baseClass);

            table.drop(session, owner); // выкидываем таблицу

            // надо бы batch update сделать, то есть зная уже сколько запискй
            SessionRows sessionRows = new SessionRows(keys, properties);
            for (Iterator<Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>> iterator = readRows.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> writeRow = iterator.next();
                sessionRows = (SessionRows) sessionRows.insertRecord(session, BaseUtils.merge(writeRow.getKey(), keyValues), BaseUtils.merge(writeRow.getValue(), propValues), false, !iterator.hasNext(), owner);
            }
            return sessionRows;
        }
    }

    public static SessionData rewrite(SessionData<?> data, SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        boolean used = data.used(query);
        if(!used)
            data.drop(session, owner);

        SessionData result = write(session, data.getKeys(), data.getProperties(), query, baseClass, env, owner);

        if(used)
            data.drop(session, owner);
        return result;
    }

    public SessionData rewrite(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        return rewrite(this, session, query, baseClass, env, owner);
    }

    public SessionData deleteRecords(SQLSession session, Map<KeyField,DataObject> keys) throws SQLException {
        return new SessionRows(this.keys, properties, BaseUtils.removeKey(rows, keys));
    }

    public SessionData deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException {
        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> removeRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        Iterator<Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>>> iterator = removeRows.entrySet().iterator();
        while(iterator.hasNext())
            if(iterator.next().getKey().get(mapField).equals(object))
                iterator.remove();
        return new SessionRows(keys, properties, removeRows);
    }

    public SessionData deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException {
        Map<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>> removeRows = new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(rows);
        Iterator<Map.Entry<Map<KeyField,DataObject>,Map<PropertyField,ObjectValue>>> iterator = removeRows.entrySet().iterator();
        while(iterator.hasNext())
            if(iterator.next().getValue().get(property).equals(object))
                iterator.remove();
        return new SessionRows(keys, properties, removeRows);
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getComponents() {
        if(components==null)
            components = AbstractMapValues.getComponents(this);
        return components;
    }

    @Override
    public void out(SQLSession session) throws SQLException {
        System.out.println("Rows :" + rows);
    }

    public boolean used(Query<?, ?> query) {
        return false;
    }

    public static Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> getClasses(Set<PropertyField> properties, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows) {
        Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orClasses = new Pair<ClassWhere<KeyField>, Map<PropertyField,ClassWhere<Field>>>(ClassWhere.<KeyField>STATIC(false), BaseUtils.toMap(properties, ClassWhere.<Field>STATIC(false)));
        for(Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> row : rows.entrySet())
            orClasses = SessionTable.orFieldsClassWheres(orClasses.first, orClasses.second, row.getKey(), row.getValue());
        return orClasses;
    }

    @IdentityLazy
    private Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> getClasses() {
        return getClasses(properties, rows);
    }

    @Override
    public ClassWhere<KeyField> getClassWhere() {
        return getClasses().first;
    }

    public ClassWhere<Field> getClassWhere(PropertyField property) {
        if(rows.size()==0)
            return ClassWhere.STATIC(false);
        else
            return getClasses().second.get(property);
    }

    public boolean isEmpty() {
        return rows.size()==0;
    }
}
