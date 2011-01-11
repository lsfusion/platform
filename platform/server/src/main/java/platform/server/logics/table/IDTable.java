package platform.server.logics.table;

import platform.base.BaseUtils;
import platform.server.classes.SystemClass;
import platform.server.data.*;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.where.EqualsWhere;
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

    public Integer generateID(SQLSession dataSession, int idType) throws SQLException {

        assert !dataSession.isInTransaction();

        // читаем
        Query<KeyField, PropertyField> query = new Query<KeyField, PropertyField>(this);
        platform.server.data.query.Join<PropertyField> joinTable = joinAnd(Collections.singletonMap(key,query.mapKeys.get(key)));
        query.and(joinTable.getWhere());
        query.properties.put(value, joinTable.getExpr(value));

        query.and(new EqualsWhere(query.mapKeys.get(key),new ValueExpr(idType, SystemClass.instance)));

        Integer freeID = (Integer) BaseUtils.singleValue(query.execute(dataSession)).get(value);

        // замещаем
        reserveID(dataSession, idType, freeID);
        return freeID+1;
    }

    public void reserveID(SQLSession session, int idType, Integer ID) throws SQLException {
        Query<KeyField, PropertyField> updateQuery = new Query<KeyField, PropertyField>(this);
        updateQuery.putKeyWhere(Collections.singletonMap(key,new DataObject(idType, SystemClass.instance)));
        updateQuery.properties.put(value,new ValueExpr(ID+1, SystemClass.instance));
        session.updateRecords(new ModifyQuery(this,updateQuery));
    }
}
