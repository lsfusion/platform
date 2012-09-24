package platform.server.data;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.base.Result;
import platform.server.Settings;
import platform.server.caches.AbstractValuesContext;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.AbstractJoin;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.AbstractWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

public abstract class SessionData<T extends SessionData<T>> extends AbstractValuesContext<T> {

    public abstract List<KeyField> getKeys();
    public abstract Set<PropertyField> getProperties();

    public abstract Join<PropertyField> join(final Map<KeyField, ? extends Expr> joinImplement);

    public abstract void drop(SQLSession session, Object owner) throws SQLException;
    public abstract void rollDrop(SQLSession session, Object owner) throws SQLException;

    public abstract boolean used(Query<?, ?> query);

    public abstract SessionData modifyRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, Modify type, Object owner) throws SQLException;


    public abstract void out(SQLSession session) throws SQLException;

    public abstract ClassWhere<KeyField> getClassWhere();
    public abstract ClassWhere<Field> getClassWhere(PropertyField property);
    
    public abstract SessionData fixKeyClasses(ClassWhere<KeyField> fixClasses);

    public abstract boolean isEmpty();
    
    private interface ResultSingleValues<R> {
        
        R empty();
        R singleRow(Map<KeyField, DataObject> keyValues, Map<PropertyField, ObjectValue> propValues) throws SQLException;
    }
    private static <R> R readSingleValues(SQLSession session, BaseClass baseClass, QueryEnvironment env, IQuery<KeyField, PropertyField> query, Result<IQuery<KeyField, PropertyField>> pullQuery, Map<KeyField, DataObject> keyValues, Map<PropertyField, ObjectValue> propValues, ResultSingleValues<R> resultRead) throws SQLException {
        IQuery.PullValues<KeyField, PropertyField> pullValues = query.pullValues();
        query = pullValues.query;
        Map<KeyField, Expr> keyExprValues = pullValues.pullKeys;
        Map<PropertyField, Expr> propExprValues = pullValues.pullProps;

        if(query.isEmpty()) // оптимизация
            return resultRead.empty();

        assert ((AbstractWhere)query.getWhere()).isValue() == query.getMapKeys().isEmpty();
        boolean singleQuery = query.getMapKeys().isEmpty();

        Map<Object, Expr> readValues = new HashMap<Object, Expr>();
        readValues.putAll(keyExprValues); // keys
        readValues.putAll(propExprValues); // properties
        if(singleQuery) // where
            readValues.put("single", ValueExpr.get(query.getWhere()));

        Map<Object, ObjectValue> readedValues = Expr.readValues(session, baseClass, readValues, env);

        for(KeyField keyValue : keyExprValues.keySet()) { // keys
            ObjectValue readedKeyValue = readedValues.get(keyValue);
            if(readedKeyValue instanceof DataObject)
                keyValues.put(keyValue, (DataObject) readedKeyValue);
            else
                return resultRead.empty(); // если null в ключах можно валить
        }
        for(PropertyField propValue : propExprValues.keySet()) // properties
            propValues.put(propValue, readedValues.get(propValue));
        if(singleQuery) { // where
            ObjectValue whereValue = readedValues.get("single");
            if(whereValue.isNull())
                return resultRead.empty();
            else
                return resultRead.singleRow(keyValues, propValues);
        }

        pullQuery.set(query);
        return null;
    }

    private static SessionData write(final SQLSession session, final List<KeyField> keys, final Set<PropertyField> properties, IQuery<KeyField, PropertyField> query, BaseClass baseClass, final QueryEnvironment env, Object owner) throws SQLException {

        assert properties.equals(query.getProperties());

        Map<KeyField, DataObject> keyValues = new HashMap<KeyField, DataObject>();
        Map<PropertyField, ObjectValue> propValues = new HashMap<PropertyField, ObjectValue>();

        if(!Settings.instance.isDisableReadSingleValues()) {
            Result<IQuery<KeyField, PropertyField>> pullQuery = new Result<IQuery<KeyField, PropertyField>>();
            SessionRows singleResult = readSingleValues(session, baseClass, env, query, pullQuery, keyValues, propValues, new ResultSingleValues<SessionRows>() {
                public SessionRows empty() {
                    return new SessionRows(keys, properties);
                }

                public SessionRows singleRow(Map<KeyField, DataObject> keyValues, Map<PropertyField, ObjectValue> propValues) {
                    return new SessionRows(keys, properties, Collections.singletonMap(keyValues, propValues));
                }
            });
            if(singleResult!=null)
                return singleResult;
            query = pullQuery.result;
        }

        final IQuery<KeyField, PropertyField> insertQuery = query;
        SessionTable table = new SessionTable(session, BaseUtils.filterList(keys, query.getMapKeys().keySet()), query.getProperties(), null, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                return session.insertSessionSelect(name, insertQuery, env);
            }
        }, getQueryClasses(query), owner);
        // нужно прочитать то что записано
        if(table.count > SessionRows.MAX_ROWS) {
            if(!Settings.instance.isDisableReadSingleValues()) { // чтение singleValues
                Map<KeyField, Object> actualKeyValues = new HashMap<KeyField, Object>();
                Map<PropertyField, Object> actualPropValues = new HashMap<PropertyField, Object>();
                session.readSingleValues(table, actualKeyValues, actualPropValues);
                table = table.removeFields(session, actualKeyValues.keySet(), actualPropValues.keySet(), owner);
                keyValues.putAll(baseClass.getDataObjects(session, actualKeyValues, Field.<KeyField>typeGetter()));
                propValues.putAll(baseClass.getObjectValues(session, actualPropValues, Field.<PropertyField>typeGetter()));
            }

            return new SessionDataTable(table, keys, keyValues, propValues);
        } else {
            OrderedMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> readRows = table.read(session, baseClass);

            table.drop(session, owner); // выкидываем таблицу

            // надо бы batch update сделать, то есть зная уже сколько запискй
            SessionRows sessionRows = new SessionRows(keys, properties);
            for (Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> writeRow : readRows.entrySet())
                sessionRows = (SessionRows) sessionRows.modifyRecord(session, BaseUtils.merge(writeRow.getKey(), keyValues), BaseUtils.merge(writeRow.getValue(), propValues), Modify.ADD, owner);
            return sessionRows;
        }
    }

    public static Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> getQueryClasses(IQuery<KeyField, PropertyField> pullQuery) {
        // читаем классы не считывая данные
        Map<PropertyField,ClassWhere<Field>> propertyClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(PropertyField field : pullQuery.getProperties())
            propertyClasses.put(field,pullQuery.<Field>getClassWhere(Collections.singleton(field)));
        ClassWhere<KeyField> classes = pullQuery.<KeyField>getClassWhere(new HashSet<PropertyField>());
        return new Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>>(classes, propertyClasses);
    }

    public SessionData rewrite(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        boolean used = used(query);
        if(!used)
            drop(session, owner);

        SessionData result = write(session, getKeys(), getProperties(), query, baseClass, env, owner);

        if(used)
            drop(session, owner);
        return result;
    }

    public SessionData modifyRows(final SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, final Modify type, QueryEnvironment env, final Object owner) throws SQLException {
        if(!Settings.instance.isDisableReadSingleValues()) {
            SessionData singleResult = readSingleValues(session, baseClass, env, query, new Result<IQuery<KeyField, PropertyField>>(), new HashMap<KeyField, DataObject>(),
                    new HashMap<PropertyField, ObjectValue>(), new ResultSingleValues<SessionData>() {
                public SessionData empty() {
                    return SessionData.this;
                }

                public SessionData singleRow(Map<KeyField, DataObject> keyValues, Map<PropertyField, ObjectValue> propValues) throws SQLException {
                    return modifyRecord(session, keyValues, propValues, type, owner);
                }
            });
            if(singleResult!=null)
                return singleResult;
        }

        Query<KeyField, PropertyField> modifyQuery = new Query<KeyField, PropertyField>(query.mapKeys);
        Join<PropertyField> prevJoin = join(modifyQuery.mapKeys);

        Where prevWhere = prevJoin.getWhere();
        modifyQuery.and(type==Modify.DELETE ? prevWhere.and(query.where.not()) : (type == Modify.UPDATE ? prevWhere : prevWhere.or(query.where)));
        for(PropertyField property : getProperties()) {
            Expr newExpr = query.properties.get(property);
            Expr prevExpr = prevJoin.getExpr(property);
            modifyQuery.properties.put(property, newExpr == null? prevExpr : (type== Modify.MODIFY || type== Modify.UPDATE ?
                    newExpr.ifElse(query.where, prevExpr) : prevExpr.ifElse(prevWhere, newExpr)));
        }
        return rewrite(session, modifyQuery, baseClass, env, owner);
    }
    public abstract SessionData updateAdded(SQLSession session, BaseClass baseClass, PropertyField property, int count) throws SQLException;

    // "обновляет" ключи в таблице
    public SessionData rewrite(SQLSession session, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> writeRows, Object owner) throws SQLException {
        drop(session, owner);

        if(writeRows.size()> SessionRows.MAX_ROWS)
            return new SessionDataTable(session, getKeys(), getProperties(), writeRows, owner);
        else
            return new SessionRows(getKeys(), getProperties(), writeRows);
    }

    protected abstract class SessionJoin extends AbstractJoin<PropertyField> {

        protected final Map<KeyField, ? extends Expr> joinImplement;
        protected SessionJoin(Map<KeyField, ? extends Expr> joinImplement) {
            this.joinImplement = joinImplement;
        }

        public Collection<PropertyField> getProperties() {
            return SessionData.this.getProperties();
        }

        public Join<PropertyField> translateRemoveValues(MapValuesTranslate translate) {
            return SessionData.this.translateRemoveValues(translate).join(translate.mapKeys().translate(joinImplement));
        }
    }
    
    public abstract int getCount();
}
