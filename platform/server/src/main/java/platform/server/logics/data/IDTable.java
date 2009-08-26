package platform.server.logics.data;

import platform.interop.Compare;
import platform.server.data.*;
import platform.server.data.classes.SystemClass;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.query.wheres.EqualsWhere;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.session.SQLSession;
import platform.base.BaseUtils;

import java.sql.SQLException;
import java.util.*;

// таблица счетчика sID
public class IDTable extends Table {

    public static final IDTable instance = new IDTable(); 

    KeyField key;
    PropertyField value;

    IDTable() {
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

        if(BusinessLogics.autoFillDB) return BusinessLogics.autoIDCounter++;
        // читаем
        JoinQuery<KeyField, PropertyField> query = new JoinQuery<KeyField, PropertyField>(this);
        Join joinTable = joinAnd(Collections.singletonMap(key,query.mapKeys.get(key)));
        query.and(joinTable.getWhere());
        query.properties.put(value, joinTable.getExpr(value));

        query.and(new EqualsWhere(query.mapKeys.get(key),new ValueExpr(idType, SystemClass.instance)));

        Integer freeID = (Integer) BaseUtils.singleValue(query.executeSelect(dataSession)).get(value);

        // замещаем
        reserveID(dataSession, idType, freeID);
        return freeID+1;
    }

    public void reserveID(SQLSession session, int idType, Integer ID) throws SQLException {
        JoinQuery<KeyField, PropertyField> updateQuery = new JoinQuery<KeyField, PropertyField>(this);
        updateQuery.putKeyWhere(Collections.singletonMap(key,new DataObject(idType, SystemClass.instance)));
        updateQuery.properties.put(value,new ValueExpr(ID+1, SystemClass.instance));
        session.updateRecords(new ModifyQuery(this,updateQuery));
    }
}
