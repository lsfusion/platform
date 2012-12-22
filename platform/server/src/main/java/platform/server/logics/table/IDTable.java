package platform.server.logics.table;

import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.server.Settings;
import platform.server.caches.IdentityLazy;
import platform.server.classes.SystemClass;
import platform.server.data.*;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.where.classes.ClassWhere;
import platform.server.logics.DataObject;

import java.sql.SQLException;

// таблица счетчика sID
public class IDTable extends GlobalTable {

    public static final IDTable instance = new IDTable(); 

    KeyField key;
    PropertyField value;

    public IDTable() {
        super("idtable");
        key = new KeyField("id", SystemClass.instance);
        keys = SetFact.singletonOrder(key);

        value = new PropertyField("value", SystemClass.instance);
        properties = SetFact.singleton(value);

        classes = new ClassWhere<KeyField>(key, SystemClass.instance);

        ImMap<Field, SystemClass> valueClasses = MapFact.toMap(key, SystemClass.instance, value, SystemClass.instance);
        propertyClasses = MapFact.singleton(value,new ClassWhere<Field>(valueClasses));
    }

    public final static int OBJECT = 1;
    public final static int FORM = 2;

    static ImMap<Integer, Integer> getCounters() {
        return MapFact.toMap(OBJECT, 10, FORM, 0); // потому как есть базовый пул 0,1,2 предопределенных ID'ков
    }

    @IdentityLazy
    private Query<KeyField, PropertyField> getGenerateQuery(int idType) {
        QueryBuilder<KeyField, PropertyField> query = new QueryBuilder<KeyField, PropertyField>(this, MapFact.singleton(key, new DataObject(idType, SystemClass.instance)));
        platform.server.data.query.Join<PropertyField> joinTable = join(query.getMapExprs());
        query.and(joinTable.getWhere());
        query.addProperty(value, joinTable.getExpr(value));
        return query.getQuery();
    }

    private int freeID = 0;
    private int maxReservedID = -1;
    public int generateID(SQLSession dataSession, int idType) throws SQLException {

        Integer result;
        synchronized (this) {
            assert !dataSession.isInTransaction();

            if(freeID > maxReservedID) { // читаем новый пул
                int reserveIDStep = Settings.instance.getReserveIDStep();
                freeID = reserveIDs(reserveIDStep, dataSession, idType);
                maxReservedID = freeID + reserveIDStep - 1;
            }
            result = freeID++;
        }

        return result;
    }

    // возвращает первый, и резервирует себе еще count id'ков
    public int reserveIDs(int count, SQLSession dataSession, int idType) throws SQLException {
        int freeID;
        synchronized (this) {
            dataSession.startTransaction();

            freeID = (Integer) getGenerateQuery(idType).execute(dataSession).singleValue().get(value) + 1; // замещаем

            QueryBuilder<KeyField, PropertyField> updateQuery = new QueryBuilder<KeyField, PropertyField>(this, MapFact.singleton(key, new DataObject(idType, SystemClass.instance)));
            updateQuery.addProperty(value, new ValueExpr(freeID + count - 1, SystemClass.instance));
            dataSession.updateRecords(new ModifyQuery(this, updateQuery.getQuery()));

            dataSession.commitTransaction();
        }
        return freeID;
    }

    public StatKeys<KeyField> getStatKeys() {
        return getStatKeys(this, getCounters().size());
    }

    public ImMap<PropertyField, Stat> getStatProps() {
        throw new RuntimeException("not supported");
    }
}
