package platform.server.data;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.caches.AbstractValuesContext;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.query.AbstractJoin;
import platform.server.data.query.IQuery;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
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

    public abstract SessionData insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, boolean groupLast, Object owner) throws SQLException;

    public abstract SessionData deleteRecords(SQLSession session, Map<KeyField,DataObject> keys) throws SQLException;

    public abstract SessionData deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException;

    public abstract SessionData deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException;

    public abstract void out(SQLSession session) throws SQLException;

    public abstract ClassWhere<KeyField> getClassWhere();
    public abstract ClassWhere<Field> getClassWhere(PropertyField property);

    public abstract boolean isEmpty();

    private static SessionData write(final SQLSession session, List<KeyField> keys, Set<PropertyField> properties, Query<KeyField, PropertyField> query, BaseClass baseClass, final QueryEnvironment env, Object owner) throws SQLException {

        assert properties.equals(query.properties.keySet());

        Map<KeyField, Expr> keyExprValues = new HashMap<KeyField, Expr>();
        Map<PropertyField, Expr> propExprValues = new HashMap<PropertyField, Expr>();
        final IQuery<KeyField, PropertyField> pullQuery = query.pullValues(keyExprValues, propExprValues);

        Map<KeyField, DataObject> keyValues = new HashMap<KeyField, DataObject>();
        for(Map.Entry<KeyField, ObjectValue> keyValue : Expr.readValues(session, baseClass, keyExprValues, env).entrySet())
            if(keyValue.getValue() instanceof DataObject)
                keyValues.put(keyValue.getKey(), (DataObject) keyValue.getValue());
            else
                return new SessionRows(keys, properties); // если null в ключах можно валить
        Map<PropertyField, ObjectValue> propValues = Expr.readValues(session, baseClass, propExprValues, env);

        // читаем классы не считывая данные
        Map<PropertyField,ClassWhere<Field>> insertClasses = new HashMap<PropertyField, ClassWhere<Field>>();
        for(PropertyField field : pullQuery.getProperties())
            insertClasses.put(field,pullQuery.<Field>getClassWhere(Collections.singleton(field)));

        SessionTable table = new SessionTable(session, BaseUtils.filterList(keys, pullQuery.getMapKeys().keySet()), pullQuery.getProperties(), null, new FillTemporaryTable() {
            public Integer fill(String name) throws SQLException {
                return session.insertSessionSelect(name, pullQuery, env);
            }
        }, pullQuery.<KeyField>getClassWhere(new ArrayList<PropertyField>()), insertClasses, owner);
        // нужно прочитать то что записано
        if(table.count > SessionRows.MAX_ROWS) {
            Map<KeyField, Object> actualKeyValues = new HashMap<KeyField, Object>();
            Map<PropertyField, Object> actualPropValues = new HashMap<PropertyField, Object>();
            session.readSingleValues(table, actualKeyValues, actualPropValues);
            return new SessionDataTable(table.removeFields(session, actualKeyValues.keySet(), actualPropValues.keySet(), owner), keys,
                    BaseUtils.merge(keyValues, baseClass.getDataObjects(session, actualKeyValues, Field.<KeyField>typeGetter())),
                    BaseUtils.merge(propValues, baseClass.getObjectValues(session, actualPropValues, Field.<PropertyField>typeGetter())));
        } else {
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

    public SessionData rewrite(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        boolean used = used(query);
        if(!used)
            drop(session, owner);

        SessionData result = write(session, getKeys(), getProperties(), query, baseClass, env, owner);

        if(used)
            drop(session, owner);
        return result;
    }

    // "обновляет" ключи в таблице
    public SessionData rewrite(SQLSession session, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException {
        assert getProperties().isEmpty();
        drop(session, owner);

        Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> data = BaseUtils.toMap(writeRows, (Map<PropertyField, ObjectValue>) new HashMap<PropertyField, ObjectValue>());

        if(writeRows.size()> SessionRows.MAX_ROWS)
            return new SessionDataTable(session, getKeys(), new HashSet<PropertyField>(), data, true, owner);
        else
            return new SessionRows(getKeys(), new HashSet<PropertyField>(), data);
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
}
