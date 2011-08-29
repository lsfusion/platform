package platform.server.logics.table;

import platform.base.BaseUtils;
import platform.server.caches.IdentityLazy;
import platform.server.classes.SystemClass;
import platform.server.data.*;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.query.Query;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;

import java.sql.SQLException;
import java.util.*;

// таблица счетчика sID
public class IDTable extends GlobalTable {

    public static final IDTable instance = new IDTable(); 

    KeyField key;
    PropertyField value;

    public IDTable() {
        super("idtable");
        key = new KeyField("id", SystemClass.instance);
        keys.add(key);

        value = new PropertyField("value", SystemClass.instance);
        properties.add(value);

        classes = new ClassWhere<KeyField>(key, SystemClass.instance);

        Map<Field, SystemClass> valueClasses = new HashMap<Field, SystemClass>();
        valueClasses.put(key, SystemClass.instance);
        valueClasses.put(value, SystemClass.instance);
        propertyClasses.put(value,new ClassWhere<Field>(valueClasses));
    }

    public final static int OBJECT = 1;
    public final static int FORM = 2;

    static List<Integer> getCounters() {
        List<Integer> result = new ArrayList<Integer>();
        result.add(OBJECT);
        result.add(FORM);
        return result;
    }

    @IdentityLazy
    private Query<KeyField, PropertyField> getGenerateQuery(int idType) {
        Query<KeyField, PropertyField> query = new Query<KeyField, PropertyField>(this);
        platform.server.data.query.Join<PropertyField> joinTable = joinAnd(Collections.singletonMap(key, query.mapKeys.get(key)));
        query.and(joinTable.getWhere());
        query.properties.put(value, joinTable.getExpr(value));

        query.and(new EqualsWhere(query.mapKeys.get(key),new ValueExpr(idType, SystemClass.instance)));
        return query;
    }

    public Integer generateID(SQLSession dataSession, int idType) throws SQLException {

        assert !dataSession.isInTransaction();

        Integer freeID;
        synchronized (this) {
            dataSession.startTransaction();
            freeID = (Integer) BaseUtils.singleValue(getGenerateQuery(idType).execute(dataSession)).get(value); // замещаем
            reserveID(dataSession, idType, freeID);
            dataSession.commitTransaction();
        }

        return freeID+1;
    }

    public void reserveID(SQLSession session, int idType, Integer ID) throws SQLException {
        Query<KeyField, PropertyField> updateQuery = getReserveQuery(idType, ID);
        session.updateRecords(new ModifyQuery(this,updateQuery));
    }

    @IdentityLazy
    private Query<KeyField, PropertyField> getReserveQuery(int idType, Integer ID) {
        Query<KeyField, PropertyField> updateQuery = new Query<KeyField, PropertyField>(this);
        updateQuery.putKeyWhere(Collections.singletonMap(key, new DataObject(idType, SystemClass.instance)));
        updateQuery.properties.put(value,new ValueExpr(ID+1, SystemClass.instance));
        return updateQuery;
    }

    public StatKeys<KeyField> getStatKeys() {
        return getStatKeys(this, getCounters().size());
    }

    public Map<PropertyField, Stat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
