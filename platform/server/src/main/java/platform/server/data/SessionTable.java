package platform.server.data;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.GlobalObject;
import platform.interop.Compare;
import platform.server.caches.AbstractMapValues;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.hash.HashValues;
import platform.server.caches.hash.HashContext;
import platform.server.classes.BaseClass;
import platform.server.data.expr.Expr;
import platform.server.data.query.Query;
import platform.server.data.query.CompileSource;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.classes.ClassWhere;
import platform.server.data.type.ParseInterface;
import platform.server.data.type.StringParseInterface;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

public class SessionTable extends Table implements SessionData<SessionTable>, Value {// в явную хранимые ряды

    public SessionTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Object owner) throws SQLException {
        this(session, keys, properties, classes, propertyClasses, new HashMap<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>>(), owner);
    }

    public SessionTable(String name, List<KeyField> keys, Set<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses) {
        super(name, keys, properties, classes, propertyClasses);
    }

    public SessionTable(SQLSession session, List<KeyField> keys, Set<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses, Map<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> rows, Object owner) throws SQLException {
        super(session.createTemporaryTable(keys, properties, owner), keys, properties, classes, propertyClasses);
        for (Map.Entry<Map<KeyField, DataObject>, Map<PropertyField, ObjectValue>> row : rows.entrySet()) {
            session.insertRecord(this, row.getKey(), row.getValue());
        }
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
        return Collections.singleton((Value)this);
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
    private static class Struct implements GlobalObject {

        public final List<KeyField> keys; // List потому как в таком порядке индексы будут строиться
        public final Collection<PropertyField> properties;
        protected final ClassWhere<KeyField> classes; // по сути условия на null'ы в том числе
        protected final Map<PropertyField,ClassWhere<Field>> propertyClasses;

        private Struct(List<KeyField> keys, Collection<PropertyField> properties, ClassWhere<KeyField> classes, Map<PropertyField, ClassWhere<Field>> propertyClasses) {
            this.keys = keys;
            this.properties = properties;
            this.classes = classes;
            this.propertyClasses = propertyClasses;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Struct && classes.equals(((Struct) o).classes) && keys.equals(((Struct) o).keys) && properties.equals(((Struct) o).properties) && propertyClasses.equals(((Struct) o).propertyClasses);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * (31 * keys.hashCode() + properties.hashCode()) + classes.hashCode()) + propertyClasses.hashCode();
        }
    }

    private Struct struct = null;
    @ManualLazy
    public GlobalObject getValueClass() {
        if(struct==null)
            struct = new Struct(keys, properties, classes, propertyClasses);
        return struct;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof SessionTable && name.equals(((SessionTable) o).name) && getValueClass().equals(((SessionTable) o).getValueClass());
    }

    @Override
    public int hashCode() { // можно было бы взять из AbstractMapValues но без мн-го наследования
        return name.hashCode() * 31 + getValueClass().hashCode();
    }

    public SessionTable insertRecord(SQLSession session, Map<KeyField, DataObject> keyFields, Map<PropertyField, ObjectValue> propFields, boolean update, Object owner) throws SQLException {

        Pair<ClassWhere<KeyField>, Map<PropertyField, ClassWhere<Field>>> orClasses = SessionRows.updateFieldsClassWheres(classes, propertyClasses, keyFields, propFields);

        if(update)
            session.updateInsertRecord(this,keyFields,propFields);
        else
            session.insertRecord(this,keyFields,propFields);

        return new SessionTable(name, keys, properties, orClasses.first, orClasses.second);
    }

    // "обновляет" ключи в таблице
    public SessionData rewrite(SQLSession session, Collection<Map<KeyField, DataObject>> writeRows, Object owner) throws SQLException {
        return SessionRows.rewrite(this, session, writeRows, owner);
    }

    public SessionData rewrite(SQLSession session, Query<KeyField, PropertyField> query, BaseClass baseClass, QueryEnvironment env, Object owner) throws SQLException {
        return SessionRows.rewrite(this, session, query, baseClass, env, owner);
    }

    public SessionTable deleteRecords(SQLSession session, Map<KeyField,DataObject> keys) throws SQLException {
        session.deleteKeyRecords(this, DataObject.getMapValues(keys));
        return this;
    }

    public SessionTable deleteAllRecords(SQLSession session) throws SQLException {
        session.deleteAllRecords(this);
        return this;
    }

    public SessionTable deleteKey(SQLSession session, KeyField mapField, DataObject object) throws SQLException {
        session.deleteKeyRecords(this, Collections.singletonMap(mapField,object.object));
        return this;
    }

    public SessionTable deleteProperty(SQLSession session, PropertyField property, DataObject object) throws SQLException {
        Query<KeyField,PropertyField> dropValues = new Query<KeyField, PropertyField>(this);
        platform.server.data.query.Join<PropertyField> dataJoin = joinAnd(dropValues.mapKeys);
        dropValues.and(dataJoin.getExpr(property).compare(object, Compare.EQUALS));
        dropValues.properties.put(property, Expr.NULL);
        session.updateRecords(new ModifyQuery(this,dropValues));
        return this;
    }

    private BaseUtils.HashComponents<Value> components = null;
    @ManualLazy
    public BaseUtils.HashComponents<Value> getComponents() {
        if(components==null)
            components = AbstractMapValues.getComponents(this);
        return components;
    }

    public void drop(SQLSession session, Object owner) throws SQLException {
        session.dropTemporaryTable(this, owner);
    }
}
