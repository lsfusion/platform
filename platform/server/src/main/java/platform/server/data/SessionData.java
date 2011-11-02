package platform.server.data;

import platform.server.caches.MapValues;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.query.Join;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.Query;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Set;

public interface SessionData<T extends SessionData<T>> extends MapValues<T>, MapKeysInterface<KeyField> {

    List<KeyField> getKeys();
    Set<PropertyField> getProperties();

    Join<PropertyField> join(final Map<KeyField, ? extends Expr> joinImplement);

    T translate(MapValuesTranslate mapValues);

    void drop(SQLSession session, Object owner) throws SQLException;

    boolean used(Query<?, ?> query);

    SessionData insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, boolean groupLast, Object owner) throws SQLException;

    SessionData rewrite(SQLSession session, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException;

    SessionData rewrite(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException;

    SessionData deleteRecords(SQLSession session, Map<KeyField,DataObject> keys) throws SQLException;

    SessionData deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException;

    SessionData deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException;

    void out(SQLSession session) throws SQLException;

    ClassWhere<KeyField> getClassWhere();
    ClassWhere<Field> getClassWhere(PropertyField property);

    boolean isEmpty();
}
